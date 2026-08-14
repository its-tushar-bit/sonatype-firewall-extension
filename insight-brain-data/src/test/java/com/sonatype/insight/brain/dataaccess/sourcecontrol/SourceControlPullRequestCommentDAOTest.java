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
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlPullRequestCommentDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    pullRequestCommentDAO = daoFactory.createSourceControlPullRequestCommentDAO();
  }

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
        targetPolicyEvaluation.getId());

    // fetch will create a new pull request comment instance; since the create time is set internally we want to make
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
    pullRequestCommentDAO.delete(fetchedPullRequestComment);

    // then : the comment no longer exists
    fetchedPullRequestComment = pullRequestCommentDAO.getById(fetchedPullRequestComment.getId());
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
          "testpathname" + commentId,
          subjectPullRequestId,
          commentId,
          pullRequestCommentVersion,
          sourcePolicyEvaluation.getId(),
          targetPolicyEvaluation.getId());
    }

    // and given: an overall comment for the same app and PR
    tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        subjectPullRequestId,
        0,
        pullRequestCommentVersion,
        "contentHash",
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());

    // and given: line comments for a different app and PR
    final int additionalPullRequestId = 2;
    Application app2 = tempEntity.newApplication(organization.getId());
    PolicyEvaluation sourcePolicyEvaluation2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    for (int commentId = 1; commentId <= lineCommentCount; commentId++) {
      tempEntity.newSourceControlPullRequestCommentForLine(
          app2.getId(),
          "hash" + commentId,
          "testpathname" + commentId,
          additionalPullRequestId,
          commentId,
          pullRequestCommentVersion,
          sourcePolicyEvaluation2.getId(), targetPolicyEvaluation2.getId());
    }

    // when: fetch ALL the line comments for the subject PR
    List<SourceControlPullRequestComment> subjectComments = pullRequestCommentDAO
        .getByApplicationIdAndPullRequestIdWithComponents(application.getId(), subjectPullRequestId);

    // then: all are retrieved and there are none we don't expect
    assertThat(subjectComments.size()).isEqualTo(lineCommentCount);
    Set<Long> commentIdSet = new HashSet<>();
    subjectComments.forEach(comment -> {
      assertThat(comment.getApplicationId()).isEqualTo(application.getId());
      assertThat(comment.getPullRequestId()).isEqualTo(subjectPullRequestId);
      assertThat(comment.getComponentHash()).isEqualTo("hash" + comment.getPullRequestCommentId());
      assertThat(comment.getPathname()).isEqualTo("testpathname" + comment.getPullRequestCommentId());
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
    assertThat(lineComment.getPathname()).isEqualTo("testpathname3");

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

    // when : trying to insert the pr comment entry an exception is thrown indicating that the given source commit
    // doesn't reference a policy evaluation.
    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 2, 3, "contentHash", "bogusPolicyEvalId",
          targetPolicyEvaluation.getId());
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("PolicyEvaluation with ID bogusPolicyEvalId does not exist.");
  }

  @Test
  public void testInsert_policyEvalDoesNotExistForTargetCommit() {
    // given : a pr comment entry that has a target commit that doesn't reference a policy evaluation
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    // when : trying to insert the pr comment entry an exception is thrown indicating that the given target commit
    // doesn't reference a policy evaluation.
    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 2, 3, "contentHash",
          sourcePolicyEvaluation.getId(), "bogusPolicyEvalId");
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("PolicyEvaluation with ID bogusPolicyEvalId does not exist.");
  }

  @Test
  public void testInsert_applicationDoesNotExist() {
    // given : a pr comment entry that doesn't reference a valid application
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    // when : trying to insert the pr comment entry an exception is thrown indicating that the given application is
    // invalid
    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequestComment("bogusAppId", 1, 2, 3, "contentHash",
          sourcePolicyEvaluation.getId(), targetPolicyEvaluation.getId());
    }).isInstanceOf(DataAccessException.class)
        .hasMessage("The source policy evaluation app ID does not match the pull request comment app ID.");
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
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit")
        .getId();
    String targetPolicyEvalId = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit")
        .getId();
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
    String sourcePolicyEvalId2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "sourceScan", "sourceCommit").getId();
    String targetPolicyEvalId2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "targetScan", "targetCommit").getId();
    tempEntity.newSourceControlPullRequestComment(app2.getId(), 3, 33, 333, "contentHash2",
        sourcePolicyEvalId2, targetPolicyEvalId2);

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

  @Test
  public void testDeleteByApplicationId() {
    // given : add some pull request data
    String sourcePolicyEvalId = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit")
        .getId();
    String targetPolicyEvalId = tempEntity
        .newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit")
        .getId();
    tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 11, 111, "contentHash1",
        sourcePolicyEvalId, targetPolicyEvalId);
    tempEntity.newSourceControlPullRequestComment(application.getId(), 2, 22, 222, "contentHash2",
        sourcePolicyEvalId, targetPolicyEvalId);

    // given : add a 2nd app
    Application app2 = tempEntity.newApplication("app2", "app2", organization.getId());
    String sourcePolicyEvalId2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "sourceScan", "sourceCommit").getId();
    String targetPolicyEvalId2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "targetScan", "targetCommit").getId();

    // given : add data for 2nd app
    tempEntity.newSourceControlPullRequestComment(app2.getId(), 3, 33, 333, "contentHash2",
        sourcePolicyEvalId2, targetPolicyEvalId2);

    // when : delete all for first app
    try (TransactionContext tx = pullRequestCommentDAO.createTransactionContext()) {
      tx.begin();
      pullRequestCommentDAO.deleteByApplicationId(tx, application.getId());
      tx.commit();
    }

    // then : fetch for original app expects no results
    List<SourceControlPullRequestComment> pullRequestComments =
        pullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(pullRequestComments).isNotNull().isEmpty();

    // then : fetch for 2nd app then expect 1 result
    pullRequestComments = pullRequestCommentDAO.getByApplicationId(app2.getId());
    assertThat(pullRequestComments).isNotNull().hasSize(1);
    pullRequestComments.forEach(comment -> assertThat(comment.getApplicationId()).isEqualTo(app2.getId()));
  }

  @Test
  public void testInsert_ValidatesOwnership() {
    PolicyEvaluation policyEvaluationSameApp =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    Application otherApp = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluationOtherApp =
        tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 2, 3, "contentHash", //
          policyEvaluationOtherApp.getId(), //
          policyEvaluationSameApp.getId());
    }).isInstanceOf(DataAccessException.class)
        .hasMessage("The source policy evaluation app ID does not match the pull request comment app ID.");

    assertThatThrownBy(() -> {
      tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 2, 3, "contentHash", //
          policyEvaluationSameApp.getId(), //
          policyEvaluationOtherApp.getId());
    }).isInstanceOf(DataAccessException.class)
        .hasMessage("The target policy evaluation app ID does not match the pull request comment app ID.");
  }

  @Test
  public void testUpdate_ValidatesOwnership() {
    PolicyEvaluation policyEvaluationSameApp1 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    PolicyEvaluation policyEvaluationSameApp2 =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");
    SourceControlPullRequestComment pullRequestComment =
        tempEntity.newSourceControlPullRequestComment(application.getId(), 1, 2, 3, "contentHash", //
            policyEvaluationSameApp1.getId(), //
            policyEvaluationSameApp2.getId());

    Application otherApp = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluationOtherApp =
        tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    pullRequestComment.setSourcePolicyEvaluationId(policyEvaluationOtherApp.getId());
    assertThatThrownBy(() -> {
      pullRequestCommentDAO.update(pullRequestComment);
    }).isInstanceOf(DataAccessException.class)
        .hasMessage("The source policy evaluation app ID does not match the pull request comment app ID.");

    pullRequestComment.setSourcePolicyEvaluationId(policyEvaluationSameApp1.getId());
    pullRequestComment.setTargetPolicyEvaluationId(policyEvaluationOtherApp.getId());
    assertThatThrownBy(() -> {
      pullRequestCommentDAO.update(pullRequestComment);
    }).isInstanceOf(DataAccessException.class)
        .hasMessage("The target policy evaluation app ID does not match the pull request comment app ID.");
  }
}
