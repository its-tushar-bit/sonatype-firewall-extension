/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlPullRequestComment.SOURCE_CONTROL_PULL_REQUEST_COMMENT;

@Named
@Singleton
public class SourceControlPullRequestCommentDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestComment>
{
  private static final int DELETE_BATCH_SIZE = 100;

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
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
          .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.APPLICATION_ID.eq(applicationInternalId))
          .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.PULL_REQUEST_ID.eq(pullRequestId))
          .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.COMPONENT_HASH.isNull())
          .fetchOne());
    }
  }

  /**
   * This method fetches all the comments associated with the given application and pull request that also
   * have a component hash assigned, thus making them line-level comments.
   */
  public List<SourceControlPullRequestComment> getByApplicationIdAndPullRequestIdWithComponents(
      TransactionContext tx,
      String applicationInternalId,
      int pullRequestId)
  {
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
        .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.APPLICATION_ID.eq(applicationInternalId))
        .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.PULL_REQUEST_ID.eq(pullRequestId))
        .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.COMPONENT_HASH.isNotNull())
        .fetch(this::toEntity);
  }

  /**
   * This method fetches all the comments associated with the given application and pull request that also
   * have a component hash assigned, thus making them line-level comments.
   */
  public List<SourceControlPullRequestComment> getByApplicationIdAndPullRequestIdWithComponents(
      String applicationInternalId,
      int pullRequestId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndPullRequestIdWithComponents(tx, applicationInternalId, pullRequestId);
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
          .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.APPLICATION_ID.eq(applicationInternalId))
          .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.COMPONENT_HASH.eq(componentHash))
          .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.PULL_REQUEST_ID.eq(pullRequestId))
          .fetchOne());
    }
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
    for (SourceControlPullRequestComment comment : getByApplicationIdAndPullRequestIdWithComponents(ctx, applicationId,
        pullRequestId))
    {
      delete(ctx, comment);
    }
  }

  /**
   * This method fetches ALL comment entries (line and overall) for the given application and pull request
   */
  public List<SourceControlPullRequestComment> getByApplicationId(final TransactionContext tx, final String id) {
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
        .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.APPLICATION_ID.eq(id))
        .fetch(this::toEntity);
  }

  public List<SourceControlPullRequestComment> getByApplicationId(final String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, id);
    }
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
    int deletedRows = 0;
    while (true) {
      try (TransactionContext tx = createTransactionContext()) {
        List<String> ids = tx.dsl()
            .select(SOURCE_CONTROL_PULL_REQUEST_COMMENT.SOURCE_CONTROL_PULL_REQUEST_COMMENT_ID)
            .from(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
            .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.UPDATE_TIME.lt(cutoffDate)
                .or(SOURCE_CONTROL_PULL_REQUEST_COMMENT.UPDATE_TIME.isNull()
                    .and(SOURCE_CONTROL_PULL_REQUEST_COMMENT.CREATE_TIME.lt(cutoffDate))))
            .limit(DELETE_BATCH_SIZE)
            .fetchInto(String.class);
        if (ids.isEmpty()) {
          return deletedRows;
        }
        tx.begin();
        deletedRows += tx.dsl()
            .deleteFrom(SOURCE_CONTROL_PULL_REQUEST_COMMENT)
            .where(SOURCE_CONTROL_PULL_REQUEST_COMMENT.SOURCE_CONTROL_PULL_REQUEST_COMMENT_ID.in(ids))
            .execute();
        tx.commit();
      }
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    for (SourceControlPullRequestComment pullRequestComment : getByApplicationId(tx, applicationId)) {
      delete(tx, pullRequestComment);
    }
  }

  @Override
  public int insert(TransactionContext tx, SourceControlPullRequestComment entity) {
    validateOwnership(tx, entity);
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }
    return super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, SourceControlPullRequestComment entity) {
    validateOwnership(tx, entity);
    entity.setUpdateTime(new Date());
    super.update(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_PULL_REQUEST_COMMENT;
  }

  @Override
  public Class<SourceControlPullRequestComment> getEntityClass() {
    return SourceControlPullRequestComment.class;
  }
}
