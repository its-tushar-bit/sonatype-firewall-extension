/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SourceControlPullRequestCommentDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestComment>
{
  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlPullRequestComment entity ";

  @Override
  public SourceControlPullRequestComment getById(String id) {
    return get(SELECT_ENTITY + "WHERE entity.id=?1", id);
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
        applicationInternalId, pullRequestId
    );
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
        applicationInternalId, pullRequestId
    );
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
        applicationInternalId, componentHash, pullRequestId
    );
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
        pullRequestId)) {
      delete(ctx, comment);
    }
  }

  public List<SourceControlPullRequestComment> getAll() {
    return getList(SELECT_ENTITY);
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

  /**
   * This method deletes all comment entries that are associated with the given policy evaluation ID, whether that
   * represents the source or target policy evaluation.
   */
  public void deleteByPolicyEvaluationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByPolicyEvaluationId(tx, id);
      tx.commit();
    }
  }

  /**
   * This method deletes all comment entries that are associated with the given policy evaluation ID, whether that
   * represents the source or target policy evaluation.
   */
  public void deleteByPolicyEvaluationId(final TransactionContext tx, final String id) {
    for (SourceControlPullRequestComment pullRequestComment : getByPolicyEvaluationId(tx, id)) {
      delete(tx, pullRequestComment);
    }
  }

  /**
   * This method fetches all comment entries associated with the given policy evaluation ID, whether that represents
   * the source or target policy evaluation.
   */
  public List<SourceControlPullRequestComment> getByPolicyEvaluationId(final TransactionContext tx, final String id) {
    return getList(
        tx,
        SELECT_ENTITY + "WHERE entity.sourcePolicyEvaluationId=?1 OR entity.targetPolicyEvaluationId=?1",
        id);
  }

  public List<SourceControlPullRequestComment> getByApplicationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, id);
    }
  }

  @Override
  public void update(final TransactionContext tx, final SourceControlPullRequestComment pullRequestComment) {
    pullRequestComment.setUpdateTime(new Date());
    super.update(tx, pullRequestComment);
  }
}
