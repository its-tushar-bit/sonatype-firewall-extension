/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SourceControlDefaultBranchCommitHistoryDAO
    extends AbstractOperationalSqlDAO<SourceControlDefaultBranchCommitHistory>
{
  private static final int DELETE_BATCH_SIZE = 100;

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlDefaultBranchCommitHistory entity ";

  @Override
  public SourceControlDefaultBranchCommitHistory getById(final String id) {
    return get(SELECT_ENTITY + "WHERE entity.id=?1", id);
  }

  public List<SourceControlDefaultBranchCommitHistory> getAll() {
    return getList(SELECT_ENTITY);
  }

  public List<SourceControlDefaultBranchCommitHistory> getByApplicationIdSortedByDateDesc(String applicationId) {
    return getList(SELECT_ENTITY + "WHERE entity.applicationId=?1" +
        "ORDER BY entity.commitTime DESC", applicationId);
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
        "ORDER BY entity.commitTime DESC";
    return createQuery(sQuery, applicationId).setMaxResults(1).get();
  }

  public void deleteByApplicationIdBeforeCommitTime(
      final String applicationId,
      final Date commitTime)
  {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationIdBeforeCommitTime(tx, applicationId, commitTime);
      tx.commit();
    }
  }

  public void deleteByApplicationIdBeforeCommitTime(
      final TransactionContext tx,
      final String applicationId,
      final Date commitTime)
  {
    List<SourceControlDefaultBranchCommitHistory> commitHistoryList = getList(
        tx, SELECT_ENTITY + "WHERE entity.applicationId=?1 AND entity.commitTime < ?2", applicationId, commitTime);
    for (SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory : commitHistoryList) {
      delete(tx, defaultBranchCommitHistory);
    }
  }

  public void deleteByApplicationId(
      final TransactionContext tx,
      final String applicationId)
  {
    List<SourceControlDefaultBranchCommitHistory> commitHistoryList = getList(
        tx, SELECT_ENTITY + "WHERE entity.applicationId=?1", applicationId);
    for (SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory : commitHistoryList) {
      delete(tx, defaultBranchCommitHistory);
    }
  }

  public void deleteByPolicyEvaluationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByPolicyEvaluationId(tx, id);
      tx.commit();
    }
  }

  public void deleteByPolicyEvaluationId(final TransactionContext tx, final String id) {
    for (SourceControlDefaultBranchCommitHistory commitHistory : getByPolicyEvaluationId(tx, id)) {
      delete(tx, commitHistory);
    }
  }

  @Override
  public void update(
      final TransactionContext tx,
      final SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory)
  {
    defaultBranchCommitHistory.setUpdateTime(new Date());
    super.update(tx, defaultBranchCommitHistory);
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
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
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlDefaultBranchCommitHistory entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(entity);
  }
}
