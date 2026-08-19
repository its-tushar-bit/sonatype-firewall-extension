/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.git.helper.ApplicationEvaluationEventBuilder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClient.StateType;
import com.sonatype.nexus.scm.api.model.ProjectUrl;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;
import com.sonatype.nexus.scm.gitlab.dto.GitlabProjectUrl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class GitCommitStatusServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private GitApiClient mockGitApiClient;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private AsyncEventBus mockAsyncEventBus;

  private TestProductLicense testProductLicense;

  private IqForScmLicenseChecker licenseChecker;

  public GitCommitStatusServiceTest() {
    super(GitCommitStatusService.class);
  }

  @BeforeEach
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
  public void testOnApplicationEvaluation_unlicensed() throws Exception {
    // given : status service, an event and a license without notifications
    testProductLicense.setMissingFeatures(LicensedFeature.NOTIFICATIONS);
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .withCommitHash("commit456")
        .build();

    // when : process event
    commitStatusService.onApplicationEvaluation(event);

    // then : no source control event created
    verifyNoSourceControlEventCreated();
    assertThatLogMessagesEqual(
        debug("License does not support source control notification feature"));
  }

  @Test
  public void testOnApplicationEvaluation_missingCommitHash() throws Exception {
    // given : status service, an event and a license without notifications
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withPolicyEvaluationId("pe1")
        .build();

    // when : process event
    commitStatusService.onApplicationEvaluation(event);

    // then : no source control event created
    verifyNoSourceControlEventCreated();
  }

  @Test
  public void testOnApplicationEvaluation_missingRepositoryInfo() throws Exception {
    // given : status service, an event and a license without notifications
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .skipRepositoryInfo()
        .build();

    ApplicationEvaluationEvent event = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app1")
        .withCommitHash("abc123")
        .withPolicyEvaluationId("pe1")
        .build();

    // when : process event
    commitStatusService.onApplicationEvaluation(event);

    // then : no source control event created
    verifyNoSourceControlEventCreated();
    assertThatLogMessagesEqual(
        debug("The git repository information could not be found for application with id app1. " +
            "scm status could not be created."));
  }

  @Test
  public void testOnApplicationEvaluation_sourceControlEventCreated() throws Exception {
    // given: a properly configures status service and a valid app eval event
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .build();

    ApplicationEvaluationEvent appEvalEvent = new ApplicationEvaluationEventBuilder()
        .withApplicationId("app-1")
        .withCommitHash("commit-1")
        .withPolicyEvaluationId("eval-1")
        .withComponentCounts(5, 3, 1)
        .withScanId("scan-1")
        .forBuildStage()
        .withSuccessOutcome()
        .build();

    // when: process event
    commitStatusService.onApplicationEvaluation(appEvalEvent);

    // then: a source control event was created
    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventPublisher).publishEvent(eventCaptor.capture());
    SourceControlEvent generatedEvent = eventCaptor.getValue();

    assertThat(generatedEvent.getEventType()).isEqualTo(SourceControlEvent.STATUS_UPDATE_EVENT);
    assertThat(generatedEvent.getEventPriority()).isEqualTo(SourceControlEvent.EVENT_PRIORITY_HIGHER);
    assertThat(generatedEvent.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
    assertThat(generatedEvent.getApplicationId()).isEqualTo(appEvalEvent.ownerId);
    assertThat(generatedEvent.getCommitHash()).isEqualTo(appEvalEvent.commitHash);
    assertThat(generatedEvent.getPolicyEvaluationId()).isEqualTo(appEvalEvent.policyEvaluationId);
    assertThat(generatedEvent.getCriticalComponentCount()).isEqualTo(appEvalEvent.criticalComponentCount);
    assertThat(generatedEvent.getSevereComponentCount()).isEqualTo(appEvalEvent.severeComponentCount);
    assertThat(generatedEvent.getModerateComponentCount()).isEqualTo(appEvalEvent.moderateComponentCount);
    assertThat(generatedEvent.getScanId()).isEqualTo(appEvalEvent.reportId);
    assertThat(generatedEvent.getStageTypeId()).isEqualTo(appEvalEvent.stageTypeId);
    assertThat(generatedEvent.getPolicyEvaluationOutcome()).isEqualTo(ApplicationEvaluationEvent.ACTION_ID_NONE);
  }

  @Test
  public void testOnSendCommitStatus_success() throws Exception {
    // given: a properly configures status service and a valid source control event
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-2", "two")
        .expectRequestState(TestableGitCommitStatusServiceBuilder.SUCCESS_STATE)
        .build();

    SourceControlEvent event = createSourceControlEvent(2, ApplicationEvaluationEvent.ACTION_ID_NONE, 7, 4, 2);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verifyStatusRequest(
        "yes",
        "Components: Critical: 7, Severe: 4, Moderate: 2",
        "http://localhost:8070/ui/links/application/two/report/scan-2?source=gitlab");
    verify(mockGitApiClient).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("Creating a gitlab commit status for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-2, with outcome: none, state: yes"),
        info("Commit status sent for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-2, evaluation outcome: none, state: yes, response: status message"));
  }

  @Test
  public void testOnSendCommitStatus_warning() throws Exception {
    // given: a properly configures status service and a valid source control event
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-3", "three")
        .expectRequestState(TestableGitCommitStatusServiceBuilder.SUCCESS_STATE)
        .build();

    SourceControlEvent event = createSourceControlEvent(3, Action.ID_WARN, 9, 1, 4);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verifyStatusRequest(
        "yes",
        "Components: Critical: 9, Severe: 1, Moderate: 4",
        "http://localhost:8070/ui/links/application/three/report/scan-3?source=gitlab");
    verify(mockGitApiClient).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("Creating a gitlab commit status for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-3, with outcome: warn, state: yes"),
        info("Commit status sent for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-3, evaluation outcome: warn, state: yes, response: status message"));
  }

  @Test
  public void testOnSendCommitStatus_failure() throws Exception {
    // given: a properly configures status service and a valid source control event indicating policy eval failure
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-4", "four")
        .expectRequestState(TestableGitCommitStatusServiceBuilder.FAIL_STATE)
        .build();

    SourceControlEvent event = createSourceControlEvent(4, Action.ID_FAIL, 0, 2, 8);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verifyStatusRequest(
        "no",
        "Components: Critical: 0, Severe: 2, Moderate: 8",
        "http://localhost:8070/ui/links/application/four/report/scan-4?source=gitlab");
    verify(mockGitApiClient).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("Creating a gitlab commit status for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-4, with outcome: fail, state: no"),
        info("Commit status sent for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-4, evaluation outcome: fail, state: no, response: status message"));
  }

  @Test
  public void testOnSendCommitStatus_sourceControlRecordNotFound() throws Exception {
    // given: a properly configures status service, a valid source control event and source control utils setup to
    // return null for git repo info
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-5", "five")
        .skipRepositoryInfo()
        .build();

    SourceControlEvent event = createSourceControlEvent(5, Action.ID_WARN, 3, 4, 5);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verify(mockGitApiClient, never()).createStatusRequest(any(), any(), any(), any());
    verify(mockGitApiClient, never()).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("The git repository information could not be found for application with id app-5, scm status could not" +
            " be created."));
  }

  @Test
  public void testOnSendCommitStatus_noToken() throws Exception {
    // given: a properly configures status service, a valid source control event and git repo info setup with
    // null for token
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-7", "six")
        .withNoToken()
        .build();

    SourceControlEvent event = createSourceControlEvent(7, Action.ID_WARN, 3, 4, 5);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verify(mockGitApiClient, never()).createStatusRequest(any(), any(), any(), any());
    verify(mockGitApiClient, never()).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("The git repository information could not be found for application with id app-7," +
            " scm status could not be created."));
  }

  @Test
  public void testOnSendCommitStatus_noProvider() throws Exception {
    // given: a properly configures status service, a valid source control event and git repo info setup with
    // null for provider
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-6", "six")
        .withNoProvider()
        .build();

    SourceControlEvent event = createSourceControlEvent(6, Action.ID_WARN, 3, 4, 5);

    // when: process event
    commitStatusService.onSendCommitStatus(event);

    // then:
    verify(mockGitApiClient, never()).createStatusRequest(any(), any(), any(), any());
    verify(mockGitApiClient, never()).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("The git repository information could not be found for application with id app-6, scm status could not" +
            " be created."));
  }

  @Test
  public void testOnSendCommitStatus_apiClientException() throws Exception {
    // given: a properly configures status service and a valid source control event
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-8", "eight")
        .expectRequestState(TestableGitCommitStatusServiceBuilder.SUCCESS_STATE)
        .throwOnCreateStatus(new IOException("gitlab API error"))
        .build();

    SourceControlEvent event = createSourceControlEvent(8, ApplicationEvaluationEvent.ACTION_ID_NONE, 8, 9, 10);

    // expect:
    assertThatExceptionOfType(SourceControlException.class)
        .isThrownBy(() -> commitStatusService.onSendCommitStatus(event))
        .withMessage(
            "Failed to update status for applicationId: app-8, repository: http://gitlab.com/testOrg/testRepo," +
                " commitHash: commit-8, triggered by policyEvaluationId: eval-8, reason: gitlab API error");

    // and:
    verifyStatusRequest(
        "yes",
        "Components: Critical: 8, Severe: 9, Moderate: 10",
        "http://localhost:8070/ui/links/application/eight/report/scan-8?source=gitlab");
    verify(mockGitApiClient).createStatus(eq(event.getCommitHash()), any());

    assertThatLogMessagesEqual(
        debug("Creating a gitlab commit status for repository: https://gitlab.com/sonatype/testing/testRepo1/," +
            " commit hash: commit-8, with outcome: none, state: yes"));
  }

  @Test
  public void testAsyncEventBusInteractions() throws Exception {
    // given:
    GitCommitStatusService commitStatusService = Mockito.spy(new TestableGitCommitStatusServiceBuilder()
        .forApplication("app-9", "nine")
        .build());

    // when:
    commitStatusService.start();

    // then:
    verify(commitStatusService, times(1)).start();
    verify(mockAsyncEventBus, times(1)).register(eq(commitStatusService));

    // and when:
    commitStatusService.stop();

    // then:
    verify(mockAsyncEventBus, times(1)).unregister(eq(commitStatusService));
    verify(commitStatusService, times(1)).stop();
    verifyNoMoreInteractions(commitStatusService, mockAsyncEventBus);
  }

  @Test
  public void testOnApplicationEvaluation_CommitStatusEnabled_False() throws Exception {
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .withCommitStatusEnabled(false)
        .build();
    ApplicationEvaluationEvent event =
        new ApplicationEvaluationEventBuilder().withApplicationId("app1").withCommitHash("commit456").build();

    commitStatusService.onApplicationEvaluation(event);

    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    assertThatLogMessagesEqual(
        debug("Source control commit status notification feature is disabled"));
  }

  @Test
  public void testOnApplicationEvaluation_CommitStatusEnabled_True() throws Exception {
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .withCommitStatusEnabled(true)
        .build();
    ApplicationEvaluationEvent event =
        new ApplicationEvaluationEventBuilder().withApplicationId("app1").withCommitHash("commit456").build();

    commitStatusService.onApplicationEvaluation(event);

    verify(mockSourceControlEventPublisher).publishEvent(any());
  }

  @Test
  public void testOnApplicationEvaluation_CommitStatusEnabled_Null() throws Exception {
    GitCommitStatusService commitStatusService = new TestableGitCommitStatusServiceBuilder()
        .withCommitStatusEnabled(null)
        .build();
    ApplicationEvaluationEvent event =
        new ApplicationEvaluationEventBuilder().withApplicationId("app1").withCommitHash("commit456").build();

    commitStatusService.onApplicationEvaluation(event);

    verify(mockSourceControlEventPublisher).publishEvent(any());
  }

  private void verifyStatusRequest(String expectedState, String expectedMessage, String expectedUrl) {
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockGitApiClient)
        .createStatusRequest(stateCaptor.capture(), any(), messageCaptor.capture(), urlCaptor.capture());
    assertThat(stateCaptor.getValue()).isEqualTo(expectedState);
    assertThat(messageCaptor.getValue()).isEqualTo(expectedMessage);
    assertThat(urlCaptor.getValue()).isEqualTo(expectedUrl);
  }

  private SourceControlEvent createSourceControlEvent(
      int suffix,
      String outcome,
      int critical,
      int severe,
      int moderate)
  {
    return new SourceControlEvent()
        .setApplicationId("app-" + suffix)
        .setEventType(SourceControlEvent.STATUS_UPDATE_EVENT)
        .setEventPriority(SourceControlEvent.EVENT_PRIORITY_HIGHER)
        .setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS)
        .setCommitHash("commit-" + suffix)
        .setPolicyEvaluationId("eval-" + suffix)
        .setPolicyEvaluationOutcome(outcome)
        .setScanId("scan-" + suffix)
        .setStageTypeId(StageTypes.RELEASE.getId())
        .withComponentCounts(critical, severe, moderate);
  }

  private void verifyNoSourceControlEventCreated() {
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  private class TestableGitCommitStatusServiceBuilder
  {
    static final String SUCCESS_STATE = "yes";

    static final String FAIL_STATE = "no";

    @Mock
    private ApiSourceControlService mockApiSourceControlService;

    @Mock
    private SourceControlUtils mockSourceControlUtils;

    @Mock
    private BaseUrl mockBaseUrl;

    @Mock
    private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

    @Mock
    private ApplicationDAO mockApplicationDAO;

    @Mock
    private GitClientFactory mockGitClientFactory;

    @Mock
    StatusRequest mockStatusRequest;

    @Mock
    Status mockStatus;

    private GitRepositoryInfo gitRepositoryInfo;

    private SourceControlProvider provider = SourceControlProvider.GITLAB;

    private final String username = "username";

    private String token = "testToken";

    private final String org = "testOrg";

    private final String repo = "testRepo";

    private final String baseBranch = "master";

    private final boolean remediationPullRequestsEnabled = true;

    private final boolean manualPullRequestsEnabled = true;

    private final boolean innerSourceAutomatedUpdatesEnabled = true;

    private final boolean statusChecksEnabled = true;

    private final boolean sshEnabled = false;

    private boolean skipRepositoryInfo = false;

    private final String statusMessage = "status message";

    private final String baseUrl = "http://localhost:8070";

    private final boolean pullRequestCommentingEnabled = true;

    private final boolean sourceControlEvaluationsEnabled = true;

    private final String sourceControlScanTarget = null;

    private String expectedRequestState = SUCCESS_STATE;

    private String applicationId;

    private String applicationPublicId;

    private Boolean commitStatusEnabled;

    private Exception apiClientThrowsException = null;

    TestableGitCommitStatusServiceBuilder forApplication(String applicationId, String publicId) {
      this.applicationId = applicationId;
      this.applicationPublicId = publicId;
      return this;
    }

    TestableGitCommitStatusServiceBuilder skipRepositoryInfo() {
      skipRepositoryInfo = true;
      return this;
    }

    TestableGitCommitStatusServiceBuilder expectRequestState(String requestState) {
      expectedRequestState = requestState;
      return this;
    }

    TestableGitCommitStatusServiceBuilder withNoProvider() {
      provider = null;
      return this;
    }

    TestableGitCommitStatusServiceBuilder withNoToken() {
      token = null;
      return this;
    }

    TestableGitCommitStatusServiceBuilder throwOnCreateStatus(Exception e) {
      apiClientThrowsException = e;
      return this;
    }

    TestableGitCommitStatusServiceBuilder withCommitStatusEnabled(Boolean commitStatusEnabled) {
      this.commitStatusEnabled = commitStatusEnabled;
      return this;
    }

    GitCommitStatusService build() throws IOException {
      MockitoAnnotations.openMocks(this);

      if (!skipRepositoryInfo) {
        String repositoryUrl = format("http://%s.com/%s/%s", null != provider ? provider.toString() : null, org, repo);
        gitRepositoryInfo = new GitRepositoryInfo(repositoryUrl, null, username, token, provider, baseBranch,
            remediationPullRequestsEnabled, manualPullRequestsEnabled, innerSourceAutomatedUpdatesEnabled,
            statusChecksEnabled,
            pullRequestCommentingEnabled,
            sourceControlEvaluationsEnabled, sshEnabled, sourceControlScanTarget);
        doReturn(gitRepositoryInfo).when(mockSourceControlUtils).getGitRepositoryInfoForApplication(any(), any());
        doReturn(gitRepositoryInfo).when(mockSourceControlUtils).getGitRepositoryInfoForApplication(any());
      }

      SourceControl sourceControl = new SourceControl();
      sourceControl.setCommitStatusEnabled(commitStatusEnabled);
      doReturn(sourceControl).when(mockApiSourceControlService).getCompositeSourceControlByOwnerDecrypted(any());

      doReturn(mockGitApiClient).when(mockGitClientFactory).createApiClient(gitRepositoryInfo);
      doReturn(mockStatusRequest).when(mockGitApiClient).createStatusRequest(any(), any(), any(), any());

      doReturn(expectedRequestState).when(mockStatusRequest).getState();

      doReturn(SUCCESS_STATE).when(mockGitApiClient).getState(StateType.SUCCESS);
      doReturn(FAIL_STATE).when(mockGitApiClient).getState(StateType.FAILURE);

      if (null != apiClientThrowsException) {
        doThrow(apiClientThrowsException).when(mockGitApiClient).createStatus(any(), eq(mockStatusRequest));
      }
      else {
        doReturn(mockStatus).when(mockGitApiClient).createStatus(any(), eq(mockStatusRequest));
      }

      doReturn(statusMessage).when(mockStatus).toString();

      Application application = new Application();
      application.setId(applicationId);
      application.setPublicId(applicationPublicId);
      doReturn(application).when(mockApplicationDAO).getByIdNotNull(eq(applicationId));

      doReturn(baseUrl).when(mockBaseUrl).get();

      ProjectUrl projectUrl = new GitlabProjectUrl("https://gitlab.com/sonatype/testing/testRepo1");
      doReturn(projectUrl).when(mockGitApiClient).getProjectUrl();

      ScmStatusHelper scmStatusHelper =
          new ScmStatusHelper(mockApplicationDAO, mockBaseUrl, developmentPrioritiesUtilsService);

      return new GitCommitStatusService(
          mockSourceControlUtils,
          mockGitClientFactory,
          licenseChecker,
          mockSourceControlEventPublisher,
          mockAsyncEventBus,
          scmStatusHelper,
          mockApiSourceControlService);
    }
  }
}
