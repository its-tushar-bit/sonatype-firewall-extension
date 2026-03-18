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
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SourceControlPullRequestCommentDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestComment>
{
  private static final int DELETE_BATCH_SIZE = 100;

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlPullRequestComment entity ";

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public SourceControlPullRequestCommentDAO(
      final OperationalDataStore operationalDataStore,
      final PolicyEvaluationDAO policyEvaluationDAO)
  {
    super(operationalDataStore);
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  /**
   * This method fetches the overall comment for the given application and pull request.
   */
  public SourceControlPullRequestComment getByApplicationIdAndPullRequestIdWithoutComponent(
      String applicationInternalId,
      int pullRequestId)
  {
    return get(
        SELECT_ENTITY + "WHERE entity.applicationId=?1 AND entity.pullRequestId=?2 AND entity.componentHash IS NULL",
        applicationInternalId, pullRequestId);
  }

  /**
   * This method fetches all the comments associated with the given application and pull request that also
   * have a component hash assigned, thus making them line-level comments.
   */
  public List<SourceControlPullRequestComment> getByApplicationIdAndPullRequestIdWithComponents(
      String applicationInternalId,
      int pullRequestId)
  {
    return getList(
        SELECT_ENTITY
            + "WHERE entity.applicationId=?1 AND entity.pullRequestId=?2 AND entity.componentHash IS NOT NULL",
        applicationInternalId, pullRequestId);
  }

  /**
   * This method fetches a particular PR line comment entry as identified by the given application, component
   * hash and pull request.
   */
  public SourceControlPullRequestComment getByApplicationIdAndComponentAndPullRequestId(
      String applicationInternalId,
      String componentHash,
      int pullRequestId)
  {
    return get(
        SELECT_ENTITY + "WHERE entity.applicationId=?1 AND entity.componentHash=?2 AND entity.pullRequestId=?3",
        applicationInternalId, componentHash, pullRequestId);
  }

  /**
   * This method deletes all the line-level comments, but not the overall comment, for the given application and
   * pull request.
   */
  public void deleteByApplicationIdAndPullRequestIdWithComponents(String applicationId, int pullRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationIdAndPullRequestIdWithComponents(tx, applicationId, pullRequestId);
      tx.commit();
    }
  }

  /**
   * This method deletes all the line-level comments, but not the overall comment, for the given application and
   * pull request.
   */
  public void deleteByApplicationIdAndPullRequestIdWithComponents(
      TransactionContext ctx,
      String applicationId,
      int pullRequestId)
  {
    for (SourceControlPullRequestComment comment : getByApplicationIdAndPullRequestIdWithComponents(applicationId,
        pullRequestId))
    {
      delete(ctx, comment);
    }
  }

  /**
   * This method fetches ALL comment entries (line and overall) for the given application and pull request
   */
  public List<SourceControlPullRequestComment> getByApplicationId(final TransactionContext tx, final String id) {
    return getList(
        tx,
        SELECT_ENTITY + "WHERE entity.applicationId=?1",
        id);
  }

  public List<SourceControlPullRequestComment> getByApplicationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, id);
    }
  }

  @Override
  public void update(final TransactionContext tx, final SourceControlPullRequestComment pullRequestComment) {
    validateOwnership(tx, pullRequestComment);

    pullRequestComment.setUpdateTime(new Date());
    super.update(tx, pullRequestComment);
  }

  @Override
  public void insert(TransactionContext tx, SourceControlPullRequestComment pullRequestComment) {
    validateOwnership(tx, pullRequestComment);

    super.insert(tx, pullRequestComment);
  }

  private void validateOwnership(TransactionContext tx, SourceControlPullRequestComment pullRequestComment) {
    PolicyEvaluation sourcePolicyEvaluation =
        policyEvaluationDAO.getByIdNotNull(tx, pullRequestComment.getSourcePolicyEvaluationId());
    if (!sourcePolicyEvaluation.getApplicationId().equals(pullRequestComment.getApplicationId())) {
      throw new DataAccessException(
          "The source policy evaluation app ID does not match the pull request comment app ID.");
    }
    PolicyEvaluation targetPolicyEvaluation =
        policyEvaluationDAO.getByIdNotNull(tx, pullRequestComment.getTargetPolicyEvaluationId());
    if (!targetPolicyEvaluation.getApplicationId().equals(pullRequestComment.getApplicationId())) {
      throw new DataAccessException(
          "The target policy evaluation app ID does not match the pull request comment app ID.");
    }
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    String sQuery = "SELECT entity.id FROM SourceControlPullRequestComment entity" +
        " WHERE entity.updateTime < ?1 OR (entity.updateTime is null AND entity.createTime < ?2)";
    int deletedRows = 0;
    while (true) {
      List<String> ids =
          new Query<String>(sQuery, cutoffDate, cutoffDate).setMaxResults(DELETE_BATCH_SIZE).getList();
      if (ids.isEmpty()) {
        return deletedRows;
      }
      deletedRows += createQuery("DELETE FROM SourceControlPullRequestComment entity WHERE entity.id IN (?1)", ids)
          .executeUpdate();
    }
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlPullRequestComment entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlPullRequestComment entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(entity);
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    for (SourceControlPullRequestComment pullRequestComment : getByApplicationId(tx, applicationId)) {
      delete(tx, pullRequestComment);
    }
  }
}
