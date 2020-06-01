/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    assertThat(pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(), 1))
        .isNull();
  }

  @Test
  public void testCrud() throws InterruptedException {
    // given : a valid comment entry persisted with references to policy evaluations
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    int pullRequestId = 202001;
    int pullRequestCommentId = 1;
    int pullRequestCommentVersion = 2;
    String contentHash = "contentHash";

    SourceControlPullRequestComment pullRequestComment = tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        pullRequestId,
        pullRequestCommentId,
        pullRequestCommentVersion,
        contentHash,
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    );

    // fetch will create a new pull request comment instance;  since the create time is set internally we want to make
    // sure the fetch time is different from the insert time so we can verify that the create time is indeed coming
    // from the DB and not the comment instance creation
    Thread.sleep(10);

    // when : fetch the pr comment entry
    SourceControlPullRequestComment fetchedPullRequestComment =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(), pullRequestId);

    // then : fetched comment matches what we supplied
    assertThat(fetchedPullRequestComment).isNotNull();
    assertThat(fetchedPullRequestComment.getApplicationId()).isEqualTo(application.getId());
    assertThat(fetchedPullRequestComment.getPullRequestId()).isEqualTo(pullRequestId);
    assertThat(fetchedPullRequestComment.getPullRequestCommentId()).isEqualTo(pullRequestCommentId);
    assertThat(fetchedPullRequestComment.getPullRequestCommentVersion()).isEqualTo(pullRequestCommentVersion);
    assertThat(fetchedPullRequestComment.getContentHash()).isEqualTo(contentHash);
    assertThat(fetchedPullRequestComment.getSourcePolicyEvaluationId()).isEqualTo(sourcePolicyEvaluation.getId());
    assertThat(fetchedPullRequestComment.getTargetPolicyEvaluationId()).isEqualTo(targetPolicyEvaluation.getId());
    assertThat(fetchedPullRequestComment.getCreateTime()).isEqualTo(pullRequestComment.getCreateTime());
    assertThat(fetchedPullRequestComment.getUpdateTime()).isNull();

    // when: we update the comment
    pullRequestCommentDAO.update(fetchedPullRequestComment);

    // when : fetch the pr comment entry again
    SourceControlPullRequestComment fetchedPullRequestCommentAgain =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(), pullRequestId);

    // then : the update time is set
    assertThat(fetchedPullRequestCommentAgain.getUpdateTime()).isNotNull();

    // when : delete the comment
    pullRequestCommentDAO.deleteByPolicyEvaluationId(fetchedPullRequestComment.getSourcePolicyEvaluationId());

    // then : the comment no longer exists
    fetchedPullRequestComment =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(), pullRequestId);
    assertThat(fetchedPullRequestComment).isNull();
  }

  @Test
  public void testCrud_forLineComments() {
    // given: valid line comment entries persisted with references to policy evaluations
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    final int subjectPullRequestId = 1;
    final int lineCommentCount = 10;
    final int pullRequestCommentVersion = 2;

    for (int commentId = 1; commentId <= lineCommentCount; commentId++) {
      tempEntity.newSourceControlPullRequestCommentForLine(
          application.getId(),
          "hash" + commentId,
          subjectPullRequestId,
          commentId,
          pullRequestCommentVersion,
          sourcePolicyEvaluation.getId(),
          targetPolicyEvaluation.getId()
      );
    }

    // and given: an overall comment for the same app and PR
    tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        subjectPullRequestId,
        0,
        pullRequestCommentVersion,
        "contentHash",
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    );

    // and given: line comments for a different app and PR
    final int additionalPullRequestId = 2;
    Application app2 = tempEntity.newApplication(organization.getId());

    for (int commentId = 1; commentId <= lineCommentCount; commentId++) {
      tempEntity.newSourceControlPullRequestCommentForLine(
          app2.getId(),
          "hash" + commentId,
          additionalPullRequestId,
          commentId,
          pullRequestCommentVersion,
          sourcePolicyEvaluation.getId(),
          targetPolicyEvaluation.getId()
      );
    }

    // when: fetch ALL the line comments for the subject PR
    List<SourceControlPullRequestComment> subjectComments = pullRequestCommentDAO
        .getByApplicationIdAndPullRequestIdWithComponents(application.getId(), subjectPullRequestId);

    // then: all are retrieved and there are none we don't expect
    assertThat(subjectComments.size()).isEqualTo(lineCommentCount);
    Set<Integer> commentIdSet = new HashSet<>();
    subjectComments.forEach(comment -> {
      assertThat(comment.getApplicationId()).isEqualTo(application.getId());
      assertThat(comment.getPullRequestId()).isEqualTo(subjectPullRequestId);
      assertThat(comment.getComponentHash()).isEqualTo("hash" + comment.getPullRequestCommentId());
      assertThat(comment.getPullRequestCommentVersion()).isEqualTo(pullRequestCommentVersion);
      assertThat(commentIdSet).doesNotContain(comment.getPullRequestCommentId());
      commentIdSet.add(comment.getPullRequestCommentId());
    });

    // when: fetch a specific line comment for the subject PR
    SourceControlPullRequestComment lineComment = pullRequestCommentDAO.getByApplicationIdAndComponentAndPullRequestId(
        application.getId(), "hash3", subjectPullRequestId);

    // then: only the specified line comment is returned
    assertThat(lineComment).isNotNull();
    assertThat(lineComment.getApplicationId()).isEqualTo(application.getId());
    assertThat(lineComment.getPullRequestId()).isEqualTo(subjectPullRequestId);
    assertThat(lineComment.getComponentHash()).isEqualTo("hash3");

    // when: delete the line comments for the subject PR
    pullRequestCommentDAO.deleteByApplicationIdAndPullRequestIdWithComponents(application.getId(),
        subjectPullRequestId);

    // then: there are no line comments for the subject PR
    subjectComments = pullRequestCommentDAO
        .getByApplicationIdAndPullRequestIdWithComponents(application.getId(), subjectPullRequestId);
    assertThat(subjectComments).isEmpty();

    // and when: fetch the line comments for the second PR
    List<SourceControlPullRequestComment> additionalComments =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithComponents(app2.getId(), additionalPullRequestId);

    // and then: the line comments for the second PR exist
    assertThat(additionalComments.size()).isEqualTo(lineCommentCount);

    // and when: fetch the overall comment for the subject PR
    SourceControlPullRequestComment overallComment =
        pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(),
            subjectPullRequestId);

    // and then: the overall comment for the subject PR exists
    assertThat(overallComment).isNotNull();
    assertThat(overallComment.getComponentHash()).isNull();
  }

  @Test
  public void testInsert_policyEvalDoesNotExistForSourceCommit() {
    // given : a pr comment entry that has a source commit that doesn't reference a policy evaluation
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        1,
        2,
        3,
        "contentHash",
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
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        1,
        2,
        3,
        "contentHash",
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
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    // when : trying to insert the pr comment entry
    Throwable thrown = catchThrowable(() -> tempEntity.newSourceControlPullRequestComment(
        "bogusAppId",
        1,
        2,
        3,
        "contentHash",
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
    List<SourceControlPullRequestComment> pullRequestComments =
        pullRequestCommentDAO.getByApplicationId(application.getId());

    // then : no results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments).isEmpty();

    // given : add some pull request data
    String sourcePolicyEvalId = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit").getId();
    String targetPolicyEvalId = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit").getId();
    tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 11, 111, "contentHash1",
        sourcePolicyEvalId, targetPolicyEvalId);
    tempEntity.newSourceControlPullRequestComment(application.getId(), 2, 22, 222, "contentHash2",
        sourcePolicyEvalId, targetPolicyEvalId);

    // when : fetch from populated table
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(application.getId());

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
    tempEntity.newSourceControlPullRequestComment(app2.getId(), 3, 33, 333, "contentHash2",
        sourcePolicyEvalId, targetPolicyEvalId);

    // when : fetch for original app
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(application.getId());

    // then : expect 2 results
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments.size()).isEqualTo(2);
    pullRequestComments.forEach(comment -> assertThat(comment.getApplicationId()).isEqualTo(application.getId()));

    // when : fetch for 2nd app
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(app2.getId());

    // then : expect 1 result
    assertThat(pullRequestComments).isNotNull();
    assertThat(pullRequestComments.size()).isEqualTo(1);
    pullRequestComments.forEach(comment -> assertThat(comment.getApplicationId()).isEqualTo(app2.getId()));
  }
}
