/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.helper.ApplicationEvaluationEventBuilder;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestCommentingEventHandlerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private AsyncEventBus mockAsyncEventBus;

  @Mock
  private PullRequestCommentingMetricsService mockPrCommentingMetricsService;

  @Mock
  private PullRequestCommentingService mockPullRequestCommentingService;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private PolicyEvaluationDAO mockPolicyEvaluationDAO;

  @Mock
  private PullRequestStatusService mockPullRequestStatusService;

  @Mock
  private GitCommitHistoryService mockGitCommitHistoryService;

  public PullRequestCommentingEventHandlerTest() {
    super(PullRequestCommentingEventHandler.class);
  }

  private TestProductLicense testProductLicense;

  private IqForScmLicenseChecker licenseChecker;

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();

    TestProductLicenseManager productLicenseManager = new TestProductLicenseManager();
    testProductLicense = new TestProductLicense(productLicenseManager, mock(DeveloperEnablementService.class));
    testProductLicense.reset();
    licenseChecker = new IqForScmLicenseChecker(testProductLicense);
  }

  @Test
  public void testOnApplicationEvaluation_featureDisabled() {
    // given: PR commenting feature disabled
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withCommentingFeatureEnabled(false)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when: handler invoked
    commentingEventHandler.onApplicationEvaluation(event);

    // then: should be no downstream interactions
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockPrCommentingMetricsService, never()).sendTelemetry(any());
  }

  @Test
  public void testOnApplicationEvaluation_Unlicensed() {
    // remove automation feature, leaving notifications
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);

    // given : commenting service object, scm enabled, and an event with a commit hash
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : a debug message is logged
    assertThatLogMessagesEqual(
        debug("License does not support source control automation feature"));

    // and : processing stops there
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockPrCommentingMetricsService, never()).sendTelemetry(any());
  }

  @Test
  public void testOnApplicationEvaluation_missingCommitHash() {
    // given : commenting service object, scm enabled, and an event without a commit hash
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .build();

    // when : process event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : comment was not created
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockPrCommentingMetricsService, never()).sendTelemetry(any());
    assertThatLogMessagesEqual(
        debug("no commit hash : skipping PR commenting for application 'app1' with policy evaluation 'pe1'"));
  }

  @Test
  public void testOnApplicationEvaluation_scmDisabled() {
    // given : commenting service object, scm disabled, and an event with a commit hash
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(false)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit123")
        .build();

    // when : process event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : comment was not created
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockPrCommentingMetricsService, never()).sendTelemetry(any());
    assertThatLogMessagesEqual(
        debug("scm disabled : skipping PR commenting for application 'app1' with policy evaluation 'pe1'"));
  }

  @Test
  public void testOnApplicationEvaluation_bitbucketCloud() {
    // given : commenting service object, scm is bitbucket cloud, and an event with a commit hash
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .withSourceControlProvider(SourceControlProvider.BITBUCKET)
        .withRepositoryUrl("https://bitbucket.org/test-org/test-app")
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit123")
        .build();

    // when : process event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : comment was not created
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    verify(mockPrCommentingMetricsService, never()).sendTelemetry(any());
    assertThatLogMessagesEqual(
        debug("'bitbucket' not currently supported for pull request commenting"));
  }

  @Test
  public void testOnApplicationEvaluation_publishSourceControlEvent() {
    // given : commenting service object, scm enabled, and an event with a commit hash and policy evaluation not
    // internally triggered
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_API);
    policyEvaluation.setId("pe1");

    doReturn(policyEvaluation).when(mockPolicyEvaluationDAO).getById("pe1");

    // when : check event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : event is ok for processing
    final ArgumentCaptor<SourceControlEvent> sourceControlEventCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventPublisher, times(1)).publishEvent(sourceControlEventCaptor.capture());
    verify(mockGitCommitHistoryService, never()).updateCommitHistoryForPolicyEvaluation(event.policyEvaluationId);
    final SourceControlEvent sourceControlEvent = sourceControlEventCaptor.getValue();
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(event.ownerId);
    assertThat(sourceControlEvent.getCommitHash()).isEqualTo(event.commitHash);
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    assertThat(sourceControlEvent.getPolicyEvaluationId()).isEqualTo(event.policyEvaluationId);
  }

  @Test
  public void testOnApplicationEvaluation_sourceControlEventNotCreatedBecausePolicyEvalInternalOnboarding() {
    // given : commenting service object, scm enabled, and an event with a commit hash and policy evaluation
    // internally triggered
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = createApplicationEventWithInternallyTriggeredPolicyEval(
        ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when : check event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : event is not published
    assertEventNotCreatedBecausePolicyEvalInternallyTriggered();

    // and: default branch commit history is updated
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForPolicyEvaluation(event.policyEvaluationId);
  }

  @Test
  public void testOnApplicationEvaluation_sourceControlEventNotCreatedBecausePolicyEvalInternalPullRequest() {
    // given : commenting service object, scm enabled, and an event with a commit hash and policy evaluation
    // internally triggered
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = createApplicationEventWithInternallyTriggeredPolicyEval(
        ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    // when : check event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : event is not published
    assertEventNotCreatedBecausePolicyEvalInternallyTriggered();

    // and: default branch commit history is updated
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForPolicyEvaluation(event.policyEvaluationId);
  }

  @Test
  public void testOnApplicationEvaluation_sourceControlEventNotCreatedBecausePolicyEvalInternalDefaultBranchMonitor() {
    // given : commenting service object, scm enabled, and an event with a commit hash and policy evaluation
    // internally triggered
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withScmEnabled(true)
        .build();

    ApplicationEvaluationEvent event = createApplicationEventWithInternallyTriggeredPolicyEval(
        ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);

    // when : check event
    commentingEventHandler.onApplicationEvaluation(event);

    // then : event is not published
    assertEventNotCreatedBecausePolicyEvalInternallyTriggered();

    // and: default branch commit history is updated
    verify(mockGitCommitHistoryService, times(1)).updateCommitHistoryForPolicyEvaluation(event.policyEvaluationId);
  }

  private ApplicationEvaluationEvent createApplicationEventWithInternallyTriggeredPolicyEval(
      ScanTriggerType scanTriggerType)
  {
    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setScanTriggerType(scanTriggerType);
    policyEvaluation.setId("pe1");

    doReturn(policyEvaluation).when(mockPolicyEvaluationDAO).getById("pe1");

    return event;
  }

  private void assertEventNotCreatedBecausePolicyEvalInternallyTriggered() {
    verify(mockSourceControlEventPublisher, times(0)).publishEvent(any());
    verify(mockPolicyEvaluationDAO, times(1)).getById("pe1");
    assertThatLogMessagesEqual(
        debug(
            "Ignoring ApplicationEvaluationEvent for application app1 because the policy evaluation pe1 was " +
                "internally triggered"));
  }

  @Test
  public void testOnApplicationEvaluationSourceControlEvent_createsCommentsAndCreatesPullRequestStatus() {
    // given: a scenario that will lead to comments being created
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs = createDTOs("app33", 5);
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withPullRequestPolicyEvaluationDTOs(pullRequestPolicyEvaluationDTOs)
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forApplicationEvaluation();

    // when: invoke handler
    commentingEventHandler.onApplicationEvaluation(sourceControlEvent);

    // then: there should have been multiple attempts to create PR comments
    final ArgumentCaptor<PullRequestPolicyEvaluationsDTO> policyEvalDTOCaptor =
        ArgumentCaptor.forClass(PullRequestPolicyEvaluationsDTO.class);
    verify(mockPullRequestCommentingService, times(pullRequestPolicyEvaluationDTOs.size()))
        .doCreateOrUpdatePullRequestComment(policyEvalDTOCaptor.capture());

    // and: there should have been 5 attempt to create a PR status
    verify(mockPullRequestStatusService, times(pullRequestPolicyEvaluationDTOs.size()))
        .doCreatePullRequestStatus(any(PullRequestPolicyEvaluationsDTO.class));

    List<PullRequestPolicyEvaluationsDTO> capturedDTOs = policyEvalDTOCaptor.getAllValues();
    verifyPullRequestPolicyEvalationsDTOs(capturedDTOs, pullRequestPolicyEvaluationDTOs);
  }

  @Test
  public void testOnApplicationEvaluationSourceControlEvent_doesNotCreateCommentsOrPullRequestStatus() {
    // given: a resolver that doesn't return any resolved PR policy evals
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forApplicationEvaluation();

    // when: invoke handler
    commentingEventHandler.onApplicationEvaluation(sourceControlEvent);

    // then: we never attempted to create comments
    final ArgumentCaptor<PullRequestPolicyEvaluationsDTO> policyEvalDTOCaptor =
        ArgumentCaptor.forClass(PullRequestPolicyEvaluationsDTO.class);
    verify(mockPullRequestCommentingService, never())
        .doCreateOrUpdatePullRequestComment(policyEvalDTOCaptor.capture());

    // and: we never attempted to create a PR status
    verify(mockPullRequestStatusService, never()).doCreatePullRequestStatus(policyEvalDTOCaptor.capture());

    List<PullRequestPolicyEvaluationsDTO> capturedDTOs = policyEvalDTOCaptor.getAllValues();
    assertThat(capturedDTOs).isEmpty();
  }

  @Test
  public void testOnDiscoveredPullRequest_createsCommentsAndCreatesPullRequestStatus() {
    // given: a resolver and a discovered PR event that will lead to comments being created
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs = createDTOs("app75", 1);
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withPullRequestPolicyEvaluationDTOs(pullRequestPolicyEvaluationDTOs)
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forDiscoveredPullRequest()
        .setPullRequestNumber(pullRequestPolicyEvaluationDTOs.get(0).getPullRequestNumber());

    // when: invoke handler
    commentingEventHandler.onDiscoveredPullRequest(sourceControlEvent);

    // then: there should have been 1 attempt to create a PR comment
    final ArgumentCaptor<PullRequestPolicyEvaluationsDTO> policyEvalDTOCaptor =
        ArgumentCaptor.forClass(PullRequestPolicyEvaluationsDTO.class);
    verify(mockPullRequestCommentingService, times(1))
        .doCreateOrUpdatePullRequestComment(policyEvalDTOCaptor.capture());

    // and: there should have been 1 attempt to create a PR status
    verify(mockPullRequestStatusService, times(1))
        .doCreatePullRequestStatus(policyEvalDTOCaptor.capture());

    PullRequestPolicyEvaluationsDTO capturedDTO = policyEvalDTOCaptor.getValue();
    assertThat(capturedDTO).isEqualTo(pullRequestPolicyEvaluationDTOs.get(0));
  }

  @Test
  public void testOnDiscoveredPullRequest_doesNotProcessPullRequest() {
    // given: a resolver that doesn't return any PR policy evals
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forDiscoveredPullRequest()
        .setPullRequestNumber(1);

    // when: invoke handler
    commentingEventHandler.onDiscoveredPullRequest(sourceControlEvent);

    // then: there should have been no attempt to create a PR comment
    verify(mockPullRequestCommentingService, never()).doCreateOrUpdatePullRequestComment(any());

    // and: we never attempted to create a PR status
    verify(mockPullRequestStatusService, never()).doCreatePullRequestStatus(any());
  }

  @Test
  public void testOnUpdatedPullRequest_createsCommentsAndCreatesPullRequestStatus() {
    // given: a resolver and a discovered PR event that will lead to comments being created
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs = createDTOs("testAppId", 1);
    pullRequestPolicyEvaluationDTOs.get(0)
        .getTargetPolicyEvaluation()
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);
    pullRequestPolicyEvaluationDTOs.get(0)
        .getFeatureBranchPolicyEvaluation()
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withPullRequestPolicyEvaluationDTOs(pullRequestPolicyEvaluationDTOs)
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent().forUpdatedPullRequest()
        .setPullRequestNumber(pullRequestPolicyEvaluationDTOs.get(0).getPullRequestNumber());

    // when: invoke handler
    commentingEventHandler.onUpdatedPullRequest(sourceControlEvent);

    // then: there should have been 1 attempt to create a PR comment
    final ArgumentCaptor<PullRequestPolicyEvaluationsDTO> policyEvalDTOCaptor =
        ArgumentCaptor.forClass(PullRequestPolicyEvaluationsDTO.class);
    verify(mockPullRequestCommentingService, times(1))
        .doCreateOrUpdatePullRequestComment(policyEvalDTOCaptor.capture());

    // and: there should have been 1 attempt to create a PR status
    verify(mockPullRequestStatusService, times(1))
        .doCreatePullRequestStatus(policyEvalDTOCaptor.capture());

    PullRequestPolicyEvaluationsDTO capturedDTO = policyEvalDTOCaptor.getValue();
    assertThat(capturedDTO).isEqualTo(pullRequestPolicyEvaluationDTOs.get(0));
  }

  @Test
  public void testOnUpdatedPullRequest_doesNotCreateCommentsOrPullRequestStatusIfThereAreNoPolicyEvaluations() {
    // given: a resolver that doesn't return any PR policy evals
    PullRequestCommentingEventHandler commentingEventHandler =
        new TestablePullRequestCommentingEventHandlerBuilder().build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent().forUpdatedPullRequest().setPullRequestNumber(1);

    // when: invoke handler
    commentingEventHandler.onUpdatedPullRequest(sourceControlEvent);

    // then: there should have been no attempt to create a PR comment
    verify(mockPullRequestCommentingService, never()).doCreateOrUpdatePullRequestComment(any());

    // and: we never attempted to create a PR status
    verify(mockPullRequestStatusService, never()).doCreatePullRequestStatus(any());
  }

  @Test
  public void testOnUpdatedPullRequest_doesNotCreateCommentsOrPRStatusIfDefaultBranchPolicyEvaluationIsExternal() {
    // given: a resolver and a discovered PR event that will lead to comments being created
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs = createDTOs("testAppId", 1);
    pullRequestPolicyEvaluationDTOs.get(0).getTargetPolicyEvaluation().setScanTriggerType(ScanTriggerType.CLI);
    pullRequestPolicyEvaluationDTOs.get(0)
        .getFeatureBranchPolicyEvaluation()
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withPullRequestPolicyEvaluationDTOs(pullRequestPolicyEvaluationDTOs)
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent().forUpdatedPullRequest()
        .setPullRequestNumber(pullRequestPolicyEvaluationDTOs.get(0).getPullRequestNumber());

    // when: invoke handler
    commentingEventHandler.onUpdatedPullRequest(sourceControlEvent);

    // then: there should have been no attempt to create a PR comment
    verify(mockPullRequestCommentingService, never()).doCreateOrUpdatePullRequestComment(any());

    // and: we never attempted to create a PR status
    verify(mockPullRequestStatusService, never()).doCreatePullRequestStatus(any());
  }

  @Test
  public void testOnUpdatedPullRequest_doesNotCreateCommentsOrPullRequestStatusIfPRBranchPolicyEvaluationIsExternal() {
    // given: a resolver and a discovered PR event that will lead to comments being created
    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs = createDTOs("testAppId", 1);
    pullRequestPolicyEvaluationDTOs.get(0)
        .getTargetPolicyEvaluation()
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);
    pullRequestPolicyEvaluationDTOs.get(0).getFeatureBranchPolicyEvaluation().setScanTriggerType(ScanTriggerType.CLI);
    PullRequestCommentingEventHandler commentingEventHandler = new TestablePullRequestCommentingEventHandlerBuilder()
        .withPullRequestPolicyEvaluationDTOs(pullRequestPolicyEvaluationDTOs)
        .build();

    SourceControlEvent sourceControlEvent = new SourceControlEvent().forUpdatedPullRequest()
        .setPullRequestNumber(pullRequestPolicyEvaluationDTOs.get(0).getPullRequestNumber());

    // when: invoke handler
    commentingEventHandler.onUpdatedPullRequest(sourceControlEvent);

    // then: there should have been no attempt to create a PR comment
    verify(mockPullRequestCommentingService, never()).doCreateOrUpdatePullRequestComment(any());

    // and: we never attempted to create a PR status
    verify(mockPullRequestStatusService, never()).doCreatePullRequestStatus(any());
  }

  @Test
  public void testRegisteredAndUnregisteredFromEventBus() throws Exception {
    // given: a commenting event handler
    PullRequestCommentingEventHandler commentingEventHandler =
        new TestablePullRequestCommentingEventHandlerBuilder()
            .build();

    // when: start the handler
    commentingEventHandler.start();

    // then: registered with event bus
    verify(mockAsyncEventBus, times(1)).register(commentingEventHandler);

    // when: stop the handler
    commentingEventHandler.stop();
    verify(mockAsyncEventBus, times(1)).unregister(commentingEventHandler);
  }

  private List<PullRequestPolicyEvaluationsDTO> createDTOs(String applicationId, int quantity) {
    List<PullRequestPolicyEvaluationsDTO> result = new ArrayList<>();

    Random random = new Random();
    for (int i = 0; i < quantity; i++) {
      PullRequestPolicyEvaluationsDTO dto = new PullRequestPolicyEvaluationsDTO()
          .setApplicationId(applicationId)
          .setPullRequestNumber(random.nextInt())
          .setPullRequestHeadCommit(UUID.randomUUID().toString())
          .setFeatureBranchName(UUID.randomUUID().toString())
          .setPullRequestHeadCommit(UUID.randomUUID().toString())
          .setFeatureBranchPolicyEvaluation(createPolicyEvaluation(applicationId))
          .setTargetPolicyEvaluation(createPolicyEvaluation(applicationId));
      result.add(dto);
    }

    return result;
  }

  private PolicyEvaluation createPolicyEvaluation(String applicationId) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setApplicationId(applicationId);
    policyEvaluation.setId(UUID.randomUUID().toString());
    return policyEvaluation;
  }

  private void verifyPullRequestPolicyEvalationsDTOs(
      List<PullRequestPolicyEvaluationsDTO> actual,
      List<PullRequestPolicyEvaluationsDTO> expected)
  {
    assertThat(actual).isNotEmpty();
    assertThat(actual.size()).isEqualTo(expected.size());
  }

  private class TestablePullRequestCommentingEventHandlerBuilder
  {
    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private PullRequestPolicyEvaluationResolver mockPullRequestPolicyEvaluationResolver;

    private boolean scmEnabled = true;

    private final String org = "testOrg";

    private final String repo = "testRepo";

    private final String username = null;

    private final String token = "testToken";

    private SourceControlProvider provider = SourceControlProvider.GITHUB;

    private final String baseBranch = "master";

    private final boolean remediationPullRequestsEnabled = true;

    private final boolean manualPullRequestsEnabled = true;

    private final boolean innerSourceAutomatedUpdatesEnabled = true;

    private final boolean statusChecksEnabled = true;

    private boolean pullRequestCommentingEnabled = true;

    private final boolean sourceControlEvaluationsEnabled = true;

    private final boolean sshEnabled = true;

    private final String sourceControlScanTarget = null;

    private GitRepositoryInfo gitRepositoryInfo;

    private String repositoryUrl;

    private String sshRepositoryUrl;

    private List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationsDTOs;

    PullRequestCommentingEventHandler build() {
      MockitoAnnotations.openMocks(this);

      if (StringUtils.isEmpty(repositoryUrl)) {
        repositoryUrl = String.format("http://%s.com/%s/%s", provider.toString(), org, repo);
      }

      doReturn(scmEnabled).when(mockSourceControlUtils).isScmEnabled(any(String.class));
      doReturn(scmEnabled).when(mockSourceControlUtils).isScmEnabled(any(GitRepositoryInfo.class));
      doReturn(repositoryUrl.contains("bitbucket.org"))
          .when(mockSourceControlUtils)
          .isBitbucketCloud(any(GitRepositoryInfo.class));

      gitRepositoryInfo = new GitRepositoryInfo(repositoryUrl, sshRepositoryUrl, username, token, provider, baseBranch,
          remediationPullRequestsEnabled, manualPullRequestsEnabled, innerSourceAutomatedUpdatesEnabled,
          statusChecksEnabled, pullRequestCommentingEnabled,
          sourceControlEvaluationsEnabled, sshEnabled, sourceControlScanTarget);
      doReturn(gitRepositoryInfo).when(mockSourceControlUtils).getGitRepositoryInfoForApplication(any());

      doReturn(null != pullRequestPolicyEvaluationsDTOs ? pullRequestPolicyEvaluationsDTOs : new ArrayList<>())
          .when(mockPullRequestPolicyEvaluationResolver)
          .resolveForPolicyEvaluation(any(), any(), any(), any());

      doReturn(CollectionUtils.isNotEmpty(pullRequestPolicyEvaluationsDTOs)
          ? pullRequestPolicyEvaluationsDTOs.get(0)
          : null)
              .when(mockPullRequestPolicyEvaluationResolver)
              .resolveForPullRequest(any(), any(), anyInt(), any(), any(), any(), any());

      return new PullRequestCommentingEventHandler(
          mockPullRequestCommentingService,
          mockSourceControlUtils,
          mockSourceControlEventPublisher,
          mockAsyncEventBus,
          licenseChecker,
          mockPullRequestPolicyEvaluationResolver,
          mockPolicyEvaluationDAO,
          mockPullRequestStatusService,
          new PullRequestCommentingEligibilityValidator(),
          mockGitCommitHistoryService);
    }

    TestablePullRequestCommentingEventHandlerBuilder withPullRequestPolicyEvaluationDTOs(
        List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationDTOs)
    {
      this.pullRequestPolicyEvaluationsDTOs = pullRequestPolicyEvaluationDTOs;
      return this;
    }

    TestablePullRequestCommentingEventHandlerBuilder withScmEnabled(boolean scmEnabled) {
      this.scmEnabled = scmEnabled;
      return this;
    }

    TestablePullRequestCommentingEventHandlerBuilder withRepositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    TestablePullRequestCommentingEventHandlerBuilder withSourceControlProvider(SourceControlProvider provider) {
      this.provider = provider;
      return this;
    }

    TestablePullRequestCommentingEventHandlerBuilder withCommentingFeatureEnabled(boolean isEnabled) {
      this.pullRequestCommentingEnabled = isEnabled;
      return this;
    }
  }
}
