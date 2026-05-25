/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;

/**
 * @since 1.11
 */
@Named
@Singleton
public class PolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<PolicyEvaluation>
{
  private final LastPolicyEvaluationDAO lastPolicyEvaluationDAO;

  @Inject
  public PolicyEvaluationDAO(
      final OperationalDataStore operationalDataStore,
      final LastPolicyEvaluationDAO lastPolicyEvaluationDAO)
  {
    super(operationalDataStore);
    this.lastPolicyEvaluationDAO = lastPolicyEvaluationDAO;
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(TransactionContext tx, String appId, String scanId) {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.APPLICATION_ID.eq(appId))
        .and(POLICY_EVALUATION.SCAN_ID.eq(scanId))
        .orderBy(POLICY_EVALUATION.TIME.desc())
        .limit(1)
        .fetchOne());
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(String appId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndScanId(tx, appId, scanId);
    }
  }

  public PolicyEvaluation getLastByApplicationIdAndScanIdNotNull(String appId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndScanIdNotNull(tx, appId, scanId);
    }
  }

  public PolicyEvaluation getLastByApplicationIdAndScanIdNotNull(TransactionContext tx, String appId, String scanId) {
    PolicyEvaluation policyEvaluation = getLastByApplicationIdAndScanId(tx, appId, scanId);
    if (policyEvaluation == null) {
      throw new NotFoundException(
          "PolicyEvaluation for applicationId " + appId + " and scanId " + scanId + " does not exist.");
    }
    return policyEvaluation;
  }

  public List<PolicyEvaluation> getLastByApplicationIds(Set<String> appIds) {
    if (appIds.size() >= getInOperatorThreshold()) {
      return getLastByApplicationIdsManualFilter(appIds);
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(LAST_POLICY_EVALUATION.APPLICATION_ID.in(appIds))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));
    }
  }

  public List<PolicyEvaluation> getAllLast() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));
    }
  }

  /**
   * As measurements have shown (cf. CLM-6085), H2 doesn't handle an {@code IN} operator with a huge list of values well
   * and at some point it is faster to just load all entities and filter them manually afterwards. Per those
   * measurements, this method outperforms the {@code IN} operator when the input exceeds ~2000 applications, even if
   * 80% of the loaded entities are dropped.
   * <p>
   * Similar to above the check is extended to Postgres with it's allowed threshold limit. (cf, CLM-18653)
   */
  private List<PolicyEvaluation> getLastByApplicationIdsManualFilter(Set<String> appIds) {
    try (TransactionContext tx = createTransactionContext()) {
      List<PolicyEvaluation> allEvals = tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));

      List<PolicyEvaluation> evals = new ArrayList<>(appIds.size());
      for (PolicyEvaluation eval : allEvals) {
        if (appIds.contains(eval.getApplicationId())) {
          evals.add(eval);
        }
      }
      return evals;
    }
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage.
   */
  public PolicyEvaluation getLastByApplicationIdAndStageId(TransactionContext tx, String appId, String stageTypeId) {
    Record record = tx.dsl()
        .select(POLICY_EVALUATION.fields())
        .from(POLICY_EVALUATION)
        .join(LAST_POLICY_EVALUATION)
        .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
        .where(LAST_POLICY_EVALUATION.APPLICATION_ID.eq(appId))
        .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
        .fetchOne();
    return record != null ? toEntity(record.into(POLICY_EVALUATION)) : null;
  }

  public List<PolicyEvaluation> getLastByApplicationIdsAndStageIds(Set<String> appIds, Set<String> stageTypeIds) {
    if (appIds.size() >= getInOperatorThreshold()) {
      return getLastByApplicationIdsAndStageIdsManualFilter(appIds, stageTypeIds);
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(LAST_POLICY_EVALUATION.APPLICATION_ID.in(appIds))
          .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.in(stageTypeIds))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));
    }
  }

  /**
   * H2-specific optimization, see comment on {@link #getLastByApplicationIdsManualFilter(Set)} for more details.
   */
  private List<PolicyEvaluation> getLastByApplicationIdsAndStageIdsManualFilter(
      Set<String> appIds,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<PolicyEvaluation> allEvals = tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.in(stageTypeIds))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));

      List<PolicyEvaluation> evals = new ArrayList<>(appIds.size());
      for (PolicyEvaluation eval : allEvals) {
        if (appIds.contains(eval.getApplicationId())) {
          evals.add(eval);
        }
      }
      return evals;
    }
  }

  public PolicyEvaluation getLastByApplicationIdAndStageId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndStageId(tx, appId, stageTypeId);
    }
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(
      TransactionContext tx,
      String appId,
      String stageTypeId)
  {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.APPLICATION_ID.eq(appId))
        .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
        .and(POLICY_EVALUATION.REEVALUATION.eq(false))
        .orderBy(POLICY_EVALUATION.TIME.desc())
        .limit(1)
        .fetchOne());
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastPrimaryByApplicationIdAndStageId(tx, appId, stageTypeId);
    }
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage, excluding
   * continuous monitoring and reevaluations.
   */
  public PolicyEvaluation getLastByApplicationIdAndStageIdNoMonitoringNoReeval(
      final String appId,
      final String stageTypeId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(appId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  @Override
  public void insert(TransactionContext tx, PolicyEvaluation policyEvaluation) {
    validate(policyEvaluation);

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
    }

    if (policyEvaluation.getId() == null) {
      policyEvaluation.setId(UUID.randomUUID().toString());
    }

    super.insert(tx, policyEvaluation);

    if (policyEvaluation.isForObsoleteScan()) {
      return;
    }

    // Update the last policy evaluation record
    String appId = policyEvaluation.getApplicationId();
    String stageTypeId = policyEvaluation.getStageTypeId();
    PolicyEvaluation lastPolicyEvaluation = getLastByApplicationIdAndStageId(tx, appId, stageTypeId);
    if (lastPolicyEvaluation == null
        || lastPolicyEvaluation.getTime().getTime() < policyEvaluation.getTime().getTime())
    {

      // Delete the current last policy evaluation record for this app and stage type
      if (lastPolicyEvaluation != null) {
        LastPolicyEvaluation lastEval = lastPolicyEvaluationDAO.getById(tx, lastPolicyEvaluation.getId());
        if (lastEval != null) {
          lastPolicyEvaluationDAO.delete(tx, lastEval);
        }
      }

      // Insert a new last policy evaluation record for this application and stage type
      lastPolicyEvaluationDAO.insert(tx, new LastPolicyEvaluation(policyEvaluation.getId(), appId, stageTypeId));
    }
  }

  /**
   * @since 1.39
   */
  public List<PolicyEvaluation> getBetweenDatesByApplicationIdAndStageIds(
      Date sinceDate,
      Date toDate,
      String appId,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(appId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.in(stageTypeIds))
          .and(POLICY_EVALUATION.TIME.ge(sinceDate))
          .and(POLICY_EVALUATION.TIME.lt(toDate))
          .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false))
          .orderBy(POLICY_EVALUATION.TIME)
          .fetch(this::toEntity);
    }
  }

  /**
   * Get the oldest policy evaluation for a given application
   *
   * @since 1.33
   */
  public PolicyEvaluation getOldestByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .orderBy(POLICY_EVALUATION.TIME)
          .limit(1)
          .fetchOne());
    }
  }

  @Override
  public void update(TransactionContext tx, PolicyEvaluation entity) {
    throw new UnsupportedOperationException("The PolicyEvaluation table does not support update operations");
  }

  @Override
  public void delete(final TransactionContext tx, PolicyEvaluation policyEvaluation) {
    // SourceControlPullRequestCommentCascade deletions are cascaded via foreign key ON DELETE CASCADE
    // SourceControlDefaultBranchCommitHistory deletions are cascaded via foreign key ON DELETE CASCADE
    // SourceControlEvent deletions are cascaded via foreign key ON DELETE CASCADE
    // LastPolicyEvaluation deletions are cascaded via foreign key ON DELETE CASCADE

    LastPolicyEvaluation lastPolicyEvaluation = lastPolicyEvaluationDAO.getById(tx, policyEvaluation.getId());

    // Delete the policy evaluation itself
    super.delete(tx, policyEvaluation);

    // Insert a new last policy evaluation if we just deleted the current last
    if (lastPolicyEvaluation != null) {
      PolicyEvaluation newestPolicyEvaluation =
          getNewestPolicyEvaluation(tx, policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId());
      lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(tx, newestPolicyEvaluation);
    }
  }

  private PolicyEvaluation getNewestPolicyEvaluation(TransactionContext tx, String applicationId, String stageTypeId) {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
        .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
        .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false))
        .orderBy(POLICY_EVALUATION.TIME.desc())
        .limit(1)
        .fetchOne());
  }

  private void validate(PolicyEvaluation policyEvaluation) {
    if (!policyEvaluation.isReevaluation() && policyEvaluation.isForObsoleteScan()) {
      throw new IllegalStateException("Primary evaluations cannot be for obsolete scans");
    }
  }

  public int getCountByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getCountByApplicationId(tx, appId);
    }
  }

  public int getCountByApplicationId(TransactionContext tx, String appId) {
    return tx.dsl()
        .selectCount()
        .from(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.APPLICATION_ID.eq(appId))
        .fetchOne(0, Integer.class);
  }

  public List<PolicyEvaluation> getPrimaryNonMonitoringByApplicationIdAndStageId(String applicationId, String stageId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageId))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.ne(StageTypes.COMPLIANCE.getId()))
          .fetch(this::toEntity);
    }
  }

  private static final List<String> stageList = Arrays.asList(Stage.ID_SOURCE, Stage.ID_BUILD, Stage.ID_DEVELOP);

  public boolean hasExternalPolicyEvaluations(String applicationId, Date cutoffTime) {
    try (TransactionContext tx = createTransactionContext()) {
      List<String> internalScanTypeStrings = ScanTriggerType.internalScanTypes.stream()
          .map(ScanTriggerType::toString)
          .toList();
      int count = tx.dsl()
          .selectCount()
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.TIME.gt(cutoffTime))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.in(stageList))
          .and(POLICY_EVALUATION.SCAN_TRIGGER_TYPE.notIn(internalScanTypeStrings))
          .fetchOne(0, Integer.class);
      return count > 0;
    }
  }

  public List<PolicyEvaluation> getPrimaryForMonitoringByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(true))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.ne(StageTypes.COMPLIANCE.getId()))
          .fetch(this::toEntity);
    }
  }

  public PolicyEvaluation getLastByCommitHash(String commitHash) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public List<PolicyEvaluation> getLastByCommitHashPerApplication(String commitHash) {
    try (TransactionContext tx = createTransactionContext()) {
      List<String> applicationIdsForCommit = tx.dsl()
          .selectDistinct(POLICY_EVALUATION.APPLICATION_ID)
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .fetchInto(String.class);

      List<PolicyEvaluation> result = new ArrayList<>();
      applicationIdsForCommit.forEach(id -> result.add(getLastByApplicationAndCommitHash(id, commitHash)));
      return result;
    }
  }

  public PolicyEvaluation getLastByApplicationAndCommitHash(
      final String applicationInternalId,
      final String commitHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationInternalId))
          .and(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastByApplicationAndAbbreviatedCommitHash(
      final String applicationInternalId,
      final String commitHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationInternalId))
          .and(POLICY_EVALUATION.COMMIT_HASH.like(commitHash + "%"))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastInTimeRangeByApplicationAndStage(
      String applicationId,
      String stageTypeId,
      Date minDate,
      Date maxDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_EVALUATION.TIME.ge(minDate));

      if (maxDate != null) {
        return toEntity(query.and(POLICY_EVALUATION.TIME.lt(maxDate))
            .orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(1)
            .fetchOne());
      }
      else {
        return toEntity(query.orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(1)
            .fetchOne());
      }
    }
  }

  public List<PolicyEvaluation> getLimitedAmountByApplicationId(
      String applicationId,
      int maxResultsToReturn,
      String stage)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false));

      if (stage != null) {
        return query.and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stage))
            .orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(maxResultsToReturn)
            .fetch(this::toEntity);
      }
      else {
        return query.orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(maxResultsToReturn)
            .fetch(this::toEntity);
      }
    }
  }

  public List<PolicyEvaluation> getByApplicationId(
      final String applicationId,
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId, page, pageSize);
    }
  }

  public List<PolicyEvaluation> getByApplicationId(
      final TransactionContext tx,
      final String applicationId,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
        .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false))
        .orderBy(POLICY_EVALUATION.TIME, POLICY_EVALUATION.POLICY_EVALUATION_ID)
        .limit(pageSize)
        .offset(offset)
        .fetch(this::toEntity);
  }

  /**
   * Retrieves a PolicyEvaluation by its scan ID and application ID.
   * <p>
   * Returns {@code null} if no matching record is found. Callers must handle the null case
   * appropriately, typically by throwing a {@link NotFoundException} if the evaluation is required.
   *
   * @param scanId the scan ID to search for
   * @param applicationId the application ID to scope the search
   * @return the matching PolicyEvaluation, or {@code null} if not found
   * @since 1.203
   */
  public PolicyEvaluation getByScanIdAndApplicationId(String scanId, String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.SCAN_ID.eq(scanId))
          .and(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .fetchOne(this::toEntity);
    }
  }

  /**
   * Fetches the latest policy evaluation for the given application, commit hash and stage, if any. It returns
   * {@code null} if no matches are found, or if commit hash is blank/missing.
   */
  public PolicyEvaluation getLastByApplicationIdCommitHashAndStageId(
      String applicationId,
      String commitHash,
      String stageTypeId)
  {
    if (StringUtils.isBlank(commitHash)) {
      return null;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastByApplicationAndCommitHashAndTriggerType(
      final String applicationId,
      final String commitHash,
      final boolean externallyTriggered)
  {
    try (TransactionContext tx = createTransactionContext()) {
      List<String> internalScanTypeStrings = ScanTriggerType.internalScanTypes.stream()
          .map(ScanTriggerType::toString)
          .toList();

      var baseQuery = tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .and(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId));

      if (externallyTriggered) {
        return toEntity(baseQuery.and(POLICY_EVALUATION.SCAN_TRIGGER_TYPE.notIn(internalScanTypeStrings))
            .orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(1)
            .fetchOne());
      }
      else {
        return toEntity(baseQuery.and(POLICY_EVALUATION.SCAN_TRIGGER_TYPE.in(internalScanTypeStrings))
            .orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(1)
            .fetchOne());
      }
    }
  }

  public int getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
      final Date lowerBound,
      final Date upperBoundDate)
  {
    // we do not allow new entries to be created with isForObsolete scan to be true unless isReevaluation is
    // also true, so in most cases entity.isForObsoleteScan = false should be redundant
    // This only enforced in the dao. it's unknown if this has always been enforced, so I am leaving
    // the extra check out of caution
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.countDistinct(POLICY_EVALUATION.APPLICATION_ID))
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.STAGE_TYPE_ID.eq(Stage.ID_BUILD))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false))
          .and(POLICY_EVALUATION.TIME.ge(lowerBound))
          .and(POLICY_EVALUATION.TIME.le(upperBoundDate))
          .fetchOne(0, Integer.class);
    }
  }

  // The point of a cut off here is so that one off anomalous scans such as local iq-cli runs are filtered out over
  // time. The exact cutoff can be determined by application logic in the service layer
  public boolean hasCIIntegrationEvaluation(final String applicationId, final Date cutOffDate) {
    // we do not allow new entries to be created with isForObsolete scan to be true unless isReevaluation is
    // also true, so in most cases entity.isForObsoleteScan = false should be redundant
    // This only enforced in the dao. it's unknown if this has always been enforced, so I am leaving
    // the extra check out of caution
    try (TransactionContext tx = createTransactionContext()) {
      int count = tx.dsl()
          .selectCount()
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(Stage.ID_BUILD))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.eq(false))
          .and(POLICY_EVALUATION.TIME.ge(cutOffDate))
          .fetchOne(0, Integer.class);
      return count > 0;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_EVALUATION;
  }

  @Override
  public Class<PolicyEvaluation> getEntityClass() {
    return PolicyEvaluation.class;
  }
}
