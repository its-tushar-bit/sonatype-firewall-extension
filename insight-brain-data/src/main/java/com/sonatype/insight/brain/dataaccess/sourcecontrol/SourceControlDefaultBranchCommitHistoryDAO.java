/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlDefaultBranchCommitHistory.SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY;

@Named
@Singleton
public class SourceControlDefaultBranchCommitHistoryDAO
    extends AbstractOperationalSqlDAO<SourceControlDefaultBranchCommitHistory>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlDefaultBranchCommitHistoryDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  @Inject
  public SourceControlDefaultBranchCommitHistoryDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<SourceControlDefaultBranchCommitHistory> getByApplicationIdSortedByDateDesc(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .orderBy(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_TIME.desc())
          .fetch(this::toEntity);
    }
  }

  public SourceControlDefaultBranchCommitHistory getLatestCommitForApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .orderBy(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public SourceControlDefaultBranchCommitHistory getByApplicationIdAndCommitHash(
      String applicationId,
      String commitHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_HASH.eq(commitHash))
          .fetchOne());
    }
  }

  public SourceControlDefaultBranchCommitHistory getByApplicationIdAndPolicyEvaluationId(
      String applicationId,
      String policyEvaluationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.POLICY_EVALUATION_ID.eq(policyEvaluationId))
          .fetchOne());
    }
  }

  public List<SourceControlDefaultBranchCommitHistory> getByPolicyEvaluationId(
      final TransactionContext tx,
      final String policyEvaluationId)
  {
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
        .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.POLICY_EVALUATION_ID.eq(policyEvaluationId))
        .fetch(this::toEntity);
  }

  public List<SourceControlDefaultBranchCommitHistory> getByPolicyEvaluationId(final String policyEvaluationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyEvaluationId(tx, policyEvaluationId);
    }
  }

  /**
   * Fetch the default branch commit history entry for the latest commit that has a policy evaluation
   *
   * @param applicationId represents the application to which the default branch commit history pertains
   * @return the entry that has a policy evaluation with the most recent commit time or null if no such entry exists
   */
  public SourceControlDefaultBranchCommitHistory getByApplicationIdForLatestCommitWithPolicyEvaluation(
      final String applicationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.POLICY_EVALUATION_ID.isNotNull())
          .orderBy(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_TIME.desc())
          .limit(1)
          .fetchOne());
    }
  }

  /**
   * Fetch the default branch commit history entry for the latest commit that has a policy evaluation
   *
   * @param applicationId represents the application to which the default branch commit history pertains
   * @param externallyTriggered specifies the type of policy evaluation
   * @return the entry that has a policy evaluation with the most recent commit time or null if no such entry exists
   */
  public SourceControlDefaultBranchCommitHistory getForLatestCommitWithPolicyEvaluation(
      final String applicationId,
      final boolean externallyTriggered)
  {
    Set<String> internalScanTypeNames = ScanTriggerType.internalScanTypes.stream()
        .map(Enum::name)
        .collect(java.util.stream.Collectors.toSet());

    try (TransactionContext tx = createTransactionContext()) {
      var policyEvaluationTable =
          com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;

      var scanTriggerCondition = externallyTriggered
          ? policyEvaluationTable.SCAN_TRIGGER_TYPE.notIn(internalScanTypeNames)
          : policyEvaluationTable.SCAN_TRIGGER_TYPE.in(internalScanTypeNames);

      Record record = tx.dsl()
          .select(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.asterisk())
          .from(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
          .join(policyEvaluationTable)
          .on(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.POLICY_EVALUATION_ID.eq(
              policyEvaluationTable.POLICY_EVALUATION_ID))
          .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
          .and(scanTriggerCondition)
          .orderBy(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_TIME.desc())
          .limit(1)
          .fetchOne();
      return record != null ? toEntity(record.into(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)) : null;
    }
  }

  public void deleteByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    List<SourceControlDefaultBranchCommitHistory> commitHistoryList =
        tx.dsl()
            .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
            .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(applicationId))
            .fetch(this::toEntity);
    for (SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory : commitHistoryList) {
      delete(tx, defaultBranchCommitHistory);
    }
  }

  @Override
  public int insert(TransactionContext tx, SourceControlDefaultBranchCommitHistory entity) {
    // Check for existing record with same (application_id, commit_hash) to handle unique constraint
    SourceControlDefaultBranchCommitHistory existing = toEntity(
        tx.dsl()
            .selectFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
            .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.APPLICATION_ID.eq(entity.getApplicationId()))
            .and(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.COMMIT_HASH.eq(entity.getCommitHash()))
            .fetchOne());

    if (existing != null) {
      // Update existing record instead of inserting duplicate
      entity.setId(existing.getId());
      entity.setCreateTime(existing.getCreateTime());
      update(tx, entity);
      return 0;
    }

    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }
    return super.insert(tx, entity);
  }

  @Override
  public void update(
      final TransactionContext tx,
      final SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory)
  {
    log.debug("Updating SourceControlDefaultBranchCommitHistory with id {} for application id {}.",
        defaultBranchCommitHistory.getId(), defaultBranchCommitHistory.getApplicationId());

    defaultBranchCommitHistory.setUpdateTime(new Date());
    super.update(tx, defaultBranchCommitHistory);
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    log.debug("Deleting all SourceControlDefaultBranchCommitHistory before {}.", cutoffDate);

    int deletedRows = 0;
    while (true) {
      try (TransactionContext tx = createTransactionContext()) {
        List<String> ids = tx.dsl()
            .select(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY_ID)
            .from(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
            .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.UPDATE_TIME.lt(cutoffDate)
                .or(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.UPDATE_TIME.isNull()
                    .and(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.CREATE_TIME.lt(cutoffDate))))
            .limit(DELETE_BATCH_SIZE)
            .fetchInto(String.class);
        if (ids.isEmpty()) {
          return deletedRows;
        }
        tx.begin();
        deletedRows += tx.dsl()
            .deleteFrom(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY)
            .where(SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY.SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY_ID.in(ids))
            .execute();
        tx.commit();
      }
    }
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlDefaultBranchCommitHistory entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    log.debug("Deleting SourceControlDefaultBranchCommitHistory with id {} for application id {}.", entity.getId(),
        entity.getApplicationId());
    super.delete(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_DEFAULT_BRANCH_COMMIT_HISTORY;
  }

  @Override
  public Class<SourceControlDefaultBranchCommitHistory> getEntityClass() {
    return SourceControlDefaultBranchCommitHistory.class;
  }
}
