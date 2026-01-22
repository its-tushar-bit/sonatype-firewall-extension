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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

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
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.scanId=?2" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, scanId).forceSingleResult().get(tx);
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
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId in (?1)";
    return getList(sQuery, appIds);
  }

  public List<PolicyEvaluation> getAllLast() {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId";
    return getList(sQuery);
  }

  /**
   * As measurements have shown (cf. CLM-6085), H2 doesn't handle an {@code IN} operator with a huge list of values well
   * and at some point it is faster to just load all entities and filter them manually afterwards. Per those
   * measurements, this method outperforms the {@code IN} operator when the input exceeds ~2000 applications, even if
   * 80% of the loaded entities are dropped.
   *
   * Similar to above the check is extended to Postgres with it's allowed threshold limit. (cf, CLM-18653)
   */
  private List<PolicyEvaluation> getLastByApplicationIdsManualFilter(Set<String> appIds) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId";
    List<PolicyEvaluation> evals = new ArrayList<>(appIds.size());
    for (PolicyEvaluation eval : getList(sQuery)) {
      if (appIds.contains(eval.getApplicationId())) {
        evals.add(eval);
      }
    }
    return evals;
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage.
   */
  public PolicyEvaluation getLastByApplicationIdAndStageId(TransactionContext tx, String appId, String stageTypeId) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId=?1" + //
        " AND lpe.stageTypeId=?2";
    return get(tx, sQuery, appId, stageTypeId);
  }

  public List<PolicyEvaluation> getLastByApplicationIdsAndStageIds(Set<String> appIds, Set<String> stageTypeIds) {
    if (appIds.size() >= getInOperatorThreshold()) {
      return getLastByApplicationIdsAndStageIdsManualFilter(appIds, stageTypeIds);
    }
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId in (?1)" + //
        " AND lpe.stageTypeId in (?2)";
    return getList(sQuery, appIds, stageTypeIds);
  }

  /**
   * H2-specific optimization, see comment on {@link #getLastByApplicationIdsManualFilter(Set)} for more details.
   */
  private List<PolicyEvaluation> getLastByApplicationIdsAndStageIdsManualFilter(Set<String> appIds,
                                                                                Set<String> stageTypeIds)
  {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.stageTypeId in (?1)";
    List<PolicyEvaluation> evals = new ArrayList<>(appIds.size());
    for (PolicyEvaluation eval : getList(sQuery, stageTypeIds)) {
      if (appIds.contains(eval.getApplicationId())) {
        evals.add(eval);
      }
    }
    return evals;
  }

  public PolicyEvaluation getLastByApplicationIdAndStageId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndStageId(tx, appId, stageTypeId);
    }
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(TransactionContext tx,
                                                                  String appId,
                                                                  String stageTypeId)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.isReevaluation=false" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, stageTypeId).forceSingleResult().get(tx);
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
    String sQuery = "SELECT pe FROM PolicyEvaluation pe" +
        " WHERE pe.applicationId = ?1" +
        " AND pe.stageTypeId = ?2" +
        " AND pe.isForMonitoring = false" +
        " AND pe.isReevaluation = false" +
        " ORDER BY pe.time DESC";
    return createQuery(sQuery, appId, stageTypeId).forceSingleResult().get();
  }

  @Override
  public void insert(TransactionContext tx, PolicyEvaluation policyEvaluation) {
    validate(policyEvaluation);

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
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
        || lastPolicyEvaluation.getTime().getTime() < policyEvaluation.getTime().getTime()) {

      // Delete the current last policy evaluation record for this app and stage type
      if (lastPolicyEvaluation != null) {
        lastPolicyEvaluationDAO.delete(tx, lastPolicyEvaluationDAO.getByEvaluationId(tx, lastPolicyEvaluation.getId()));
      }

      // Insert a new last policy evaluation record for this application and stage type
      lastPolicyEvaluationDAO.insert(tx, new LastPolicyEvaluation(policyEvaluation.getId(), appId, stageTypeId));
    }
  }

  /**
   * @since 1.39
   */
  public List<PolicyEvaluation> getBetweenDatesByApplicationIdAndStageIds(Date sinceDate,
                                                                          Date toDate,
                                                                          String appId,
                                                                          Set<String> stageTypeIds)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2) AND entity.time >= ?3" + //
        "  AND entity.time < ?4 AND entity.isForObsoleteScan = false" + //
        " ORDER BY entity.time";
    return getList(sQuery, appId, stageTypeIds, sinceDate, toDate);
  }

  /**
   * Get the oldest policy evaluation for a given application
   *
   * @since 1.33
   */
  public PolicyEvaluation getOldestByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1" + //
        " ORDER BY entity.time";

    return createQuery(sQuery, applicationId).forceSingleResult().get();
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

    LastPolicyEvaluation lastPolicyEvaluation = lastPolicyEvaluationDAO.getByEvaluationId(tx, policyEvaluation.getId());

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
    String sQuery = "SELECT e from PolicyEvaluation e " + //
        "WHERE e.applicationId = ?1 " + //
        "AND e.stageTypeId = ?2 " + //
        "AND e.isForObsoleteScan = false " + //
        "ORDER BY e.time DESC";
    return createQuery(sQuery, applicationId, stageTypeId).forceSingleResult().get(tx);
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
    String sQuery = "SELECT COUNT(entity.id) FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1";

    return getSingle(tx, Number.class, sQuery, appId).intValue();
  }

  public List<PolicyEvaluation> getPrimaryNonMonitoringByApplicationIdAndStageId(String applicationId, String stageId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.isForMonitoring=false AND entity.isReevaluation=false AND entity.stageTypeId<>?3";
    return getList(sQuery, applicationId, stageId, StageTypes.COMPLIANCE.getId());
  }

  private static final List<String> stageList = Arrays.asList(Stage.ID_SOURCE, Stage.ID_BUILD, Stage.ID_DEVELOP);

  public boolean hasExternalPolicyEvaluations(String applicationId, Date cutoffTime) {
    String sQuery = "SELECT COUNT(entity.id) FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.time>?2" + //
        " AND entity.stageTypeId IN (?3)" +
        " AND entity.scanTriggerType NOT IN (?4)";
    return getSingle(Number.class, sQuery, applicationId, cutoffTime, stageList,
        ScanTriggerType.internalScanTypes).intValue() > 0;
  }

  public List<PolicyEvaluation> getPrimaryForMonitoringByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.isForMonitoring=true AND entity.isReevaluation=false AND entity.stageTypeId<>?2";
    return getList(sQuery, applicationId, StageTypes.COMPLIANCE.getId());
  }

  public PolicyEvaluation getLastByCommitHash(String commitHash) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.commitHash=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, commitHash).forceSingleResult().get();
  }

  public List<PolicyEvaluation> getLastByCommitHashPerApplication(String commitHash) {
    List<PolicyEvaluation> result = new ArrayList<>();

    String sQuery = "SELECT DISTINCT entity.applicationId FROM PolicyEvaluation entity" + //
        " WHERE entity.commitHash=?1";

    List<String> applicationIdsForCommit = new Query<String>(sQuery, commitHash).getList();

    applicationIdsForCommit.forEach(id -> result.add(getLastByApplicationAndCommitHash(id, commitHash)));

    return result;
  }

  public PolicyEvaluation getLastByApplicationAndCommitHash(
      final String applicationInternalId,
      final String commitHash)
  {
    final String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.commitHash=?2" + //
        " AND entity.applicationId=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, applicationInternalId, commitHash).forceSingleResult().get();
  }

  public PolicyEvaluation getLastByApplicationAndAbbreviatedCommitHash(
      final String applicationInternalId,
      final String commitHash)
  {
    final String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.commitHash like ?2" + //
        " AND entity.applicationId=?1" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, applicationInternalId, commitHash + "%").forceSingleResult().get();
  }

  public PolicyEvaluation getLastInTimeRangeByApplicationAndStage(
      String applicationId,
      String stageTypeId,
      Date minDate,
      Date maxDate)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" +
        " WHERE entity.applicationId = ?1" +
        "  AND entity.stageTypeId = ?2" +
        "  AND entity.time >= ?3" +
        (maxDate == null ? "" : "  AND entity.time < ?4") +
        " ORDER BY entity.time DESC";

    if (maxDate != null) {
      return createQuery(sQuery, applicationId, stageTypeId, minDate, maxDate).forceSingleResult().get();
    }
    else {
      return createQuery(sQuery, applicationId, stageTypeId, minDate).forceSingleResult().get();
    }
  }

  public List<PolicyEvaluation> getLimitedAmountByApplicationId(
      String applicationId,
      int maxResultsToReturn,
      String stage)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1 " + //
        (stage != null ? " AND entity.stageTypeId = ?2 " : "") +
        " AND entity.isForObsoleteScan = false" + //
        " ORDER BY entity.time DESC";
    Query<PolicyEvaluation> query =
        stage != null
            ? new Query<>(sQuery, applicationId, stage)
            : new Query<>(sQuery, applicationId);
    query.setMaxResults(maxResultsToReturn);
    return query.getList();
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
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" +
        " WHERE entity.applicationId = ?1 " +
        " AND entity.isForObsoleteScan = false" +
        " ORDER BY entity.time, entity.id";
    int offset = (page - 1) * pageSize;
    jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, applicationId);
    return paginationQuery.getResultList();
  }

  /**
   * Fetches the latest policy evaluation for the given application, commit hash and stage, if any.
   * It returns {@code null} if no matches are found, or if commit hash is blank/missing.
   */
  public PolicyEvaluation getLastByApplicationIdCommitHashAndStageId(
      String applicationId,
      String commitHash,
      String stageTypeId)
  {
    if (StringUtils.isBlank(commitHash)) {
      return null;
    }
    String sQuery = "SELECT entity FROM PolicyEvaluation entity " + //
        "WHERE entity.applicationId = ?1 " + //
        "AND entity.commitHash = ?2 " + //
        "AND entity.stageTypeId = ?3 " +
        "ORDER BY entity.time DESC";
    return createQuery(sQuery, applicationId, commitHash, stageTypeId).forceSingleResult().get();
  }

  public PolicyEvaluation getLastByApplicationAndCommitHashAndTriggerType(
      final String applicationId,
      final String commitHash,
      final boolean externallyTriggered)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.commitHash=?1" + //
        " AND entity.applicationId=?2" + //
        " AND entity.scanTriggerType ";
    if (externallyTriggered) {
      sQuery += "NOT ";
    }
    sQuery += "IN (?3) ORDER BY entity.time DESC";
    return createQuery(sQuery, commitHash, applicationId, ScanTriggerType.internalScanTypes).forceSingleResult().get();
  }

  public int getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
      final Date lowerBound, final Date upperBoundDate
  )
  {
    // we do not allow new entries to be created with isForObsolete scan to be true unless isReevaluation is
    // also true, so in most cases entity.isForObsoleteScan = false should be redundant
    // This only enforced in the dao. it's unknown if this has always been enforced, so I am leaving
    // the extra check out of caution
    String sQuery = "SELECT COUNT(DISTINCT entity.applicationId)" +
        " FROM PolicyEvaluation entity" +
        " WHERE entity.stageTypeId = ?1" +
        " AND entity.isReevaluation = false" +
        " AND entity.isForMonitoring = false" +
        " AND entity.isForObsoleteScan = false" +
        " AND entity.time >= ?2" +
        " AND entity.time <= ?3";
    return getSingle(Number.class, sQuery, Stage.ID_BUILD, lowerBound, upperBoundDate)
        .intValue();
  }

  // The point of a cut off here is so that one off anomalous scans such as local iq-cli runs are filtered out over
  // time. The exact cutoff can be determined by application logic in the service layer
  public boolean hasCIIntegrationEvaluation(final String applicationId, final Date cutOffDate) {
    // we do not allow new entries to be created with isForObsolete scan to be true unless isReevaluation is
    // also true, so in most cases entity.isForObsoleteScan = false should be redundant
    // This only enforced in the dao. it's unknown if this has always been enforced, so I am leaving
    // the extra check out of caution
    String sQuery = "SELECT COUNT(entity.applicationId)" +
        " FROM PolicyEvaluation entity" +
        " WHERE entity.applicationId = ?1" +
        " AND entity.stageTypeId = ?2" +
        " AND entity.isReevaluation = false" +
        " AND entity.isForMonitoring = false" +
        " AND entity.isForObsoleteScan = false" +
        " AND entity.time >= ?3";
    return getSingle(Long.class, sQuery, applicationId, Stage.ID_BUILD, cutOffDate) > 0;
  }
}
