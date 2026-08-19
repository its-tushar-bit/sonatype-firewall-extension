/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;
import com.sonatype.insight.brain.jooq.generated.ods.tables.records.PolicyEvaluationRecord;

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

  public PolicyEvaluation getLastByOwnerIdAndScanId(TransactionContext tx, String ownerId, String scanId) {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
        .and(POLICY_EVALUATION.SCAN_ID.eq(scanId))
        .orderBy(POLICY_EVALUATION.TIME.desc())
        .limit(1)
        .fetchOne());
  }

  public PolicyEvaluation getLastByOwnerIdAndScanId(String ownerId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByOwnerIdAndScanId(tx, ownerId, scanId);
    }
  }

  public PolicyEvaluation getLastByOwnerIdAndScanIdNotNull(String ownerId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByOwnerIdAndScanIdNotNull(tx, ownerId, scanId);
    }
  }

  public PolicyEvaluation getLastByOwnerIdAndScanIdNotNull(TransactionContext tx, String ownerId, String scanId) {
    PolicyEvaluation policyEvaluation = getLastByOwnerIdAndScanId(tx, ownerId, scanId);
    if (policyEvaluation == null) {
      throw new NotFoundException(
          "PolicyEvaluation for ownerId " + ownerId + " and scanId " + scanId + " does not exist.");
    }
    return policyEvaluation;
  }

  public List<PolicyEvaluation> getLastByOwnerIds(Set<String> ownerIds) {
    if (ownerIds.size() >= getInOperatorThreshold()) {
      return getLastByOwnerIdsManualFilter(ownerIds);
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(LAST_POLICY_EVALUATION.OWNER_ID.in(ownerIds))
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
  private List<PolicyEvaluation> getLastByOwnerIdsManualFilter(Set<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      List<PolicyEvaluation> allEvals = tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));

      List<PolicyEvaluation> evals = new ArrayList<>(ownerIds.size());
      for (PolicyEvaluation eval : allEvals) {
        if (ownerIds.contains(eval.getOwnerId())) {
          evals.add(eval);
        }
      }
      return evals;
    }
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage.
   */
  public PolicyEvaluation getLastByOwnerIdAndStageId(TransactionContext tx, String ownerId, String stageTypeId) {
    Record record = tx.dsl()
        .select(POLICY_EVALUATION.fields())
        .from(POLICY_EVALUATION)
        .join(LAST_POLICY_EVALUATION)
        .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
        .where(LAST_POLICY_EVALUATION.OWNER_ID.eq(ownerId))
        .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
        .fetchOne();
    return record != null ? toEntity(record.into(POLICY_EVALUATION)) : null;
  }

  public List<PolicyEvaluation> getLastByOwnerIdsAndStageIds(Set<String> ownerIds, Set<String> stageTypeIds) {
    if (ownerIds.size() >= getInOperatorThreshold()) {
      return getLastByOwnerIdsAndStageIdsManualFilter(ownerIds, stageTypeIds);
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_EVALUATION.fields())
          .from(POLICY_EVALUATION)
          .join(LAST_POLICY_EVALUATION)
          .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
          .where(LAST_POLICY_EVALUATION.OWNER_ID.in(ownerIds))
          .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.in(stageTypeIds))
          .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));
    }
  }

  /**
   * H2-specific optimization, see comment on {@link #getLastByOwnerIdsManualFilter(Set)} for more details.
   */
  private List<PolicyEvaluation> getLastByOwnerIdsAndStageIdsManualFilter(
      Set<String> ownerIds,
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

      List<PolicyEvaluation> evals = new ArrayList<>(ownerIds.size());
      for (PolicyEvaluation eval : allEvals) {
        if (ownerIds.contains(eval.getOwnerId())) {
          evals.add(eval);
        }
      }
      return evals;
    }
  }

  public PolicyEvaluation getLastByOwnerIdAndStageId(String ownerId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByOwnerIdAndStageId(tx, ownerId, stageTypeId);
    }
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByOwnerIdAndStageId(
      TransactionContext tx,
      String ownerId,
      String stageTypeId)
  {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
        .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
        .and(POLICY_EVALUATION.REEVALUATION.eq(false))
        .orderBy(POLICY_EVALUATION.TIME.desc())
        .limit(1)
        .fetchOne());
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByOwnerIdAndStageId(String ownerId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastPrimaryByOwnerIdAndStageId(tx, ownerId, stageTypeId);
    }
  }

  /**
   * Batch variant of {@link #getLastPrimaryByOwnerIdAndStageId(String, String)}: returns the last primary
   * evaluation (i.e. not a reevaluation) for each of the given applications and the given stage, keyed by application
   * id. Applications without a primary evaluation are absent from the map.
   * <p>
   * Unlike {@link #getLastByOwnerIdsAndStageIds(Set, Set)} (which joins {@code LAST_POLICY_EVALUATION} and can
   * therefore return a reevaluation), this method preserves the "last primary" semantics. The most recent primary per
   * application is selected with a {@code GROUP BY application_id} max-time subquery joined back to the matching row
   * (a portable greatest-n-per-group, avoiding a window function so the query works on both PostgreSQL and the
   * embedded H2 database). This returns at most a couple of rows per application instead of its full primary-
   * evaluation history, and the {@code (reevaluation, stage_type_id, application_id, time)} index covers the subquery.
   */
  public Map<String, PolicyEvaluation> getLastPrimaryByOwnerIdsAndStageId(
      Set<String> ownerIds,
      String stageTypeId)
  {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return Map.of();
    }
    List<PolicyEvaluation> latestPrimaries = getListWithSqlInClause(
        ownerIds,
        appIdChunk -> {
          try (TransactionContext tx = createTransactionContext()) {
            var maxTime = DSL.max(POLICY_EVALUATION.TIME).as("max_time");
            Table<?> latest = tx.dsl()
                .select(POLICY_EVALUATION.OWNER_ID, maxTime)
                .from(POLICY_EVALUATION)
                .where(POLICY_EVALUATION.OWNER_ID.in(appIdChunk))
                .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
                .and(POLICY_EVALUATION.REEVALUATION.eq(false))
                .groupBy(POLICY_EVALUATION.OWNER_ID)
                .asTable("latest");
            return tx.dsl()
                .select(POLICY_EVALUATION.fields())
                .from(POLICY_EVALUATION)
                .join(latest)
                .on(POLICY_EVALUATION.OWNER_ID.eq(latest.field(POLICY_EVALUATION.OWNER_ID)))
                .and(POLICY_EVALUATION.TIME.eq(latest.field(maxTime)))
                // Repeat the chunk predicate on the outer query (not just the JOIN) so planners that don't push
                // the JOIN condition into an index scan -- notably H2 -- still filter on OWNER_ID directly.
                .where(POLICY_EVALUATION.OWNER_ID.in(appIdChunk))
                .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
                .and(POLICY_EVALUATION.REEVALUATION.eq(false))
                .fetch(r -> toEntity(r.into(POLICY_EVALUATION)));
          }
        },
        getDataStore(),
        // Each chunk element is bound twice (the subquery IN and the outer IN); the 100-param buffer covers the
        // handful of constant predicates (stage type, reevaluation) on both queries.
        2,
        100);

    Map<String, PolicyEvaluation> latestByApp = new HashMap<>();
    for (PolicyEvaluation eval : latestPrimaries) {
      // The subquery returns one row per application unless two primaries share the exact max time, in which case
      // the winner is arbitrary -- as it is for the single-application getLastPrimaryByOwnerIdAndStageId, which
      // orders only by time desc with no tiebreaker.
      latestByApp.putIfAbsent(eval.getOwnerId(), eval);
    }
    return latestByApp;
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage, excluding
   * continuous monitoring and reevaluations.
   */
  public PolicyEvaluation getLastByOwnerIdAndStageIdNoMonitoringNoReeval(
      final String ownerId,
      final String stageTypeId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  @Override
  public int insert(TransactionContext tx, PolicyEvaluation policyEvaluation) {
    validate(policyEvaluation);

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
    }

    if (policyEvaluation.getId() == null) {
      policyEvaluation.setId(UUID.randomUUID().toString());
    }

    int inserted = super.insert(tx, policyEvaluation);

    if (policyEvaluation.isForObsoleteScan()) {
      return inserted;
    }

    // Update the last policy evaluation record
    String ownerId = policyEvaluation.getOwnerId();
    String stageTypeId = policyEvaluation.getStageTypeId();
    PolicyEvaluation lastPolicyEvaluation = getLastByOwnerIdAndStageId(tx, ownerId, stageTypeId);
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
      lastPolicyEvaluationDAO.insert(tx, new LastPolicyEvaluation(policyEvaluation.getId(), ownerId, stageTypeId));
    }

    return inserted;
  }

  /**
   * @since 1.39
   */
  public List<PolicyEvaluation> getBetweenDatesByOwnerIdAndStageIds(
      Date sinceDate,
      Date toDate,
      String ownerId,
      Set<String> stageTypeIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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
  public PolicyEvaluation getOldestByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .orderBy(POLICY_EVALUATION.TIME)
          .limit(1)
          .fetchOne());
    }
  }

  @Override
  public int update(TransactionContext tx, PolicyEvaluation entity) {
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
          getNewestPolicyEvaluation(tx, policyEvaluation.getOwnerId(), policyEvaluation.getStageTypeId());
      lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(tx, newestPolicyEvaluation);
    }
  }

  private PolicyEvaluation getNewestPolicyEvaluation(TransactionContext tx, String ownerId, String stageTypeId) {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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

  public int getCountByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getCountByOwnerId(tx, ownerId);
    }
  }

  public int getCountByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectCount()
        .from(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
        .fetchOne(0, Integer.class);
  }

  public void deleteByOwnerId(TransactionContext tx, String ownerId) {
    tx.dsl()
        .deleteFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
        .execute();
  }

  public void deleteByOwnerIds(TransactionContext tx, Collection<String> ownerIds) {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return;
    }
    getListWithSqlInClause(ownerIds, idChunk -> List.of(tx.dsl()
        .deleteFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.in(idChunk))
        .execute()), getDataStore());
  }

  public List<PolicyEvaluation> getPrimaryNonMonitoringByOwnerIdAndStageId(String ownerId, String stageId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageId))
          .and(POLICY_EVALUATION.FOR_MONITORING.eq(false))
          .and(POLICY_EVALUATION.REEVALUATION.eq(false))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.ne(StageTypes.COMPLIANCE.getId()))
          .fetch(this::toEntity);
    }
  }

  private static final List<String> stageList = Arrays.asList(Stage.ID_SOURCE, Stage.ID_BUILD, Stage.ID_DEVELOP);

  public boolean hasExternalPolicyEvaluations(String ownerId, Date cutoffTime) {
    try (TransactionContext tx = createTransactionContext()) {
      List<String> internalScanTypeStrings = ScanTriggerType.internalScanTypes.stream()
          .map(ScanTriggerType::toString)
          .toList();
      int count = tx.dsl()
          .selectCount()
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .and(POLICY_EVALUATION.TIME.gt(cutoffTime))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.in(stageList))
          .and(POLICY_EVALUATION.SCAN_TRIGGER_TYPE.notIn(internalScanTypeStrings))
          .fetchOne(0, Integer.class);
      return count > 0;
    }
  }

  public List<PolicyEvaluation> getPrimaryForMonitoringByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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
      List<String> ownerIdsForCommit = tx.dsl()
          .selectDistinct(POLICY_EVALUATION.OWNER_ID)
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .fetchInto(String.class);

      List<PolicyEvaluation> result = new ArrayList<>();
      ownerIdsForCommit.forEach(id -> result.add(getLastByApplicationAndCommitHash(id, commitHash)));
      return result;
    }
  }

  public PolicyEvaluation getLastByApplicationAndCommitHash(
      final String ownerInternalId,
      final String commitHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerInternalId))
          .and(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastByApplicationAndAbbreviatedCommitHash(
      final String ownerInternalId,
      final String commitHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerInternalId))
          .and(POLICY_EVALUATION.COMMIT_HASH.like(commitHash + "%"))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastInTimeRangeByApplicationAndStage(
      String ownerId,
      String stageTypeId,
      Date minDate,
      Date maxDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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

  /**
   * Latest evaluation for each requested window, resolved in a single round-trip. For every
   * {@link StageEvaluationWindow} this returns the most recent evaluation for the application on that stage whose time
   * falls in the half-open range {@code [minDate, maxDate)} (a null {@code maxDate} leaves the window open-ended), or
   * no
   * row when nothing matches. Each window contributes at most one row, so the result size is bounded by the number of
   * distinct windows regardless of how much scan history the application has. Duplicate windows are coalesced.
   */
  public List<PolicyEvaluation> getLatestEvaluationPerWindow(
      String ownerId,
      Collection<StageEvaluationWindow> windows)
  {
    if (windows == null || windows.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      Select<PolicyEvaluationRecord> combined = null;
      for (StageEvaluationWindow window : new LinkedHashSet<>(windows)) {
        var condition = POLICY_EVALUATION.OWNER_ID.eq(ownerId)
            .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(window.stageTypeId()))
            .and(POLICY_EVALUATION.TIME.ge(window.minDate()));
        if (window.maxDate() != null) {
          condition = condition.and(POLICY_EVALUATION.TIME.lt(window.maxDate()));
        }

        Select<PolicyEvaluationRecord> latestForWindow = tx.dsl()
            .selectFrom(POLICY_EVALUATION)
            .where(condition)
            .orderBy(POLICY_EVALUATION.TIME.desc())
            .limit(1);

        combined = combined == null ? latestForWindow : combined.unionAll(latestForWindow);
      }

      return combined == null ? Collections.emptyList() : combined.fetch(this::toEntity);
    }
  }

  /**
   * A per-violation lookup window: the latest evaluation on {@code stageTypeId} with time in
   * {@code [minDate, maxDate)}.
   * A null {@code maxDate} leaves the upper bound open.
   */
  public record StageEvaluationWindow(String stageTypeId, Date minDate, Date maxDate)
  {
  }

  public List<PolicyEvaluation> getLimitedAmountByOwnerId(
      String ownerId,
      int maxResultsToReturn,
      String stage)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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

  public List<PolicyEvaluation> getByOwnerId(
      final String ownerId,
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId, page, pageSize);
    }
  }

  public List<PolicyEvaluation> getByOwnerId(
      final TransactionContext tx,
      final String ownerId,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(POLICY_EVALUATION)
        .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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
   * @param ownerId the owner ID to scope the search
   * @return the matching PolicyEvaluation, or {@code null} if not found
   * @since 1.203
   */
  public PolicyEvaluation getByScanIdAndApplicationId(String scanId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.SCAN_ID.eq(scanId))
          .and(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .fetchOne(this::toEntity);
    }
  }

  /**
   * Fetches the latest policy evaluation for the given application, commit hash and stage, if any. It returns
   * {@code null} if no matches are found, or if commit hash is blank/missing.
   */
  public PolicyEvaluation getLastByOwnerIdCommitHashAndStageId(
      String ownerId,
      String commitHash,
      String stageTypeId)
  {
    if (StringUtils.isBlank(commitHash)) {
      return null;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
          .and(POLICY_EVALUATION.COMMIT_HASH.eq(commitHash))
          .and(POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .orderBy(POLICY_EVALUATION.TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public PolicyEvaluation getLastByApplicationAndCommitHashAndTriggerType(
      final String ownerId,
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
          .and(POLICY_EVALUATION.OWNER_ID.eq(ownerId));

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
          .select(DSL.countDistinct(POLICY_EVALUATION.OWNER_ID))
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
  public boolean hasCIIntegrationEvaluation(final String ownerId, final Date cutOffDate) {
    // we do not allow new entries to be created with isForObsolete scan to be true unless isReevaluation is
    // also true, so in most cases entity.isForObsoleteScan = false should be redundant
    // This only enforced in the dao. it's unknown if this has always been enforced, so I am leaving
    // the extra check out of caution
    try (TransactionContext tx = createTransactionContext()) {
      int count = tx.dsl()
          .selectCount()
          .from(POLICY_EVALUATION)
          .where(POLICY_EVALUATION.OWNER_ID.eq(ownerId))
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
