/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SourceControlDefaultBranchCommitHistoryDAO
    extends AbstractOperationalSqlDAO<SourceControlDefaultBranchCommitHistory>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlDefaultBranchCommitHistoryDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlDefaultBranchCommitHistory entity ";

  public static final String WHERE_ENTITY_APPLICATION_ID_1 = "WHERE entity.applicationId=?1 ";

  public static final String ORDER_BY_ENTITY_COMMIT_TIME_DESC = "ORDER BY entity.commitTime DESC";

  @Inject
  public SourceControlDefaultBranchCommitHistoryDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<SourceControlDefaultBranchCommitHistory> getByApplicationIdSortedByDateDesc(String applicationId) {
    return getList(SELECT_ENTITY + WHERE_ENTITY_APPLICATION_ID_1 +
        ORDER_BY_ENTITY_COMMIT_TIME_DESC, applicationId);
  }

  public SourceControlDefaultBranchCommitHistory getLatestCommitForApplicationId(String applicationId) {
    return createQuery(SELECT_ENTITY + WHERE_ENTITY_APPLICATION_ID_1 +
        ORDER_BY_ENTITY_COMMIT_TIME_DESC, applicationId).forceSingleResult().get();
  }

  public SourceControlDefaultBranchCommitHistory getByApplicationIdAndCommitHash(
      String applicationId,
      String commitHash)
  {
    return get(
        SELECT_ENTITY + "WHERE entity.applicationId=?1 AND entity.commitHash=?2",
        applicationId,
        commitHash
    );
  }

  public SourceControlDefaultBranchCommitHistory getByApplicationIdAndPolicyEvaluationId(
      String applicationId,
      String policyEvaluationId)
  {
    return get(
        SELECT_ENTITY + "WHERE entity.applicationId=?1 AND entity.policyEvaluationId=?2",
        applicationId,
        policyEvaluationId
    );
  }

  public List<SourceControlDefaultBranchCommitHistory> getByPolicyEvaluationId(
      final TransactionContext tx,
      final String policyEvaluationId)
  {
    return getList(
        tx,
        SELECT_ENTITY + "WHERE entity.policyEvaluationId=?1",
        policyEvaluationId);
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
    String sQuery = SELECT_ENTITY +
        "WHERE entity.applicationId=?1 AND entity.policyEvaluationId IS NOT NULL " +
        ORDER_BY_ENTITY_COMMIT_TIME_DESC;
    return createQuery(sQuery, applicationId).setMaxResults(1).get();
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
    String sQuery = "SELECT h " +
        "FROM SourceControlDefaultBranchCommitHistory h, PolicyEvaluation p " +
        "WHERE h.policyEvaluationId = p.id " +
        "AND h.applicationId=?1 AND p.scanTriggerType ";
    if (externallyTriggered) {
      sQuery += "NOT ";
    }
    sQuery += "IN (?2) ORDER BY h.commitTime DESC";

    return createQuery(sQuery, applicationId, ScanTriggerType.internalScanTypes).forceSingleResult().get();
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
        getList(tx, SELECT_ENTITY + WHERE_ENTITY_APPLICATION_ID_1, applicationId);
    for (SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory : commitHistoryList) {
      delete(tx, defaultBranchCommitHistory);
    }
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

    String sQuery = "SELECT entity.id FROM SourceControlDefaultBranchCommitHistory entity" +
        " WHERE entity.updateTime < ?1 OR (entity.updateTime is null AND entity.createTime < ?2)";
    int deletedRows = 0;
    while (true) {
      List<String> ids =
          new Query<String>(sQuery, cutoffDate, cutoffDate).setMaxResults(DELETE_BATCH_SIZE).getList();
      if (ids.isEmpty()) {
        return deletedRows;
      }
      deletedRows +=
          createQuery("DELETE FROM SourceControlDefaultBranchCommitHistory entity WHERE entity.id IN (?1)", ids)
              .executeUpdate();
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
  public final void delete(SourceControlDefaultBranchCommitHistory entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(entity);
  }
}
