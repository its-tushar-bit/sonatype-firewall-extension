/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.CLOSE_PULL_REQUEST_EVENT;
import static com.sonatype.nexus.scm.SourceControlProvider.AZURE;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.PullRequestLifecycleInfo;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CheckRun;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CheckRuns;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CheckSuiteNode;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CheckSuites;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.Commit;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CommitNode;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.PullRequestCommits;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.PullRequestsByIdData.PullRequest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.nexus.scm.gitlab.dto.GitlabMergeRequestResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PullRequestStateEventHandlerTest
    extends AbstractComponentTest
{
  private static final String TEST_ORG = "test-scm-org";

  private static final String TEST_REPO = "test-repo";

  // of the two providers mocked here, only Bitbucket allows/requires a username
  private static final String BITBUCKET_USERNAME = "user";

  private static final String TOKEN = "token";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0); // 0 = random port

  @Inject
  private PullRequestStateEventHandler handler;

  @Inject
  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private GitClientFactory gitClientFactory;

  @Inject
  private com.sonatype.insight.brain.sourcecontrol.SourceControlUtils sourceControlUtils;

  @Inject
  private PullRequestPollingService pullRequestPollingService;

  // Mocks for telemetry testing
  @Mock
  private TelemetrySender mockTelemetrySender;

  @Mock
  private TelemetryUtils mockTelemetryUtils;

  // Handler with mocked telemetry dependencies for specific tests
  private PullRequestStateEventHandler handlerWithMockedTelemetry;

  private Application applicationWithBatchSupport;

  private Application applicationWithoutBatchSupport;

  private String gitlabRepoUrl;

  private String bitbucketRepoUrl;

  @Before
  public void setup() throws Exception {
    // Initialize mocks
    MockitoAnnotations.openMocks(this);

    gitlabRepoUrl = "http://localhost:%d/%s/%s.git".formatted(wireMockRule.port(), TEST_ORG, TEST_REPO);
    bitbucketRepoUrl = "http://localhost:%d/scm/%s/%s.git".formatted(wireMockRule.port(), TEST_ORG, TEST_REPO);
    String encyptedToken = passwordHandler.encryptPassword(TOKEN);

    // Create two applications with different SCM providers
    applicationWithBatchSupport = tempEntity.newApplicationWithParent();
    applicationWithoutBatchSupport = tempEntity.newApplicationWithParent();

    // Create source control entries for each application
    // GitLab supports batch fetch
    tempEntity.newSourceControl(
        applicationWithBatchSupport.getId(),
        gitlabRepoUrl,
        encyptedToken,
        SourceControlProvider.GITLAB);

    // For this test, we'll use BITBUCKET as a provider that doesn't support batch fetch
    tempEntity.newSourceControl(
        applicationWithoutBatchSupport.getId(),
        bitbucketRepoUrl,
        BITBUCKET_USERNAME,
        encyptedToken,
        SourceControlProvider.BITBUCKET);

    setupGitlabBoilerplateEndpoints();

    // Create handler with mocked telemetry dependencies for specific tests
    handlerWithMockedTelemetry = new PullRequestStateEventHandler(
        gitClientFactory,
        sourceControlUtils,
        sourceControlDAO,
        sourceControlEventDAO,
        sourceControlPullRequestDAO,
        pullRequestPollingService,
        mockTelemetrySender,
        mockTelemetryUtils);
  }

  /**
   * During gitlab client initialization, it calls these endpoints.
   */
  private void setupGitlabBoilerplateEndpoints() throws IOException {
    stubFor(get(urlEqualTo("/api/v4/version"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(readTestResourceFile("gitlab-version.json"))));

    stubFor(get(urlEqualTo("/api/v4/user"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(readTestResourceFile("gitlab-user.json"))));
  }

  private void setupGitlabMergeRequestsEndpoint(int[] prNumbers, String response, int statusCode) {
    String queryParams = Arrays.stream(prNumbers)
        .mapToObj(num -> "iids[]=" + num)
        .collect(Collectors.joining("&"));
    String gitlabApiPath = "/api/v4/projects/%s%%2F%s/merge_requests?%s".formatted(TEST_ORG, TEST_REPO, queryParams);

    stubFor(get(urlEqualTo(gitlabApiPath))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(response)));
  }

  private void setupGitlabMergeRequestsEndpoint(int[] prNumbers, String responseResource) throws IOException {
    String responseJson = readTestResourceFile(responseResource);
    setupGitlabMergeRequestsEndpoint(prNumbers, responseJson, 200);
  }

  private void setupBitbucketPullRequestEndpoint(int prNumber, String response, int statusCode) {
    String bitbucketApiPath = "/rest/api/1.0/projects/%s/repos/%s/pull-requests/%d"
        .formatted(TEST_ORG, TEST_REPO, prNumber);

    stubFor(get(urlEqualTo(bitbucketApiPath))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(response)));
  }

  private void setupBitbucketPullRequestEndpoint(int prNumber, String responseResource) throws IOException {
    String responseJson = readTestResourceFile(responseResource);
    setupBitbucketPullRequestEndpoint(prNumber, responseJson, 200);
  }

  private String readTestResourceFile(String filename) throws IOException {
    return IOUtils.resourceToString("/%s/%s".formatted(getClass().getSimpleName(), filename), StandardCharsets.UTF_8);
  }

  private SourceControlEvent createSinglePrEvent(int prNumber, Application application) {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("event-" + prNumber);
    event.setEventType(SourceControlEvent.PR_STATE_UPDATE_EVENT);
    event.setApplicationId(application.getId());
    event.setPullRequestNumber(prNumber);
    return event;
  }

  private SourceControlEvent createBatchPrEvent(int[] prNumbers, Application application) {
    SourceControlEvent event = new SourceControlEvent();
    event.setId("batch-event");
    event.setEventType(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
    event.setApplicationId(application.getId());

    // Create JSON array of PR numbers
    String detailsJson = "[%s]".formatted(Arrays.stream(prNumbers)
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(",")));

    event.setEventStatusDetails(detailsJson);
    return event;
  }

  @Test
  public void testHandleSinglePrEvent_PrMerged() throws Exception {
    int prNumber = 3;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "different-base-branch", // Using different value to verify it gets updated
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is MERGED
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-merged.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.MERGED);

    // Verify branch and commit hash information
    assertThat(updatedPr.getBranchName()).isEqualTo("pr-commenting-test-2");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("011279f12d6740fca6a76cef0f49ccf8b9f51a5d");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("cb3cb611ad1a1b7922416b3400ea64cd83e07c60");

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_PrClosed() throws Exception {
    int prNumber = 3;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "old-target-branch", // Using different value to verify it gets updated
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is DECLINED
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-declined.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.CLOSED);

    // Verify branch and commit hash information
    assertThat(updatedPr.getBranchName()).isEqualTo("pr-commenting-test-2");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("011279f12d6740fca6a76cef0f49ccf8b9f51a5d");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("cb3cb611ad1a1b7922416b3400ea64cd83e07c60");

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_PrStillOpen() throws Exception {
    int prNumber = 6;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "develop", // Using different value to verify it gets updated to main
        PullRequestState.OPEN);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is OPEN
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-open.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.OPEN);

    // Verify branch and commit hash information
    assertThat(updatedPr.getBranchName()).isEqualTo("foo");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("9c83f35a0ac808df52286e19a72365d713bca86a");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("137f2be0e87037a7e93f1aa83e867189e0792cd1");

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_PrMissing() throws Exception {
    int prNumber = 3;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "main",
        PullRequestState.OPEN);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-invalid-pr.json", 404);

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.MISSING);

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    // Last detected update time should be updated since the state changed
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_PullRequestNotFoundInDatabase() throws Exception {
    String missingId = "missing-id";
    int prNumber = 3;
    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is OPEN
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-open.json");

    // Execute
    handler.handle(event);

    // Verify
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(missingId);
    assertThat(updatedPr).isNull();

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_ApiError() throws Exception {
    int prNumber = 3;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "error-branch", // Using a value that should not change due to API error
        PullRequestState.OPEN);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock broken Bitbucket API response
    setupBitbucketPullRequestEndpoint(prNumber, "Internal Server Error", 500);

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.OPEN); // State should remain unchanged

    // Verify branch and commit hash information remains unchanged during API error
    assertThat(updatedPr.getBranchName()).isEqualTo("foo-branch");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("error-branch"); // Should not be updated due to API error
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("deadbeef");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("deadbeef2");

    // Do not update lastCheckTime if there was an error fetching from the API
    assertThat(updatedPr.getLastCheckTime()).isNotBetween(before, after, true, true);
    assertThat(updatedPr.getLastDetectedUpdateTime()).isNotBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleBatchPrEvent_MultiplePrStates() throws Exception {
    // Create multiple PRs with different numbers
    int pr1Number = 1;
    int pr2Number = 2;
    int pr3Number = 3;
    int pr4Number = 4;

    SourceControlPullRequest pr1 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr1Number,
        "deadbeef1",
        "deadbeef2",
        "branch1",
        "feature-branch", // Different value to verify it gets updated
        PullRequestState.OPEN);
    pr1.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr1);

    SourceControlPullRequest pr2 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr2Number,
        "deadbeef2",
        "deadbeef3",
        "branch2",
        "release-branch",
        PullRequestState.OPEN);
    pr2.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr2);

    SourceControlPullRequest pr3 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr3Number,
        "deadbeef3",
        "deadbeef4",
        "branch3",
        "master", // Different value to verify it gets updated
        PullRequestState.OPEN);
    pr3.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr3);

    SourceControlPullRequest pr4 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr4Number,
        "deadbeef5",
        "deadbeef6",
        "branch4",
        "hotfix-branch", // Different value to verify it gets updated
        PullRequestState.OPEN);
    pr4.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr4);

    // Create a batch PR state update event
    int[] prNumbers = {pr1Number, pr2Number, pr3Number, pr4Number};
    SourceControlEvent batchEvent = createBatchPrEvent(prNumbers, applicationWithBatchSupport);
    sourceControlEventDAO.insert(batchEvent);

    // Setup the GitLab API response with different PR states
    setupGitlabMergeRequestsEndpoint(prNumbers, "gitlab-batch.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(batchEvent);

    // Verify
    Date after = new Date();

    // Verify PR states are updated correctly according to the response
    SourceControlPullRequest updatedPr1 = sourceControlPullRequestDAO.getById(pr1.getId());
    assertThat(updatedPr1).isNotNull();
    assertThat(updatedPr1.getState()).isEqualTo(PullRequestState.OPEN); // PR 1 is still OPEN
    // Verify branch names and commit hashes for PR 1
    assertThat(updatedPr1.getBranchName()).isEqualTo("172a68/ch.qos.logback/logback-access/0.6-to-1.3.15");
    assertThat(updatedPr1.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr1.getHeadCommitHash()).isEqualTo("85db157527840b31a5ad650b688ab77408d3abaf");
    // Gitlab doesn't return the information necessary to update the base commit hash
    assertThat(updatedPr1.getBaseCommitHash()).isEqualTo("deadbeef2");
    // Verify timestamps
    assertThat(updatedPr1.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr1.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    SourceControlPullRequest updatedPr2 = sourceControlPullRequestDAO.getById(pr2.getId());
    assertThat(updatedPr2).isNotNull();
    assertThat(updatedPr2.getState()).isEqualTo(PullRequestState.MISSING); // PR 2 does not exist
    // Branch names and commit hashes should remain unchanged for missing PR
    assertThat(updatedPr2.getBranchName()).isEqualTo("branch2");
    assertThat(updatedPr2.getBaseBranchName()).isEqualTo("release-branch");
    assertThat(updatedPr2.getHeadCommitHash()).isEqualTo("deadbeef2");
    assertThat(updatedPr2.getBaseCommitHash()).isEqualTo("deadbeef3");
    // Verify timestamps
    assertThat(updatedPr2.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr2.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    SourceControlPullRequest updatedPr3 = sourceControlPullRequestDAO.getById(pr3.getId());
    assertThat(updatedPr3).isNotNull();
    assertThat(updatedPr3.getState()).isEqualTo(PullRequestState.MERGED); // PR 3 should be MERGED
    // Verify branch names and commit hashes for PR 3
    assertThat(updatedPr3.getBranchName()).isEqualTo("foo");
    assertThat(updatedPr3.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr3.getHeadCommitHash()).isEqualTo("74a6286e67e98e4294b2e546f3622eb01f81dde6");
    assertThat(updatedPr3.getBaseCommitHash()).isEqualTo("deadbeef4");
    // Verify timestamps
    assertThat(updatedPr3.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr3.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    SourceControlPullRequest updatedPr4 = sourceControlPullRequestDAO.getById(pr4.getId());
    assertThat(updatedPr4).isNotNull();
    assertThat(updatedPr4.getState()).isEqualTo(PullRequestState.CLOSED); // PR 4 should be CLOSED
    // Verify branch names and commit hashes for PR 4
    assertThat(updatedPr4.getBranchName()).isEqualTo("bar");
    assertThat(updatedPr4.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr4.getHeadCommitHash()).isEqualTo("5a30ddee14603a702b2b544f5de20c5dbb1821a1");
    assertThat(updatedPr4.getBaseCommitHash()).isEqualTo("deadbeef6");
    // Verify timestamps
    assertThat(updatedPr4.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr4.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(batchEvent.getId())).isNull();
  }

  @Test
  public void testHandleBatchPrEvent_ApiError() throws Exception {
    // Create multiple PRs
    int pr1Number = 1;
    int pr2Number = 2;
    int pr3Number = 3;

    SourceControlPullRequest pr1 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr1Number,
        "deadbeef1",
        "deadbeef2",
        "branch1",
        "batch-error-branch1", // Custom value for error testing
        PullRequestState.OPEN);

    SourceControlPullRequest pr2 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr2Number,
        "deadbeef3",
        "deadbeef4",
        "branch2",
        "batch-error-branch2", // Custom value for error testing
        PullRequestState.OPEN);

    SourceControlPullRequest pr3 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr3Number,
        "deadbeef5",
        "deadbeef6",
        "branch3",
        "batch-error-branch3", // Custom value for error testing
        PullRequestState.OPEN);

    // Create a batch PR state update event
    int[] prNumbers = {pr1Number, pr2Number, pr3Number};
    SourceControlEvent batchEvent = createBatchPrEvent(prNumbers, applicationWithBatchSupport);
    sourceControlEventDAO.insert(batchEvent);

    // Setup the GitLab API to return an error
    setupGitlabMergeRequestsEndpoint(prNumbers, "Internal Server Error", 500);

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(batchEvent);

    // Verify
    Date after = new Date();

    // Verify PR states remain unchanged when API returns an error
    SourceControlPullRequest updatedPr1 = sourceControlPullRequestDAO.getById(pr1.getId());
    assertThat(updatedPr1).isNotNull();
    assertThat(updatedPr1.getState()).isEqualTo(PullRequestState.OPEN);
    // Verify branch names and commit hashes remain unchanged
    assertThat(updatedPr1.getBranchName()).isEqualTo("branch1");
    assertThat(updatedPr1.getBaseBranchName()).isEqualTo("batch-error-branch1");
    assertThat(updatedPr1.getHeadCommitHash()).isEqualTo("deadbeef1");
    assertThat(updatedPr1.getBaseCommitHash()).isEqualTo("deadbeef2");
    assertThat(updatedPr1.getLastCheckTime()).isNotBetween(before, after, true, true);
    assertThat(updatedPr1.getLastDetectedUpdateTime()).isNotBetween(before, after, true, true);

    SourceControlPullRequest updatedPr2 = sourceControlPullRequestDAO.getById(pr2.getId());
    assertThat(updatedPr2).isNotNull();
    assertThat(updatedPr2.getState()).isEqualTo(PullRequestState.OPEN);
    // Verify branch names and commit hashes remain unchanged
    assertThat(updatedPr2.getBranchName()).isEqualTo("branch2");
    assertThat(updatedPr2.getBaseBranchName()).isEqualTo("batch-error-branch2");
    assertThat(updatedPr2.getHeadCommitHash()).isEqualTo("deadbeef3");
    assertThat(updatedPr2.getBaseCommitHash()).isEqualTo("deadbeef4");
    assertThat(updatedPr2.getLastCheckTime()).isNotBetween(before, after, true, true);
    assertThat(updatedPr2.getLastDetectedUpdateTime()).isNotBetween(before, after, true, true);

    SourceControlPullRequest updatedPr3 = sourceControlPullRequestDAO.getById(pr3.getId());
    assertThat(updatedPr3).isNotNull();
    assertThat(updatedPr3.getState()).isEqualTo(PullRequestState.OPEN);
    // Verify branch names and commit hashes remain unchanged
    assertThat(updatedPr3.getBranchName()).isEqualTo("branch3");
    assertThat(updatedPr3.getBaseBranchName()).isEqualTo("batch-error-branch3");
    assertThat(updatedPr3.getHeadCommitHash()).isEqualTo("deadbeef5");
    assertThat(updatedPr3.getBaseCommitHash()).isEqualTo("deadbeef6");
    assertThat(updatedPr3.getLastCheckTime()).isNotBetween(before, after, true, true);
    assertThat(updatedPr3.getLastDetectedUpdateTime()).isNotBetween(before, after, true, true);

    // But the event should still be deleted
    assertThat(sourceControlEventDAO.getById(batchEvent.getId())).isNull();
  }

  @Test
  public void testHandleBatchPrEvent_PrNotFoundInDatabase() throws Exception {
    // Create two PRs, but not one with PR number 4
    int pr1Number = 1;
    int pr3Number = 3;
    int pr4Number = 4;

    SourceControlPullRequest pr1 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr1Number,
        "deadbeef1",
        "deadbeef2",
        "branch1",
        "dev-branch", // Different value to verify it gets updated
        PullRequestState.OPEN);
    pr1.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr1);

    SourceControlPullRequest pr3 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr3Number,
        "deadbeef5",
        "deadbeef6",
        "branch3",
        "legacy-branch", // Different value to verify it gets updated
        PullRequestState.OPEN);
    pr3.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pr3);

    // Create a batch PR state update event that includes PR 4 which doesn't exist in database
    int[] prNumbers = {pr1Number, pr3Number, pr4Number};
    SourceControlEvent batchEvent = createBatchPrEvent(prNumbers, applicationWithBatchSupport);
    sourceControlEventDAO.insert(batchEvent);

    // Setup the GitLab API response that includes all three PRs
    setupGitlabMergeRequestsEndpoint(prNumbers, "gitlab-batch.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(batchEvent);

    // Verify
    Date after = new Date();

    // Verify PR states are updated correctly according to the response
    SourceControlPullRequest updatedPr1 = sourceControlPullRequestDAO.getById(pr1.getId());
    assertThat(updatedPr1).isNotNull();
    assertThat(updatedPr1.getState()).isEqualTo(PullRequestState.OPEN); // PR 1 is still OPEN
    // Verify branch names and commit hashes for PR 1
    assertThat(updatedPr1.getBranchName()).isEqualTo("172a68/ch.qos.logback/logback-access/0.6-to-1.3.15");
    assertThat(updatedPr1.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr1.getHeadCommitHash()).isEqualTo("85db157527840b31a5ad650b688ab77408d3abaf");
    // Gitlab doesn't return the information necessary to update the base commit hash
    assertThat(updatedPr1.getBaseCommitHash()).isEqualTo("deadbeef2");
    // Verify timestamps
    assertThat(updatedPr1.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr1.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // PR #3 should be updated to MERGED
    SourceControlPullRequest updatedPr3 = sourceControlPullRequestDAO.getById(pr3.getId());
    assertThat(updatedPr3).isNotNull();
    assertThat(updatedPr3.getState()).isEqualTo(PullRequestState.MERGED);
    // Verify branch names and commit hashes for PR 3
    assertThat(updatedPr3.getBranchName()).isEqualTo("foo");
    assertThat(updatedPr3.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr3.getHeadCommitHash()).isEqualTo("74a6286e67e98e4294b2e546f3622eb01f81dde6");
    assertThat(updatedPr3.getBaseCommitHash()).isEqualTo("deadbeef6");
    // Verify timestamps
    assertThat(updatedPr3.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr3.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(batchEvent.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_InvalidRepository() throws Exception {
    int prNumber = 6;
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "invalid-repo-branch", // Using a value that shouldn't change
        PullRequestState.OPEN);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing invalid repository
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-invalid-repo.json", 404);

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();

    // PR should be marked missing
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.MISSING);

    // Branch and commit info should remain unchanged
    assertThat(updatedPr.getBranchName()).isEqualTo("foo-branch");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("invalid-repo-branch"); // Should keep original value
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("deadbeef");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("deadbeef2");

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    // Last detected update time should be updated since the state changed
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleBatchPrEvent_InvalidRepository() throws Exception {
    int pr1Number = 1;

    SourceControlPullRequest pr1 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr1Number,
        "deadbeef1",
        "deadbeef2",
        "branch1",
        "batch-invalid-repo-branch", // Should not change since repo is invalid
        PullRequestState.OPEN);

    // Create a batch PR state update event
    int[] prNumbers = {pr1Number};
    SourceControlEvent batchEvent = createBatchPrEvent(prNumbers, applicationWithBatchSupport);
    sourceControlEventDAO.insert(batchEvent);

    // Setup the GitLab API to return a repository not found error
    setupGitlabMergeRequestsEndpoint(prNumbers, "gitlab-invalid-repo.json", 404);

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(batchEvent);

    // Verify
    Date after = new Date();

    // Verify PR states remain unchanged when API returns a repository not found error
    SourceControlPullRequest updatedPr1 = sourceControlPullRequestDAO.getById(pr1.getId());
    assertThat(updatedPr1).isNotNull();
    assertThat(updatedPr1.getState()).isEqualTo(PullRequestState.MISSING);

    // Branch and commit info should remain unchanged
    assertThat(updatedPr1.getBranchName()).isEqualTo("branch1");
    assertThat(updatedPr1.getBaseBranchName()).isEqualTo("batch-invalid-repo-branch");
    assertThat(updatedPr1.getHeadCommitHash()).isEqualTo("deadbeef1");
    assertThat(updatedPr1.getBaseCommitHash()).isEqualTo("deadbeef2");
    assertThat(updatedPr1.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr1.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted even when repository is invalid
    assertThat(sourceControlEventDAO.getById(batchEvent.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_NoChanges() throws Exception {
    int prNumber = 7;
    // Initial setup with already correct data that should match the API response
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "9c83f35a0ac808df52286e19a72365d713bca86a", // Head commit hash that matches API response
        "137f2be0e87037a7e93f1aa83e867189e0792cd1", // Base commit hash that matches API response
        "foo", // Branch name that matches API response
        "main", // Base branch name that matches API response
        PullRequestState.OPEN);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Set up the API to return data that matches what we already have
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-open.json");

    // Record the current lastDetectedUpdateTime to verify it doesn't change
    Date originalLastDetectedUpdateTime = pullRequest.getLastDetectedUpdateTime();

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    // Verify
    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();

    // State should remain OPEN
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.OPEN);

    // Branch and commit info should remain unchanged
    assertThat(updatedPr.getBranchName()).isEqualTo("foo");
    assertThat(updatedPr.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr.getHeadCommitHash()).isEqualTo("9c83f35a0ac808df52286e19a72365d713bca86a");
    assertThat(updatedPr.getBaseCommitHash()).isEqualTo("137f2be0e87037a7e93f1aa83e867189e0792cd1");

    // Verify lastCheckTime is updated
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);

    // Verify lastDetectedUpdateTime is NOT updated since there were no changes
    assertThat(updatedPr.getLastDetectedUpdateTime()).isEqualTo(originalLastDetectedUpdateTime);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleBatchPrEvent_NoChanges() throws Exception {
    // Create multiple PRs with data that already matches the expected API response
    int pr1Number = 1;
    int pr3Number = 3;
    int pr4Number = 4;

    // PR1 - Open status with matching data
    SourceControlPullRequest pr1 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr1Number,
        "85db157527840b31a5ad650b688ab77408d3abaf", // Head commit hash that matches API response
        "deadbeef2", // Any value is fine, as the test will mock the base commit hash
        "172a68/ch.qos.logback/logback-access/0.6-to-1.3.15", // Branch name that matches API response
        "main", // Base branch name that matches API response
        PullRequestState.OPEN);

    // PR3 - Merged status with matching data
    SourceControlPullRequest pr3 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr3Number,
        "74a6286e67e98e4294b2e546f3622eb01f81dde6", // Head commit hash that matches API response
        "deadbeef4", // Any value is fine, as the test will mock the base commit hash
        "foo", // Branch name that matches API response
        "main", // Base branch name that matches API response
        PullRequestState.MERGED);

    // PR4 - Closed status with matching data
    SourceControlPullRequest pr4 = tempEntity.newSourceControlPullRequest(
        gitlabRepoUrl,
        pr4Number,
        "5a30ddee14603a702b2b544f5de20c5dbb1821a1", // Head commit hash that matches API response
        "deadbeef6", // Any value is fine, as the test will mock the base commit hash
        "bar", // Branch name that matches API response
        "main", // Base branch name that matches API response
        PullRequestState.CLOSED);

    // Save original timestamps to verify they don't change
    Date pr1OriginalUpdateTime = pr1.getLastDetectedUpdateTime();
    Date pr3OriginalUpdateTime = pr3.getLastDetectedUpdateTime();
    Date pr4OriginalUpdateTime = pr4.getLastDetectedUpdateTime();

    // Create a batch PR state update event
    int[] prNumbers = {pr1Number, pr3Number, pr4Number};
    SourceControlEvent batchEvent = createBatchPrEvent(prNumbers, applicationWithBatchSupport);
    sourceControlEventDAO.insert(batchEvent);

    // Setup the GitLab API response with the same data
    setupGitlabMergeRequestsEndpoint(prNumbers, "gitlab-batch.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(batchEvent);

    // Verify
    Date after = new Date();

    // Verify PR states and data remain unchanged
    SourceControlPullRequest updatedPr1 = sourceControlPullRequestDAO.getById(pr1.getId());
    assertThat(updatedPr1).isNotNull();
    assertThat(updatedPr1.getState()).isEqualTo(PullRequestState.OPEN);
    assertThat(updatedPr1.getBranchName()).isEqualTo("172a68/ch.qos.logback/logback-access/0.6-to-1.3.15");
    assertThat(updatedPr1.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr1.getHeadCommitHash()).isEqualTo("85db157527840b31a5ad650b688ab77408d3abaf");
    // Verify lastCheckTime is updated
    assertThat(updatedPr1.getLastCheckTime()).isBetween(before, after, true, true);
    // Verify lastDetectedUpdateTime is NOT updated since there were no changes
    assertThat(updatedPr1.getLastDetectedUpdateTime()).isEqualTo(pr1OriginalUpdateTime);

    SourceControlPullRequest updatedPr3 = sourceControlPullRequestDAO.getById(pr3.getId());
    assertThat(updatedPr3).isNotNull();
    assertThat(updatedPr3.getState()).isEqualTo(PullRequestState.MERGED);
    assertThat(updatedPr3.getBranchName()).isEqualTo("foo");
    assertThat(updatedPr3.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr3.getHeadCommitHash()).isEqualTo("74a6286e67e98e4294b2e546f3622eb01f81dde6");
    // Verify lastCheckTime is updated
    assertThat(updatedPr3.getLastCheckTime()).isBetween(before, after, true, true);
    // Verify lastDetectedUpdateTime is NOT updated since there were no changes
    assertThat(updatedPr3.getLastDetectedUpdateTime()).isEqualTo(pr3OriginalUpdateTime);

    SourceControlPullRequest updatedPr4 = sourceControlPullRequestDAO.getById(pr4.getId());
    assertThat(updatedPr4).isNotNull();
    assertThat(updatedPr4.getState()).isEqualTo(PullRequestState.CLOSED);
    assertThat(updatedPr4.getBranchName()).isEqualTo("bar");
    assertThat(updatedPr4.getBaseBranchName()).isEqualTo("main");
    assertThat(updatedPr4.getHeadCommitHash()).isEqualTo("5a30ddee14603a702b2b544f5de20c5dbb1821a1");
    // Verify lastCheckTime is updated
    assertThat(updatedPr4.getLastCheckTime()).isBetween(before, after, true, true);
    // Verify lastDetectedUpdateTime is NOT updated since there were no changes
    assertThat(updatedPr4.getLastDetectedUpdateTime()).isEqualTo(pr4OriginalUpdateTime);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(batchEvent.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_ZeroPullRequestNumber() {
    SourceControlEvent event = createSinglePrEvent(0, applicationWithoutBatchSupport);

    sourceControlEventDAO.insert(event);

    handler.handle(event);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_NegativePullRequestNumber() {
    SourceControlEvent event = createSinglePrEvent(-1, applicationWithoutBatchSupport);

    sourceControlEventDAO.insert(event);

    handler.handle(event);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandleSinglePrEvent_IntegerPullRequestNumber() throws Exception {
    Integer prNumber = Integer.valueOf(42);
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "foo-branch",
        "integer-test-branch",
        PullRequestState.OPEN);

    // Create a PR state update event using explicit Integer
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is OPEN
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-open.json");

    // Capture timestamps for checking
    Date before = new Date();

    // Execute
    handler.handle(event);

    Date after = new Date();
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.OPEN);

    // Verify timestamps
    assertThat(updatedPr.getLastCheckTime()).isBetween(before, after, true, true);
    assertThat(updatedPr.getLastDetectedUpdateTime()).isBetween(before, after, true, true);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_ManualPullRequest_DoesNotClose() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControl sourceControl = tempEntity.newSourceControl(
        githubApp.getId(),
        "https://github.com/test-org/test-repo.git",
        passwordHandler.encryptPassword(TOKEN),
        GITHUB);
    sourceControl.setClosePrOnFailedChecksEnabled(true);
    sourceControlDAO.update(sourceControl);

    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        "https://github.com/test-org/test-repo.git",
        124,
        "abc124",
        "def457",
        "manual-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.MANUAL);
    sourceControlPullRequestDAO.update(pullRequest);

    PullRequestLifecycleInfo prLifecycleInfo = mock(PullRequestLifecycleInfo.class);

    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isEmpty();
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_FeatureDisabled_DoesNotClose() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        "https://github.com/test-org/test-repo.git",
        125,
        "abc125",
        "def458",
        "auto-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    PullRequestLifecycleInfo prLifecycleInfo = mock(PullRequestLifecycleInfo.class);

    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isEmpty();
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_oldPR() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITHUB, false, true, 5);

    PullRequestLifecycleInfo prLifecycleInfo = createGithubPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This pull request was automatically closed.**  \n" +
            "This automated pull request was not merged and has been closed after 5 days of inactivity, " +
            "per Lifecycle configuration.");

    // and when:
    handler.updateSourceControlPullRequest(pullRequest, prLifecycleInfo, true);

    // then:
    SourceControlPullRequest updatedPullRequest =
        sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(githubApp.getId(), 1);
    assertThat(updatedPullRequest.getState()).isEqualTo(PullRequestState.AUTO_CLOSED);
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_failedChecks() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITHUB, true, false, 0);

    PullRequestLifecycleInfo prLifecycleInfo = createGithubPullRequestLifecycleInfo(true);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This pull request was automatically closed.**  \n" +
            "This automated pull request failed one or more required checks and has been closed, " +
            "per Lifecycle configuration.");
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_NoAutoClose_noFailedChecks() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITHUB, true, false, 0);

    PullRequestLifecycleInfo prLifecycleInfo = createGithubPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isEmpty();
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_oldPR_Gitlab() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITLAB, false, true, 5);

    PullRequestLifecycleInfo prLifecycleInfo = createGitlabPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This merge request was automatically closed.**  \n" +
            "This automated merge request was not merged and has been closed after 5 days of inactivity, " +
            "per Lifecycle configuration.");

    // and when:
    handler.updateSourceControlPullRequest(pullRequest, prLifecycleInfo, true);

    // then:
    SourceControlPullRequest updatedPullRequest =
        sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(githubApp.getId(), 1);
    assertThat(updatedPullRequest.getState()).isEqualTo(PullRequestState.AUTO_CLOSED);
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_failedChecks_Gitlab() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITLAB, true, false, 0);

    PullRequestLifecycleInfo prLifecycleInfo = createGitlabPullRequestLifecycleInfo(true);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This merge request was automatically closed.**  \n" +
            "This automated merge request failed one or more required checks and has been closed, " +
            "per Lifecycle configuration.");
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_NoAutoClose_noFailedChecks_Gitlab() {
    Application githubApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(githubApp, GITLAB, true, false, 0);

    PullRequestLifecycleInfo prLifecycleInfo = createGitlabPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(githubApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(githubApp.getId());
    assertThat(events).isEmpty();
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_oldPR_Azure() {
    Application azureApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(azureApp, AZURE, false, true, 5);

    PullRequestLifecycleInfo prLifecycleInfo = createGitlabPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(azureApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(azureApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This pull request was automatically closed.**  \n" +
            "This automated pull request was not merged and has been closed after 5 days of inactivity, " +
            "per Lifecycle configuration.");

    // and when:
    handler.updateSourceControlPullRequest(pullRequest, prLifecycleInfo, true);

    // then:
    SourceControlPullRequest updatedPullRequest =
        sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(azureApp.getId(), 1);
    assertThat(updatedPullRequest.getState()).isEqualTo(PullRequestState.AUTO_CLOSED);
  }

  @Test
  public void testCloseAutoPullRequestIfEnabled_TriggerAutoClose_oldPR_Bitbucket() {
    Application bitbucketApp = tempEntity.newApplicationWithParent();

    SourceControlPullRequest pullRequest =
        setupSourceControlAndPullRequestForAutoPrClosing(bitbucketApp, BITBUCKET, false, true, 5);

    PullRequestLifecycleInfo prLifecycleInfo = createGitlabPullRequestLifecycleInfo(false);

    // when:
    handler.closeAutoPullRequestIfEnabled(bitbucketApp.getId(), pullRequest, prLifecycleInfo);

    // then:
    List<SourceControlEvent> events = sourceControlEventDAO.getAllByApplicationId(bitbucketApp.getId());
    assertThat(events).isNotEmpty();
    SourceControlEvent firstEvent = events.get(0);
    assertThat(firstEvent.getEventType()).isEqualTo(CLOSE_PULL_REQUEST_EVENT);
    assertThat(firstEvent.getPullRequestNumber()).isEqualTo(1);
    assertThat(firstEvent.getPullRequestContents()).isEqualTo(
        "**This pull request was automatically closed.**  \n" +
            "This automated pull request was not merged and has been closed after 5 days of inactivity, " +
            "per Lifecycle configuration.");

    // and when:
    handler.updateSourceControlPullRequest(pullRequest, prLifecycleInfo, true);

    // then:
    SourceControlPullRequest updatedPullRequest =
        sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(bitbucketApp.getId(), 1);
    assertThat(updatedPullRequest.getState()).isEqualTo(PullRequestState.AUTO_CLOSED);
  }

  @Test
  public void testHandle_TelemetrySent_PrOpenToOpen_NoTelemetryEmitted() throws Exception {
    int prNumber = 103;

    // Create a PR that will remain OPEN (no state transition)
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "telemetry-no-transition",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Create a remediation event for this PR
    SourceControlEvent remediationEvent = new SourceControlEvent();
    remediationEvent.setId("remediation-event-" + prNumber);
    remediationEvent.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    remediationEvent.setApplicationId(applicationWithoutBatchSupport.getId());
    remediationEvent.setPullRequestNumber(prNumber);
    remediationEvent.setIsGoldenPullRequest(true);
    remediationEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(remediationEvent);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is still OPEN
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-open.json");

    // Execute with mocked handler
    handlerWithMockedTelemetry.handle(event);

    // Verify PR state remains OPEN (no lifecycle transition)
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.OPEN);

    // Verify NO telemetry was sent for non-lifecycle transition
    verifyNoInteractions(mockTelemetrySender);

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandle_TelemetrySent_PrMerged_SendsCorrectTelemetryFields() throws Exception {
    int prNumber = 200;
    String applicationId = applicationWithoutBatchSupport.getId();

    // Create a PR that will transition from OPEN to MERGED
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "telemetry-verify-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Create a golden remediation event for this PR
    SourceControlEvent remediationEvent = new SourceControlEvent();
    remediationEvent.setId("remediation-event-" + prNumber);
    remediationEvent.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    remediationEvent.setApplicationId(applicationId);
    remediationEvent.setPullRequestNumber(prNumber);
    remediationEvent.setIsGoldenPullRequest(true);
    remediationEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(remediationEvent);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is MERGED
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-merged.json");

    // Configure mocks
    when(mockTelemetryUtils.obfuscate(applicationId)).thenReturn("obfuscated-" + applicationId);
    when(mockTelemetryUtils.convertGoldenStatusToString(true)).thenReturn("golden");

    // Execute with mocked handler
    handlerWithMockedTelemetry.handle(event);

    // Verify PR state was updated to MERGED
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.MERGED);

    // Verify telemetry was sent with correct fields
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);

    // Verify new field names and values
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(prNumber);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + applicationId);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("golden");
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo("AUTOMATIC");
    assertThat(telemetryData.getAttributes().get("event_type")).isEqualTo("pr_merged");
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandle_TelemetrySent_PrClosed_SendsCorrectTelemetryFields() throws Exception {
    int prNumber = 201;
    String applicationId = applicationWithoutBatchSupport.getId();

    // Create a PR that will transition from OPEN to CLOSED
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "telemetry-closed-verify-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Create a non-golden remediation event for this PR
    SourceControlEvent remediationEvent = new SourceControlEvent();
    remediationEvent.setId("remediation-event-" + prNumber);
    remediationEvent.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    remediationEvent.setApplicationId(applicationId);
    remediationEvent.setPullRequestNumber(prNumber);
    remediationEvent.setIsGoldenPullRequest(false);
    remediationEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(remediationEvent);

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is CLOSED
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-declined.json");

    // Configure mocks
    when(mockTelemetryUtils.obfuscate(applicationId)).thenReturn("obfuscated-" + applicationId);
    when(mockTelemetryUtils.convertGoldenStatusToString(false)).thenReturn("not_golden");

    // Execute with mocked handler
    handlerWithMockedTelemetry.handle(event);

    // Verify PR state was updated to CLOSED
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.CLOSED);

    // Verify telemetry was sent with correct fields
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);

    // Verify new field names and values
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(prNumber);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + applicationId);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("not_golden");
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo("AUTOMATIC");
    assertThat(telemetryData.getAttributes().get("event_type")).isEqualTo("pr_closed_unmerged");
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  @Test
  public void testHandle_TelemetrySent_PrMerged_UnknownGoldenStatus() throws Exception {
    int prNumber = 202;
    String applicationId = applicationWithoutBatchSupport.getId();

    // Create a PR that will transition from OPEN to MERGED
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        bitbucketRepoUrl,
        prNumber,
        "deadbeef",
        "deadbeef2",
        "telemetry-unknown-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    sourceControlPullRequestDAO.update(pullRequest);

    // Note: NOT creating any remediation event, so golden status should be unknown

    // Create a PR state update event
    SourceControlEvent event = createSinglePrEvent(prNumber, applicationWithoutBatchSupport);

    // Setup mock Bitbucket API response showing PR is MERGED
    setupBitbucketPullRequestEndpoint(prNumber, "bitbucket-merged.json");

    // Configure mocks
    when(mockTelemetryUtils.obfuscate(applicationId)).thenReturn("obfuscated-" + applicationId);

    // Execute with mocked handler
    handlerWithMockedTelemetry.handle(event);

    // Verify PR state was updated to MERGED
    SourceControlPullRequest updatedPr = sourceControlPullRequestDAO.getById(pullRequest.getId());
    assertThat(updatedPr).isNotNull();
    assertThat(updatedPr.getState()).isEqualTo(PullRequestState.MERGED);

    // Verify telemetry was sent with unknown golden status
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);

    // Verify golden status is unknown when no original remediation event exists
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("unknown");
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo("AUTOMATIC");
    assertThat(telemetryData.getAttributes().get("event_type")).isEqualTo("pr_merged");

    // Verify event is deleted
    assertThat(sourceControlEventDAO.getById(event.getId())).isNull();
  }

  private SourceControlPullRequest setupSourceControlAndPullRequestForAutoPrClosing(
      Application app,
      SourceControlProvider provider,
      boolean closePrOnFailedChecks,
      boolean closePrAfterDaysOpen,
      int closePrAfterDays)
  {
    String repoUrl = switch (provider) {
      case GITLAB -> "https://gitlab.com/test-org/test-repo.git";
      case AZURE -> "https://dev.azure.com/org/prj/_git/app";
      case BITBUCKET -> "https://bitbucket.org/test-org/test-repo.git";
      default -> "https://github.com/test-org/test-repo.git";
    };
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        repoUrl,
        1,
        "deadbeef1",
        "deadbeef2",
        "auto-branch",
        "main",
        PullRequestState.OPEN);
    pullRequest.setSource(PullRequestSource.AUTOMATIC);
    Instant tenDaysAgo = LocalDate.now().minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant();
    pullRequest.setCreateTime(Date.from(tenDaysAgo));
    sourceControlPullRequestDAO.update(pullRequest);

    SourceControl rootOrgSourceControl = (provider == AZURE || provider == BITBUCKET)
        ? tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "username", null, provider)
        : tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, provider);
    rootOrgSourceControl.setClosePrOnFailedChecksEnabled(closePrOnFailedChecks);
    rootOrgSourceControl.setClosePrAfterDaysOpenEnabled(closePrAfterDaysOpen);
    rootOrgSourceControl.setClosePrAfterDays(closePrAfterDays);
    sourceControlDAO.update(rootOrgSourceControl);

    tempEntity.newSourceControl(
        app.getId(),
        repoUrl,
        (provider == AZURE || provider == BITBUCKET) ? "username" : null,
        passwordHandler.encryptPassword(TOKEN),
        provider);
    return pullRequest;
  }

  private PullRequestLifecycleInfo createGithubPullRequestLifecycleInfo(boolean failedRequiredChecks) {
    PullRequest prLifecycleInfo = new PullRequest();
    prLifecycleInfo.setState(PullRequestState.OPEN.name());
    prLifecycleInfo.setHeadCommitHash("head-commit");
    prLifecycleInfo.setBaseCommitHash("base-commit");
    prLifecycleInfo.setBranchName("branch-name");
    prLifecycleInfo.setBaseBranchName("base-branch-name");

    CheckRun checkRun = new CheckRun();
    checkRun.name = "check-run-name";
    checkRun.status = "FAILURE";
    checkRun.isRequired = failedRequiredChecks;

    CheckRun[] checkRunNodes = {checkRun};
    CheckRuns checkRuns = new CheckRuns();
    checkRuns.nodes = checkRunNodes;

    CheckSuiteNode checkSuiteNode = new CheckSuiteNode();
    checkSuiteNode.checkRuns = checkRuns;
    CheckSuiteNode[] checkSuitesNodes = {checkSuiteNode};
    CheckSuites checkSuites = new CheckSuites();
    checkSuites.nodes = checkSuitesNodes;

    Commit commit = new Commit();
    commit.commitHash = "a-commit";
    commit.checkSuites = checkSuites;
    CommitNode commitNode = new CommitNode();
    commitNode.commit = commit;
    CommitNode[] nodes = {commitNode};
    PullRequestCommits commits = new PullRequestCommits();
    commits.nodes = nodes;
    prLifecycleInfo.setCommits(commits);

    return prLifecycleInfo;
  }

  private PullRequestLifecycleInfo createGitlabPullRequestLifecycleInfo(boolean failedRequiredChecks) {
    GitlabMergeRequestResponse prLifecycleInfo = new GitlabMergeRequestResponse();
    prLifecycleInfo.setHeadCommitHash("head-commit");
    prLifecycleInfo.setBaseCommitHash("base-commit");
    prLifecycleInfo.setHead("branch-name");
    prLifecycleInfo.setBase("base-branch-name");
    if (failedRequiredChecks) {
      prLifecycleInfo.setDetailedMergeStatus("ci_must_pass");
    }
    else {
      prLifecycleInfo.setDetailedMergeStatus("mergeable");
    }

    return prLifecycleInfo;
  }
}
