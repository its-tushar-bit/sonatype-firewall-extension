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
  @Override
  public SourceControlPullRequestComment getById(String id) {
    return get("SELECT entity FROM SourceControlPullRequestComment entity WHERE entity.id=?1", id);
  }

  public SourceControlPullRequestComment getByApplicationIdAndPullRequestId(
      String applicationInternalId,
      int pullRequestId)
  {
    return get(
        "SELECT entity FROM SourceControlPullRequestComment entity " +
            "WHERE entity.applicationId=?1 AND entity.pullRequestId=?2",
        applicationInternalId, pullRequestId
    );
  }

  public List<SourceControlPullRequestComment> getAll() {
    return getList("SELECT entity FROM SourceControlPullRequestComment entity");
  }

  public List<SourceControlPullRequestComment> getByApplicationId(final TransactionContext tx, final String id) {
    return getList(
        tx,
        "SELECT entity FROM SourceControlPullRequestComment entity WHERE entity.applicationId=?1",
        id);
  }

  public void deleteByPolicyEvaluationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByPolicyEvaluationId(tx, id);
      tx.commit();
    }
  }

  public void deleteByPolicyEvaluationId(final TransactionContext tx, final String id) {
    for (SourceControlPullRequestComment pullRequestComment : getByPolicyEvaluationId(tx, id)) {
      delete(tx, pullRequestComment);
    }
  }

  public List<SourceControlPullRequestComment> getByPolicyEvaluationId(final TransactionContext tx, final String id) {
    return getList(
        tx,
        "SELECT entity FROM SourceControlPullRequestComment entity " +
            "WHERE entity.sourcePolicyEvaluationId=?1 OR entity.targetPolicyEvaluationId=?1",
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
