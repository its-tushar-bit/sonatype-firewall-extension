/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import java.io.IOException;
import jakarta.inject.Provider;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PullRequestRemediationServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestExecutor mockPullRequestExecutor;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private GitApiClient mockGitApiClient;

  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private PullRequestTask mockPullRequestTask;

  @Mock
  private Provider<PullRequestTask> mockPullRequestTaskProvider;

  @Mock
  private SourceControlSshService mockSourceControlSshService;

  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private InnerSourceApplicationDAO mockInnerSourceApplicationDAO;

  @Mock
  private ScmReducedSecurityService mockScmReducedSecurityService;

  @Mock
  private TelemetrySender mockTelemetrySender;

  @Mock
  private TelemetryUtils mockTelemetryUtils;

  // subject
  private PullRequestRemediationService pullRequestRemediationService;

  private OrganizationDAO organizationDAO;

  public PullRequestRemediationServiceTest() {
    super(PullRequestRemediationService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    organizationDAO = daoFactory.createOrganizationDAO();
    pullRequestRemediationService = new PullRequestRemediationService(mockPullRequestExecutor, mockGitClientFactory,
        mockApplicationDAO, organizationDAO, mockSourceControlUtils, mockTelemetryUtils, mockPullRequestTaskProvider,
        mockSourceControlSshService, mockSourceControlEventDAO, mockScmReducedSecurityService,
        mockInnerSourceApplicationDAO, mockTelemetrySender);
  }

  private Application setupApplication(String appId) {
    Application application = new Application();
    application.setId(appId);
    when(mockApplicationDAO.getById(appId)).thenReturn(application);
    return application;
  }

  private void setupGitRepositoryInfoForApp(String appId) {
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("repoUrl", "sshRepoUrl", "username", "token",
        SourceControlProvider.GITLAB, "baseBranch", true, true, true, true, true, true, false, null);

    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(appId)).thenReturn(gitRepositoryInfo);
  }

  @Test
  public void testOnRemediateComponent_success() throws Exception {
    // expect:
    final String branchName = "unique/branch";
    final String appId = "app-123-abc";
    final String toVersion = "version-Y";
    final String scanId = "scan-345";
    final String stage = Stage.ID_BUILD;
    final String prContents = "pull request details here";
    final ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("pkg-A", "version-X");
    final String prUrl = "https://gitlab.com/sonatype/test/-/merge_requests/99";

    // given: a repo branch that does not already exist
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);
    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);

    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(componentId)
        .setApplicationId(application.getId())
        .setRemediationVersion(toVersion)
        .setScanId(scanId)
        .setStageTypeId(stage)
        .setPullRequestContents(prContents)
        .setBranchName(branchName);

    // when: try to remediate a component for this same branch
    pullRequestRemediationService.onRemediateComponent(event);

    // then: make sure the remediation details used for PR creation actually came from the event
    ArgumentCaptor<PullRequestRemediationDetails> remediationDetailsCaptor =
        ArgumentCaptor.forClass(PullRequestRemediationDetails.class);
    verify(mockPullRequestTask).run(remediationDetailsCaptor.capture(), any());

    PullRequestRemediationDetails remediationDetails = remediationDetailsCaptor.getValue();

    assertThat(remediationDetails.getApp().getId()).isEqualTo(appId);
    assertThat(remediationDetails.getPullRequestBranchName()).isEqualTo(branchName);
    assertThat(remediationDetails.getRemediatedVersion()).isEqualTo(toVersion);
    assertThat(remediationDetails.getScanId()).isEqualTo(scanId);
    assertThat(remediationDetails.getStage()).isEqualTo(stage);
    assertThat(remediationDetails.getContents()).isEqualTo(prContents);
    assertThat(remediationDetails.getToBeRemediated()).isEqualTo(componentId);

    verifySshServiceInvoked(appId);
  }

  @Test
  public void testOnRemediateComponent_branchExistsOnServer() throws Exception {
    // given: a repo branch that already exists
    final String branchName = "branch/already/exists";
    setupBranchExistence(branchName, true);
    SourceControlEvent event = new SourceControlEvent().setBranchName(branchName);
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("repoUrl", "sshRepoUrl", "username", "token",
        SourceControlProvider.GITLAB, "baseBranch", true, true, true, true, true, true, false, null);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // when: try to remediate a component for this same branch
    assertThatThrownBy(() -> pullRequestRemediationService.onRemediateComponent(event))
        .isInstanceOf(SourceControlException.class)
        .hasMessage("Branch already exists on remote server for remediation: branch/already/exists");

    assertThatLogMessagesEqual(
        info("Branch already exists on remote server for remediation [branch/already/exists]")
    );

    verifyNoInteractions(mockSourceControlSshService);
  }

  @Test
  public void testIsFormatSupportedForPullRequestRemediation_notSupported() {
    // when we check format support for a format we know is not currently supported
    boolean supported =
        pullRequestRemediationService.isFormatSupportedForPullRequestRemediation(ComponentIdentifier.FORMAT_NUGET);

    // then we see that the format is not supported
    assertThat(supported).isFalse();

    verifyNoInteractions(mockSourceControlSshService);
  }

  @Test
  public void testIsFormatSupportedForPullRequestRemediation_mavenFormatSupported() {
    // given: a service object and a component with a supported format
    when(mockPullRequestExecutor.isSupportedFormat(ComponentIdentifier.FORMAT_MAVEN)).thenReturn(true);

    // when: we check format support
    boolean supported =
        pullRequestRemediationService.isFormatSupportedForPullRequestRemediation(ComponentIdentifier.FORMAT_MAVEN);

    // then we see that the format is not supported
    assertThat(supported).isTrue();
  }

  @Test
  public void testOnRemediateComponent_manualRemediationPullRequest() throws Exception {
    final String branchName = "manual/fix/branch";
    final String appId = "app-123-abc";
    final String prUrl = "https://gitlab.com/sonatype/test/-/merge_requests/99";
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);

    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);

    //create a manual remediation event
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(ComponentIdentifier.createNpmCoordinates("pkg-A", "version-X"))
        .setApplicationId(application.getId())
        .setRemediationVersion("version-Y")
        .setScanId("scan-345")
        .setStageTypeId(Stage.ID_BUILD)
        .setPullRequestContents("manual remediation contents")
        .setBranchName(branchName)
        .setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);

    pullRequestRemediationService.onRemediateComponent(event);

    //verify the isManualRemediation flag is set in the PullRequestRemediationDetails
    ArgumentCaptor<PullRequestRemediationDetails> detailsCaptor =
        ArgumentCaptor.forClass(PullRequestRemediationDetails.class);
    verify(mockPullRequestTask).run(detailsCaptor.capture(), any());

    PullRequestRemediationDetails capturedDetails = detailsCaptor.getValue();
    assertThat(capturedDetails.isManualPullRequest()).isTrue();

    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventDAO).update(eventCaptor.capture());

    SourceControlEvent capturedEvent = eventCaptor.getValue();
    assertThat(capturedEvent.getEventStatusDetails()).isEqualTo(prUrl);
  }

  @Test
  public void testOnRemediateComponent_noUpdateWhenInvalidPullRequestUrl() throws Exception {
    final String branchName = "unique/branch";
    final String appId = "app-123-abc";
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);

    PullRequestResult pullRequestResult = createPullRequestResult(false, null);

    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);

    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(ComponentIdentifier.createNpmCoordinates("pkg-A", "version-X"))
        .setApplicationId(application.getId())
        .setRemediationVersion("version-Y")
        .setScanId("scan-345")
        .setStageTypeId(Stage.ID_BUILD)
        .setPullRequestContents("pull request contents")
        .setBranchName(branchName);

    pullRequestRemediationService.onRemediateComponent(event);

    verify(mockSourceControlEventDAO, times(0)).update(event);
  }

  @Test
  public void testOnRemediatePullRequestClosing_branchExists_closesPullRequestSuccessfully() throws Exception {
    final String branchName = "existing/branch";
    final String appId = "app-123-abc";
    final int prNumber = 42;
    final String pullRequestContents = "Closing pull request due to PR being older than 20 days.";

    SourceControlEvent event = new SourceControlEvent()
        .setBranchName(branchName)
        .setApplicationId(appId)
        .setPullRequestNumber(prNumber)
        .setPullRequestContents(pullRequestContents);

    setupBranchExistence(branchName, true);
    setupGitRepositoryInfoForApp(appId);

    when(mockGitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockGitApiClient);

    pullRequestRemediationService.onRemediatePullRequestClosing(event);

    verify(mockGitApiClient).createPullRequestComment(prNumber, pullRequestContents);
    verify(mockGitApiClient).closePullRequest(prNumber);
  }

  @Test
  public void testOnRemediatePullRequestClosing_branchDoesNotExist() throws Exception {
    final String branchName = "nonexistent/branch";
    SourceControlEvent event = new SourceControlEvent().setBranchName(branchName);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(event.getApplicationId());

    assertThatThrownBy(() -> pullRequestRemediationService.onRemediatePullRequestClosing(event))
        .isInstanceOf(SourceControlException.class)
        .hasMessage("Branch does not exist on remote server for remediation closing: nonexistent/branch");

    assertThatLogMessagesEqual(
        info("Branch nonexistent/branch does not exist on remote server for remediation closing.")
    );
  }

  private void setupBranchExistence(String branchName, boolean exists) throws IOException {
    when(mockGitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isBranchOnServer(branchName)).thenReturn(exists);
  }

  @Test
  public void testOnRemediateComponent_telemetryIsSent() throws Exception {
    // expect:
    final String branchName = "telemetry/test/branch";
    final String appId = "app-123-telemetry";
    final String toVersion = "2.1.0";
    final String scanId = "scan-telemetry-123";
    final String stage = Stage.ID_BUILD;
    final String prContents = "telemetry test PR contents";
    final ComponentIdentifier componentId =
        ComponentIdentifier.createMavenCoordinates("com.test", "test-artifact", "1.0.0");
    final String purl = PackageUrlIdentifier.fromComponentIdentifier(componentId).getPackageUrl();
    final String prUrl = "https://github.com/sonatype/test/pull/123";

    // given: successful PR creation scenario
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);
    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);
    when(mockTelemetryUtils.obfuscate(appId)).thenReturn("obfuscated-" + appId);
    when(mockTelemetryUtils.convertGoldenStatusToString(true)).thenReturn("golden");

    // create automatic remediation event (manual = false by default)
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(componentId)
        .setApplicationId(application.getId())
        .setRemediationVersion(toVersion)
        .setScanId(scanId)
        .setStageTypeId(stage)
        .setPullRequestContents(prContents)
        .setBranchName(branchName)
        .setIsGoldenPullRequest(true); // set as golden PR

    // when: remediate component successfully
    pullRequestRemediationService.onRemediateComponent(event);

    // then: verify telemetry was sent with correct data
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + appId);
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo(
        PullRequestSource.AUTOMATIC.name());
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(123);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("golden");
    assertThat(telemetryData.getAttributes().get("component_package_url")).isEqualTo(purl);

    // also verify the event was updated with PR details
    verify(mockSourceControlEventDAO).update(event);
    assertThat(event.getEventStatusDetails()).isEqualTo(prUrl);
    assertThat(event.getPullRequestNumber()).isEqualTo(123);
  }

  @Test
  public void testOnRemediateComponent_manualPR_telemetryIsSentWithCorrectType() throws Exception {
    // expect:
    final String branchName = "manual/telemetry/branch";
    final String appId = "app-456-manual";
    final String toVersion = "3.0.0";
    final String scanId = "scan-manual-456";
    final String stage = Stage.ID_BUILD;
    final String prContents = "manual PR telemetry test";
    final ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("test-package", "2.0.0");
    final String purl = PackageUrlIdentifier.fromComponentIdentifier(componentId).getPackageUrl();
    final String prUrl = "https://gitlab.com/sonatype/test/-/merge_requests/456";

    // given: successful manual PR creation scenario
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);
    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);
    when(mockTelemetryUtils.obfuscate(appId)).thenReturn("obfuscated-" + appId);
    when(mockTelemetryUtils.convertGoldenStatusToString(false)).thenReturn("not_golden");

    // create manual remediation event
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(componentId)
        .setApplicationId(application.getId())
        .setRemediationVersion(toVersion)
        .setScanId(scanId)
        .setStageTypeId(stage)
        .setPullRequestContents(prContents)
        .setBranchName(branchName)
        .setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT)
        .setIsGoldenPullRequest(false); // non-golden manual PR

    // when: remediate component successfully
    pullRequestRemediationService.onRemediateComponent(event);

    // then: verify telemetry was sent with correct manual PR type
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + appId);
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo(
        PullRequestSource.MANUAL.name());
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(456);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("not_golden");
    assertThat(telemetryData.getAttributes().get("component_package_url")).isEqualTo(purl);

    // also verify the event was updated
    verify(mockSourceControlEventDAO).update(event);
    assertThat(event.getEventStatusDetails()).isEqualTo(prUrl);
    assertThat(event.getPullRequestNumber()).isEqualTo(456);
  }

  @Test
  public void testOnRemediateComponent_manualGoldenPR_telemetryIsSentWithCorrectFlags() throws Exception {
    // expect:
    final String branchName = "manual-golden/telemetry/branch";
    final String appId = "app-789-manual-golden";
    final String toVersion = "4.0.0";
    final String scanId = "scan-manual-golden-789";
    final String stage = Stage.ID_BUILD;
    final String prContents = "manual golden PR telemetry test";
    final ComponentIdentifier componentId =
        ComponentIdentifier.createMavenCoordinates("com.example", "golden-artifact", "3.0.0");
    final String purl = PackageUrlIdentifier.fromComponentIdentifier(componentId).getPackageUrl();
    final String prUrl = "https://github.com/sonatype/test/pull/789";

    // given: successful manual golden PR creation scenario
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);
    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);
    when(mockTelemetryUtils.obfuscate(appId)).thenReturn("obfuscated-" + appId);
    when(mockTelemetryUtils.convertGoldenStatusToString(true)).thenReturn("golden");

    // create manual golden remediation event
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(componentId)
        .setApplicationId(application.getId())
        .setRemediationVersion(toVersion)
        .setScanId(scanId)
        .setStageTypeId(stage)
        .setPullRequestContents(prContents)
        .setBranchName(branchName)
        .setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT)
        .setIsGoldenPullRequest(true); // golden manual PR

    // when: remediate component successfully
    pullRequestRemediationService.onRemediateComponent(event);

    // then: verify telemetry was sent with correct manual golden flags
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + appId);
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo(
        PullRequestSource.MANUAL.name());
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(789);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("golden");
    assertThat(telemetryData.getAttributes().get("component_package_url")).isEqualTo(purl);

    // also verify the event was updated
    verify(mockSourceControlEventDAO).update(event);
    assertThat(event.getEventStatusDetails()).isEqualTo(prUrl);
    assertThat(event.getPullRequestNumber()).isEqualTo(789);
  }

  @Test
  public void testOnRemediateComponent_autoNonGoldenPR_telemetryIsSentWithCorrectFlags() throws Exception {
    // expect:
    final String branchName = "auto-non-golden/telemetry/branch";
    final String appId = "app-101-auto-non-golden";
    final String toVersion = "5.0.0";
    final String scanId = "scan-auto-non-golden-101";
    final String stage = Stage.ID_BUILD;
    final String prContents = "automatic non-golden PR telemetry test";
    final ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("auto-package", "4.0.0");
    final String purl = PackageUrlIdentifier.fromComponentIdentifier(componentId).getPackageUrl();
    final String prUrl = "https://bitbucket.org/sonatype/test/pull-requests/101";

    // given: successful automatic non-golden PR creation scenario
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);
    PullRequestResult pullRequestResult = createPullRequestResult(true, prUrl);
    when(mockPullRequestTask.run(any(), any())).thenReturn(pullRequestResult);
    when(mockTelemetryUtils.obfuscate(appId)).thenReturn("obfuscated-" + appId);
    when(mockTelemetryUtils.convertGoldenStatusToString(false)).thenReturn("not_golden");

    // create automatic non-golden remediation event (automatic is default)
    SourceControlEvent event = new SourceControlEvent()
        .withComponentIdentifier(componentId)
        .setApplicationId(application.getId())
        .setRemediationVersion(toVersion)
        .setScanId(scanId)
        .setStageTypeId(stage)
        .setPullRequestContents(prContents)
        .setBranchName(branchName)
        .setIsGoldenPullRequest(false); // non-golden automatic PR

    // when: remediate component successfully
    pullRequestRemediationService.onRemediateComponent(event);

    // then: verify telemetry was sent with correct automatic non-golden flags
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryCaptor.capture());

    TelemetryData telemetryData = telemetryCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo("obfuscated-" + appId);
    assertThat(telemetryData.getAttributes().get("event_time")).isNotNull();
    assertThat(telemetryData.getAttributes().get("pull_request_creation_type")).isEqualTo(
        PullRequestSource.AUTOMATIC.name());
    assertThat(telemetryData.getAttributes().get("pull_request_number")).isEqualTo(101);
    assertThat(telemetryData.getAttributes().get("pull_request_type")).isEqualTo("not_golden");
    assertThat(telemetryData.getAttributes().get("component_package_url")).isEqualTo(purl);

    // also verify the event was updated
    verify(mockSourceControlEventDAO).update(event);
    assertThat(event.getEventStatusDetails()).isEqualTo(prUrl);
    assertThat(event.getPullRequestNumber()).isEqualTo(101);
  }

  private void verifySshServiceInvoked(String appId) {
    verify(mockSourceControlSshService, times(1)).verifySshUrlAndUpdateIfNeeded(appId);
  }

  private PullRequestResult createPullRequestResult(boolean successful, String url) {
    PullRequestResult result = new PullRequestResult();
    result.setSuccessful(successful);
    result.setPullRequestUrl(url);
    return result;
  }
}
