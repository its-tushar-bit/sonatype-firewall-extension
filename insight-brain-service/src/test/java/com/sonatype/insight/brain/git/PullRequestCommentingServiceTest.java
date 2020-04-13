/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.CommentResponse;
import com.sonatype.nexus.scm.api.model.CommitInformation;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.api.model.PullRequestState;
import com.sonatype.nexus.scm.github.dto.GithubCommentResponse;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;
import com.sonatype.nexus.scm.github.graphql.GitHubGraphQlClient;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.git.PullRequestCommentingService.APPLICATION_PULL_REQUEST_FETCH_COUNT;
import static com.sonatype.insight.brain.git.PullRequestCommentingService.COMMIT_HISTORY_FETCH_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestCommentingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestCommentingMetricsService mockPullRequestCommentingMetricsService;

  public PullRequestCommentingServiceTest() {
    super(PullRequestCommentingService.class);
  }

  private TestProductLicense testProductLicense;

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.initMocks(this);
    super.setup();

    TestProductLicenseManager productLicenseManager = new TestProductLicenseManager();
    testProductLicense = new TestProductLicense(productLicenseManager);
  }

  @Test
  public void testApplicationEvaluationEvent_Unlicensed() throws IOException {
    // remove automation feature, leaving notifications
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);

    // given : commenting service object, scm enabled, and an event with a commit hash
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : a debug message is logged
    assertThatLogMessagesEqual(
        debug("License does not support SourceControl automation features"));

    // and : processing stops there
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
  }

  @Test
  public void testOnApplicationEvaluation_missingCommitHash() throws IOException {
    // given : commenting service object, scm enabled, and an event without a commit hash
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment was not created
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("no commit hash : skipping PR commenting for application 'app1' with policy evaluation 'pe1'"));
  }

  @Test
  public void testOnApplicationEvaluation_scmDisabled() throws IOException {
    // given : commenting service object, scm disabled, and an event with a commit hash
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withScmEnabled(false)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit123")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment was not created
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("scm disabled : skipping PR commenting for application 'app1' with policy evaluation 'pe1'"));
  }

  @Test
  public void testEventHasCommitHashAndScmIsEnabled_ok() throws IOException {
    // given : commenting service object, scm enabled, and an event with a commit hash
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : check event
    boolean isOk = commentingService.eventHasCommitHashAndScmIsEnabled(event);

    // then : event is ok for processing
    assertThat(isOk).isTrue();
  }

  @Test
  public void testOnApplicationEvaluation_pullRequestIsForBaseBranch() throws IOException {
    // given : a pull request for the base branch
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withSourcePolicyEvaluation("pe1", "commit456", "app1")
        .withBaseBranchPullRequest(4)
        .expectSourceCommit("commit456")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment not created due to PR being for base branch
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'commit456' with 1 pull request(s) and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        debug("application 'app1' pull request '4' is for the base branch, skipping commenting for this PR")
    );
  }

  @Test
  public void testOnApplicationEvaluation_commentAlreadyExists() throws IOException {
    // given : a pull request for a dev branch that already has our comment
    String commentText = "at least one new policy violation";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withSourcePolicyEvaluation("pe1", "commit456", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 7, "commit456", "baseCommit")
        .withPolicyEvaluationDiffMarkup(commentText)
        .expectApplicationId("app1")
        .withCommentForPullRequest(7, 27)
        .withCommentResponseForPR(7, 27)
        .expectSourceCommit("commit456")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment not created due to PR already having a comment from us
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'commit456' with 1 pull request(s) and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        info("pull request comment '27' updated for application 'app1' pull request '7'"),
        debug("comment text = at least one new policy violation"),
        debug("pull request comment '27' for application 'app1' pull request '7' recorded in database")
    );
  }

  @Test
  public void testOnApplicationEvaluation_missingBaseBranchPolicyEvaluation() throws IOException {
    // given : no base branch policy eval to compare against
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withSourcePolicyEvaluation("pe1", "commit456", "app1")
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 8, "commit456", "baseCommit")
        .expectApplicationId("app1")
        .expectSourceCommit("commit456")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment not created due to no base branch policy eval to compare to
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'commit456' with 1 pull request(s) and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        warn("no policy evaluation for base branch, skipping PR commenting for application 'app1' pull request '8'")
    );
  }

  @Test
  public void testOnApplicationEvaluation_policyEvalDiffNotMeaningful() throws IOException {
    // given : PR needing comment but nothing relevant in the policy eval diff
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 11, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup("")
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment not created due to no meaningful policy eval diff
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'sourceCommit' with 1 pull request(s) " +
            "and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        info("no added violations in policy eval diff, and no previous PR comments for application " +
            "'app1' pull request '11'.")
    );
  }

  @Test
  public void testOnApplicationEvaluation_shouldCreateComment() throws IOException {
    // given : all the necessary pieces to create a PR comment
    String commentText = "at least one new policy violation";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 14, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup(commentText)
        .withCommentResponseForPR(14, 27)
        .withAddedViolation(new PolicyViolation())
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comment should be created
    verify(mockPullRequestCommentingMetricsService, only()).onCommentCreated(eq("app1"), eq(14), eq(27));
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'sourceCommit' with 1 pull request(s) " +
            "and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        info("pull request comment '27' created for application 'app1' pull request '14'"),
        debug("comment text = " + commentText),
        debug("pull request comment '27' for application 'app1' pull request '14' recorded in database")
    );
  }

  @Test
  public void testOnApplicationEvaluation_GitLabUnsupported() throws IOException {
    // given : app source control provider = GitLab
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withProvider(SourceControlProvider.GITLAB)
        .withGitRepositoryEffectivelyPrivateThrows(UnsupportedOperationException.class)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit789")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : GitLab not supported yet
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(debug("GitLab not currently supported for pull request commenting"));
  }

  @Test
  public void testOnApplicationEvaluation_RepositoryNotPrivate() throws IOException {
    // given : all the necessary pieces to create a PR comment
    String commentText = "at least one new policy violation";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 14, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup(commentText)
        .withCommentResponseForPR(14, 25)
        .withGitRepositoryPrivate(false)
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(debug(
        "obtained CommitInfo from SCM for commit 'sourceCommit' with 1 pull request(s) and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        debug("Repository is not private: http://github.com/testOrg/testRepo"));
  }

  @Test
  public void testOnApplicationEvaluation_processMultiplePrsAndRecordCommentForMatchingHeadCommit() throws IOException {
    // given : multiple associated PR's, two with matching head commit, and two with other head commits
    String commentText = "at least one new policy violation";
    String applicationId = "app1";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("Associated_PR_With_Other_Head_Commit", 13, "otherCommit", "baseCommit")
        .withDevBranchPullRequest("First_PR_With_Head_Commit", 14, "sourceCommit", "baseCommit")
        .withDevBranchPullRequest("Another_PR_With_Other_Head_Commit", 15, "anotherCommit", "baseCommit")
        .withDevBranchPullRequest("Secondary_PR_With_Head_Commit", 16, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup(commentText)
        .withCommentResponseForPR(14, 28)
        .withCommentResponseForPR(16, 32)
        .withAddedViolation(new PolicyViolation())
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId(applicationId)
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comments should be created for those PR's with matching head commits
    verify(mockPullRequestCommentingMetricsService, times(1)).onCommentCreated(eq(applicationId), eq(14), eq(28));
    verify(mockPullRequestCommentingMetricsService, times(1)).onCommentCreated(eq(applicationId), eq(16), eq(32));
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'sourceCommit' with 4 pull request(s) " +
            "and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        debug(
            "The head commit hash 'otherCommit', for application 'app1', PR '13' does not match the commit on " +
                "the policy evaluation 'sourceCommit'"),
        info("pull request comment '28' created for application 'app1' pull request '14'"),
        debug("comment text = " + commentText),
        debug("pull request comment '28' for application 'app1' pull request '14' recorded in database"),
        debug(
            "The head commit hash 'anotherCommit', for application 'app1', PR '15' does not match the commit on " +
                "the policy evaluation 'sourceCommit'"),
        info("pull request comment '32' created for application 'app1' pull request '16'"),
        debug("comment text = " + commentText),
        debug("pull request comment '32' for application 'app1' pull request '16' recorded in database")
    );
  }

  @Test
  public void testOnApplicationEvaluation_processOnlyOpenPrs() throws IOException {
    // given : multiple associated PR's, two with matching head commit, and two with other head commits
    String commentText = "at least one new policy violation";
    String applicationId = "app1";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("Closed_PR", 13, "sourceCommit", "baseCommit", PullRequestState.CLOSED)
        .withDevBranchPullRequest("First_PR_With_Head_Commit", 14, "sourceCommit", "baseCommit")
        .withDevBranchPullRequest("Merged_PR", 15, "sourceCommit", "baseCommit", PullRequestState.MERGED)
        .withDevBranchPullRequest("Secondary_PR_With_Head_Commit", 16, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup(commentText)
        .withCommentResponseForPR(14, 42)
        .withCommentResponseForPR(16, 48)
        .withAddedViolation(new PolicyViolation())
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId(applicationId)
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : comments should be created for those PR's with matching head commits
    verify(mockPullRequestCommentingMetricsService, times(1)).onCommentCreated(eq(applicationId), eq(14), eq(42));
    verify(mockPullRequestCommentingMetricsService, times(1)).onCommentCreated(eq(applicationId), eq(16), eq(48));
    assertThatLogMessagesEqual(
        debug("obtained CommitInfo from SCM for commit 'sourceCommit' with 4 pull request(s) " +
            "and 0 base branch commit(s)"),
        debug("0 base branch commits to process for application 'app1'"),
        debug("application 'app1' pull request '13' state 'CLOSED' is not open, skipping commenting for this PR"),
        info("pull request comment '42' created for application 'app1' pull request '14'"),
        debug("comment text = " + commentText),
        debug("pull request comment '42' for application 'app1' pull request '14' recorded in database"),
        debug("application 'app1' pull request '15' state 'MERGED' is not open, skipping commenting for this PR"),
        info("pull request comment '48' created for application 'app1' pull request '16'"),
        debug("comment text = " + commentText),
        debug("pull request comment '48' for application 'app1' pull request '16' recorded in database")
    );
  }

  @Test
  public void testOnDiscoveredPullRequest_missingBaseBranchPolicyEval() throws IOException {
    // given : no base branch policy eval to compare against
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 20, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    DiscoveredPullRequestEvent event =
        createDiscoveredPullRequestEvent("app1", "sourcePe", "sourceCommit", 20, null);

    // when : process event
    commentingService.onDiscoveredPullRequest(event);

    // then : comment should be created
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        debug(
            "obtained CommitInfo from SCM for commit 'sourceCommit' with 1 pull request(s) and 0 base branch commit(s)"
        ),
        debug("0 base branch commits to process for application 'app1'"),
        warn("no policy evaluation for base branch, skipping PR commenting for application 'app1' pull request '20'")
    );
  }

  @Test
  public void testOnDiscoveredPullRequest_hasBaseBranchPolicyEvalCommentAlreadyExists()
      throws IOException
  {
    // given : all the necessary pieces to create a PR comment, except the comment already exists
    String commentText = "at least one new policy violation";
    String applicationId = "app1";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 20, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", applicationId)
        .withBasePolicyEvaluation("basePe", "baseCommit", applicationId)
        .withPolicyEvaluationDiffMarkup(commentText)
        .expectApplicationId(applicationId)
        .withCommentResponseForPR(20, 25)
        .withCommentForPullRequest(20, 25, "sourcePe-0", "basePe")
        .expectSourceCommit("sourceCommit")
        .build();

    DiscoveredPullRequestEvent event =
        createDiscoveredPullRequestEvent(applicationId, "sourcePe", "sourceCommit", 20, "basePe");

    // when : process event
    commentingService.onDiscoveredPullRequest(event);

    // then : comment should be created
    verify(mockPullRequestCommentingMetricsService, only()).onCommentUpdated(eq(applicationId), eq(20), eq(25));
    assertThatLogMessagesEqual(
        info("pull request comment '25' updated for application 'app1' pull request '20'"),
        debug("comment text = at least one new policy violation"),
        debug("pull request comment '25' for application 'app1' pull request '20' recorded in database")
    );
  }

  @Test
  public void testOnDiscoveredPullRequest_policyEvaluationIdsUnchanged()
      throws IOException
  {
    // given : all the necessary pieces to create a PR comment, except the comment already exists
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 20, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .expectApplicationId("app1")
        .withCommentForPullRequest(20, 10)
        .expectSourceCommit("sourceCommit")
        .build();

    DiscoveredPullRequestEvent event =
        createDiscoveredPullRequestEvent("app1", "sourcePe", "sourceCommit", 20, "basePe");

    // when : process event
    commentingService.onDiscoveredPullRequest(event);

    // then : comment should be created
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
    assertThatLogMessagesEqual(
        info("policy evaluations have not changed for 'app1' pull request '20'")
    );
  }

  @Test
  public void testOnDiscoveredPullRequest_hasBaseBranchPolicyEvalCommentDoesNotExist()
      throws IOException
  {
    // given : all the necessary pieces to create a PR comment
    String commentText = "at least one new policy violation";
    String applicationId = "app1";
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withDevBranchPullRequest("INT-2493-pr-commenting-immediate-flow", 20, "sourceCommit", "baseCommit")
        .withSourcePolicyEvaluation("sourcePe", "sourceCommit", "app1")
        .withBasePolicyEvaluation("basePe", "baseCommit", "app1")
        .withPolicyEvaluationDiffMarkup(commentText)
        .withCommentResponseForPR(20, 25)
        .withAddedViolation(new PolicyViolation())
        .expectApplicationId("app1")
        .expectSourceCommit("sourceCommit")
        .build();

    DiscoveredPullRequestEvent event =
        createDiscoveredPullRequestEvent(applicationId, "sourcePe", "sourceCommit", 20, "basePe");

    // when : process event
    commentingService.onDiscoveredPullRequest(event);

    // then : comment should be created
    verify(mockPullRequestCommentingMetricsService, only()).onCommentCreated(eq(applicationId), eq(20), eq(25));
    assertThatLogMessagesEqual(
        info("pull request comment '25' created for application 'app1' pull request '20'"),
        debug("comment text = " + commentText),
        debug("pull request comment '25' for application 'app1' pull request '20' recorded in database")
    );
  }

  @Test
  public void testOnApplicationEvaluation_featureFlagOff() throws IOException {
    // given : all the necessary pieces to create a PR comment
    PullRequestCommentingService commentingService = new TestablePullRequestCommentingServiceBuilder()
        .withFeatureFlagEnabled(false)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("sourcePe")
        .withCommitHash("sourceCommit")
        .build();

    // when : process event
    commentingService.onApplicationEvaluation(event);

    // then : no comment should be created
    verify(mockPullRequestCommentingMetricsService, never()).onCommentCreated(anyString(), anyInt(), anyInt());
  }

  private DiscoveredPullRequestEvent createDiscoveredPullRequestEvent(
      String applicationId,
      String sourcePolicyEvaluationId,
      String commitHash,
      int pullRequestNumber,
      String targetPolicyEvaluationId)
  {
    DiscoveredPullRequestEvent event = new DiscoveredPullRequestEvent();
    event.applicationId = applicationId;
    event.policyEvaluationId = sourcePolicyEvaluationId;
    event.commitHash = commitHash;
    event.targetPolicyEvaluationId = targetPolicyEvaluationId;
    event.pullRequestNumber = pullRequestNumber;
    return event;
  }

  private class TestablePullRequestCommentingServiceBuilder
  {
    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private GitClientFactory mockGitClientFactory;

    @Mock
    private GitApiClient mockGitApiClient;

    @Mock
    private GitHubGraphQlClient mockGitHubGraphQlClient;

    @Mock
    private SourceControlPullRequestCommentDAO mockPullRequestCommentDAO;

    @Mock
    private PolicyEvaluationDAO mockPolicyEvaluationDAO;

    @Mock
    private PullRequestFeedbackMarkupService mockPullRequestFeedbackMarkupService;

    @Mock
    private GitCommitHistoryService mockGitCommitHistoryService;

    @Mock
    private AsyncEventBus mockAsyncEventBus;

    @Mock
    private PullRequestUtils mockPullRequestUtils;

    @Mock
    private PolicyEvaluationDiffService mockPolicyEvaluationDiffService;

    private boolean scmEnabled = true;

    private String org = "testOrg";

    private String repo = "testRepo";

    private String token = "testToken";

    private SourceControlProvider provider = SourceControlProvider.GITHUB;

    private String baseBranch = "master";

    private String sourceCommitHash = "";

    private String applicationId = "";

    private boolean enablePullRequests = true;

    private boolean enableStatusChecks = true;

    private CommitInformation commitInformation = new CommitInformation();

    private SourceControlPullRequestComment pullRequestComment;

    private PolicyEvaluation sourcePolicyEvaluation = new PolicyEvaluation();

    private PolicyEvaluation basePolicyEvaluation = null;

    private Optional<String> policyEvaluationDiffMarkup;

    private GitRepositoryInfo gitRepositoryInfo;

    private boolean isGitRepositoryPrivate = true;

    private final Map<Integer, CommentResponse> pullRequestCommentResponseMap = new HashMap<>();

    private Class<? extends Exception> gitRepositoryEffectivelyPrivateThrows;

    private Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff =
        Optional.of(new PolicyViolationDiff<>());

    private boolean featureFlagEnabled = true;

    PullRequestCommentingService build() throws IOException {
      MockitoAnnotations.initMocks(this);

      doReturn(scmEnabled).when(mockSourceControlUtils).isScmEnabled(any(String.class));
      doReturn(scmEnabled).when(mockSourceControlUtils).isScmEnabled(any(GitRepositoryInfo.class));

      String repositoryUrl = String.format("http://%s.com/%s/%s", provider.toString(), org, repo);
      gitRepositoryInfo =
          new GitRepositoryInfo(repositoryUrl, token, provider, baseBranch, enablePullRequests, enableStatusChecks);
      doReturn(gitRepositoryInfo).when(mockSourceControlUtils).getGitRepositoryInfoForApplication(any());

      doReturn(mockGitApiClient).when(mockGitClientFactory).createApiClient(gitRepositoryInfo);

      for (Entry<Integer, CommentResponse> entry : pullRequestCommentResponseMap.entrySet()) {
        doReturn(entry.getValue()).when(mockGitApiClient).createPullRequestComment(eq(entry.getKey()), any());
        doReturn(entry.getValue()).when(mockGitApiClient).updatePullRequestComment(eq(entry.getValue().getId()), any());
      }

      doReturn(mockGitHubGraphQlClient).when(mockGitClientFactory).createGraphqlApiClient(gitRepositoryInfo);

      doReturn(commitInformation).when(mockGitHubGraphQlClient).getCommitInformationForCommit(
          eq(org), eq(repo), eq(sourceCommitHash), eq(baseBranch), eq(COMMIT_HISTORY_FETCH_COUNT),
          eq(APPLICATION_PULL_REQUEST_FETCH_COUNT));

      if (null != pullRequestComment) {
        doReturn(pullRequestComment).when(mockPullRequestCommentDAO)
            .getByApplicationIdAndPullRequestIdWithoutComponent(eq(applicationId),
                eq(pullRequestComment.getPullRequestId()));
      }

      doReturn(null != basePolicyEvaluation ? Optional.of(basePolicyEvaluation) : Optional.empty())
          .when(mockGitCommitHistoryService).getLatestPolicyEvaluationForApplicationBaseBranch(eq(applicationId));

      if (null != basePolicyEvaluation) {
        doReturn(basePolicyEvaluation).when(mockPolicyEvaluationDAO).getById(basePolicyEvaluation.getId());
      }

      doReturn(sourcePolicyEvaluation).when(mockPolicyEvaluationDAO).getById(eq(sourcePolicyEvaluation.getId()));

      doReturn(policyViolationDiff).when(mockPolicyEvaluationDiffService)
          .createPolicyViolationDiff(basePolicyEvaluation, sourcePolicyEvaluation);

      doReturn(policyEvaluationDiffMarkup).when(mockPullRequestFeedbackMarkupService)
          .createMarkup(any(), any(), any());

      if (gitRepositoryEffectivelyPrivateThrows != null) {
        doThrow(UnsupportedOperationException.class).when(mockPullRequestUtils)
            .isEffectivelyPrivate(eq(gitRepositoryInfo), eq(isGitRepositoryPrivate));
      }
      else {
        doReturn(isGitRepositoryPrivate).when(mockPullRequestUtils)
            .isEffectivelyPrivate(any(GitRepositoryInfo.class), anyBoolean());
        commitInformation.setRepositoryPrivate(isGitRepositoryPrivate);
      }

      return new PullRequestCommentingService(
          mockSourceControlUtils,
          mockGitClientFactory,
          mockPullRequestCommentDAO,
          mockPolicyEvaluationDAO,
          mockPullRequestFeedbackMarkupService,
          mockGitCommitHistoryService,
          mockPullRequestCommentingMetricsService,
          mockAsyncEventBus,
          testProductLicense,
          mockPullRequestUtils,
          mockPolicyEvaluationDiffService,
          getInsightConfig(featureFlagEnabled)
      );
    }

    TestablePullRequestCommentingServiceBuilder withFeatureFlagEnabled(boolean featureFlagEnabled) {
      this.featureFlagEnabled = featureFlagEnabled;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withScmEnabled(boolean scmEnabled) {
      this.scmEnabled = scmEnabled;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder expectSourceCommit(String sourceCommitHash) {
      this.sourceCommitHash = sourceCommitHash;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder expectApplicationId(String applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withDevBranchPullRequest(
        String branchName,
        int pullRequestNumber,
        String headCommitHash,
        String baseCommitHash)
    {
      return withDevBranchPullRequest(branchName, pullRequestNumber, headCommitHash, baseCommitHash,
          PullRequestState.OPEN);
    }

    TestablePullRequestCommentingServiceBuilder withDevBranchPullRequest(
        String branchName,
        int pullRequestNumber,
        String headCommitHash,
        String baseCommitHash,
        PullRequestState pullRequestState)
    {
      PullRequest pullRequest = new GithubPullRequest();
      pullRequest.setHead(branchName);
      pullRequest.setHeadCommitHash(headCommitHash);
      pullRequest.setNumber(pullRequestNumber);
      pullRequest.setBase(baseBranch);
      pullRequest.setBaseCommitHash(baseCommitHash);
      commitInformation.addPullRequest(pullRequest);
      sourceCommitHash = headCommitHash;
      pullRequest.setState(pullRequestState);
      pullRequest.setRepositoryPrivate(isGitRepositoryPrivate);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withBaseBranchPullRequest(int pullRequestNumber) {
      PullRequest pullRequest = new GithubPullRequest();
      pullRequest.setHead(baseBranch);
      pullRequest.setNumber(pullRequestNumber);
      commitInformation.addPullRequest(pullRequest);
      pullRequest.setState(PullRequestState.OPEN);
      pullRequest.setRepositoryPrivate(isGitRepositoryPrivate);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withCommentForPullRequest(int pullRequestNumber, int commentId) {
      return withCommentForPullRequest(pullRequestNumber, commentId, "sourcePe", "basePe");
    }

    TestablePullRequestCommentingServiceBuilder withCommentForPullRequest(
        int pullRequestNumber,
        int commentId,
        String sourcePolicyEvaluationId,
        String targetPolicyEvaluationId)
    {
      pullRequestComment = new SourceControlPullRequestComment();
      pullRequestComment.setApplicationId(applicationId);
      pullRequestComment.setPullRequestId(pullRequestNumber);
      pullRequestComment.setPullRequestCommentId(commentId);
      pullRequestComment.setSourcePolicyEvaluationId(sourcePolicyEvaluationId);
      pullRequestComment.setTargetPolicyEvaluationId(targetPolicyEvaluationId);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withSourcePolicyEvaluation(
        String policyEvaluationId,
        String commitHash,
        String applicationId)
    {
      sourcePolicyEvaluation.setId(policyEvaluationId);
      sourcePolicyEvaluation.setCommitHash(commitHash);
      sourcePolicyEvaluation.setApplicationId(applicationId);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withBasePolicyEvaluation(
        String policyEvaluationId,
        String commitHash,
        String applicationId)
    {
      basePolicyEvaluation = new PolicyEvaluation();
      basePolicyEvaluation.setId(policyEvaluationId);
      basePolicyEvaluation.setCommitHash(commitHash);
      basePolicyEvaluation.setApplicationId(applicationId);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withPolicyEvaluationDiffMarkup(String markup) {
      policyEvaluationDiffMarkup = StringUtils.isBlank(markup) ? Optional.empty() : Optional.of(markup);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withCommentResponseForPR(int pullRequestId, int commentId) {
      CommentResponse commentResponse = new GithubCommentResponse();
      commentResponse.setId(commentId);
      pullRequestCommentResponseMap.put(pullRequestId, commentResponse);
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withProvider(SourceControlProvider provider) {
      this.provider = provider;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withGitRepositoryPrivate(boolean isGitRepositoryPrivate) {
      this.isGitRepositoryPrivate = isGitRepositoryPrivate;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withGitRepositoryEffectivelyPrivateThrows(
        Class<? extends Exception> gitRepositoryEffectivelyPrivateThrows)
    {
      this.gitRepositoryEffectivelyPrivateThrows = gitRepositoryEffectivelyPrivateThrows;
      return this;
    }

    TestablePullRequestCommentingServiceBuilder withAddedViolation(PolicyViolation policyViolation) {
      policyViolationDiff.get().addAppeared(policyViolation);
      return this;
    }

    private InsightConfig getInsightConfig(boolean enableFeatureFlag) {
      InsightConfig config = new InsightConfig();
      Map<String, Boolean> features = new HashMap<>();
      features.put(Feature.PR_COMMENTING.getFlag(), enableFeatureFlag);
      config.setFeatures(features);
      return config;
    }
  }

  private class ApplicationEvaluationEventBuilder
  {
    private ApplicationEvaluationEvent applicationEvaluationEvent;

    private ApplicationEvaluationEventBuilder() {
      applicationEvaluationEvent = new ApplicationEvaluationEvent();
    }

    ApplicationEvaluationEventBuilder withApplicationId(String applicationId) {
      applicationEvaluationEvent.ownerId = applicationId;
      return this;
    }

    ApplicationEvaluationEventBuilder withPolicyEvaluationId(String policyEvaluationId) {
      applicationEvaluationEvent.policyEvaluationId = policyEvaluationId;
      return this;
    }

    ApplicationEvaluationEventBuilder withCommitHash(String commitHash) {
      applicationEvaluationEvent.commitHash = commitHash;
      return this;
    }

    ApplicationEvaluationEvent build() {
      return applicationEvaluationEvent;
    }
  }
}
