/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

public class SourceControlPullRequestCommentDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestCommentDAO pullRequestCommentDAO = new SourceControlPullRequestCommentDAO();

  @Test
  public void testGetCommentForPullRequest_commentDoesNotExist() {
    assertThat(pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, 1)).isNull();
  }

  @Test
  public void testCrud() throws InterruptedException {
    // given : a valid comment entry persisted with references to policy evaluations
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        applicationId, BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        applicationId, BuildStageType.ID, "targetScan", "targetCommit");

    int pullRequestId = 202001;
    int pullRequestCommentId = 1;

    SourceControlPullRequestComment pullRequestComment = tempEntity.newSourceControlPullRequestComment(
        applicationId,
        pullRequestId,
        pullRequestCommentId,
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    );

    // fetch will create a new pull request comment instance;  since the create time is set internally we want to make
    // sure the fetch time is different from the insert time so we can verify that the create time is indeed coming
    // from the DB and not the comment instance creation
    Thread.sleep(10);

    // when : fetch the pr comment entry
    SourceControlPullRequestComment fetchedPullRequestComment =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, pullRequestId);

    // then : fetched comment matches what we supplied
    assertThat(fetchedPullRequestComment).isNotNull();
    assertThat(fetchedPullRequestComment.getApplicationId()).isEqualTo(applicationId);
    assertThat(fetchedPullRequestComment.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(fetchedPullRequestComment.getPullRequestCommentId()).isEqualTo(pullRequestCommentId);
    assertThat(fetchedPullRequestComment.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluation.getId());
    assertThat(fetchedPullRequestComment.getTargetPolicyEvaluationId()).isEqualTo(targetPolicyEvaluation.getId());
    assertThat(fetchedPullRequestComment.getCreateTime()).isEqualTo(pullRequestComment.getCreateTime());
    assertThat(fetchedPullRequestComment.getUpdateTime()).isNull();

    // when: we update the comment
    pullRequestCommentDAO.update(fetchedPullRequestComment);

    // when : fetch the pr comment entry again
    SourceControlPullRequestComment fetchedPullRequestCommentAgain =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, pullRequestId);

    // then : the update time is set
    assertThat(fetchedPullRequestCommentAgain.getUpdateTime()).isNotNull();

    // when : delete the comment
    pullRequestCommentDAO.deleteByPolicyEvaluationId(fetchedPullRequestComment.getSourcePolicyEvaluationId());

    // then : the comment no longer exists
    fetchedPullRequestComment = pullRequestCommentDAO.getByApplicationIdAndPullRequestId(applicationId, pullRequestId);
    assertThat(fetchedPullRequestComment).isNull();
  }

  @Test
  public void testInsert_policyEvalDoesNotExistForSourceCommit() {
    // given : a pr comment entry that has a source commit that doesn't reference a policy evaluation
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "targetScan", "targetCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        applicationId,
        1,
        2,
        "bogusPolicyEvalId",
        targetPolicyEvaluation.getId()
    ));

    // then : exception thrown indicating that the given source commit doesn't reference a policy eval
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_pull_request_source_policy_eval_fk: \"");
  }

  @Test
  public void testInsert_policyEvalDoesNotExistForTargetCommit() {
    // given : a pr comment entry that has a target commit that doesn't reference a policy evaluation
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "sourceScan", "sourceCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        applicationId,
        1,
        2,
        sourcePolicyEvaluation.getId(),
        "bogusPolicyEvalId"
    ));

    // then : exception thrown indicating that the given target commit doesn't reference a policy eval
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_pull_request_target_policy_eval_fk: \"");
  }

  @Test
  public void testInsert_applicationDoesNotExist() {
    // given : a pr comment entry that doesn't reference a valid application
    PolicyEvaluation sourcePolicyEvaluation  =
        tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "targetScan", "targetCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        "bogusAppId",
        1,
        2,
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    ));

    // then : an exception is thrown indicating that the given application is invalid
    assertThat(thrown).hasStackTraceContaining(
        "Referential integrity constraint violation: \"source_control_pull_request_comment_app_fk: \"");
  }

  @Test
  public void testGetByApplicationId() {
    // when : fetch from empty table
    List<SourceControlPullRequestComment> pullRequestComments = pullRequestCommentDAO.getByApplicationId(applicationId);

    // then : no results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments).isEmpty();

    // given : add some pull request data
    String sourcePolicyEvalId = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "sourceScan", "sourceCommit").getId();
    String targetPolicyEvalId = tempEntity
        .newPolicyEvaluation(applicationId, BuildStageType.ID, "targetScan", "targetCommit").getId();
    tempEntity.newSourceControlPullRequestComment(applicationId, 1, 11, sourcePolicyEvalId, targetPolicyEvalId);
    tempEntity.newSourceControlPullRequestComment(applicationId, 2, 22, sourcePolicyEvalId, targetPolicyEvalId);

    // when : fetch from populated table
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(applicationId);

    // then : expect 2 results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments.size()).isEqualTo(2);

    // given : add a 2nd app
    Application app2 = tempEntity.newApplication("app2", "app2", organization.getId());

    // when : fetch for 2nd app
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(app2.getId());

    // then : no results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments).isEmpty();

    // given : add data for 2nd app
    tempEntity.newSourceControlPullRequestComment(app2.getId(), 3, 33, sourcePolicyEvalId, targetPolicyEvalId);

    // when : fetch for original app
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(applicationId);

    // then : expect 2 results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments.size()).isEqualTo(2);
    pullRequestComments.forEach(comment -> assertThat(comment.getApplicationId()).isEqualTo(applicationId));

    // when : fetch for 2nd app
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(app2.getId());

    // then : expect 1 result
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments.size()).isEqualTo(1);
    pullRequestComments.forEach(comment -> assertThat(comment.getApplicationId()).isEqualTo(app2.getId()));
  }
}
