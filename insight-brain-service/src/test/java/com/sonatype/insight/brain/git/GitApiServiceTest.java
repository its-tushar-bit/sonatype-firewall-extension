/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ProjectUri;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;
import com.sonatype.nexus.scm.api.model.User;
import com.sonatype.nexus.scm.github.dto.GithubStatus;
import com.sonatype.nexus.scm.github.dto.GithubStatusRequest;
import com.sonatype.nexus.scm.github.dto.GithubUser;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitApiServiceTest
    extends AbstractComponentTest
{
  private static final String TOKEN = "token";

  private static final String VALID_URL = "https://example.com/organization/project";

  @Inject
  private GitApiService gitApiService;

  private ApiSourceControlService mockSourceControlService;

  private ApplicationDAO mockApplicationDAO;

  private GitClientFactory mockGitClientFactory;

  private BaseUrl mockBaseUrl;

  private Application application;

  private SourceControl sourceControl;

  private ApplicationEvaluationEvent event;

  private GitApiClient mockGitApiClient = mock(GitApiClient.class);

  private Status status;

  private Organization org;

  private GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  @Override
  public void configure(Binder binder) {
    mockSourceControlService = mock(ApiSourceControlService.class);
    binder.bind(ApiSourceControlService.class).toInstance(mockSourceControlService);
    mockApplicationDAO = mock(ApplicationDAO.class);
    binder.bind(ApplicationDAO.class).toInstance(mockApplicationDAO);
    mockGitClientFactory = mock(GitClientFactory.class);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    mockBaseUrl = mock(BaseUrl.class);
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }

  @Before
  public void setup() {
    application = tempEntity.newApplicationWithParent("app", "appId", "orgId");
    User creator = new GithubUser();
    creator.setUsername("foo");
    status = new GithubStatus();
    status.setTargetUrl("http://example.com");
    status.setUser(creator);
    org = tempEntity.newOrganization();
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_WithCommitHash_Success() throws IOException {
    String policyEvaluationOutcome = ApplicationEvaluationEvent.ACTION_ID_NONE;
    String gitHubCommitStatus = "success";
    assertApplicationEvaluationOutcome(policyEvaluationOutcome, gitHubCommitStatus);
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_WithCommitHash_Failure() throws IOException {
    String policyEvaluationOutcome = Action.ID_FAIL;
    String gitHubCommitStatus = "failure";
    assertApplicationEvaluationOutcome(policyEvaluationOutcome, gitHubCommitStatus);
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_WithCommitHash_Warning() throws IOException {
    String policyEvaluationOutcome = Action.ID_WARN;
    String gitHubCommitStatus = "pending";
    assertApplicationEvaluationOutcome(policyEvaluationOutcome, gitHubCommitStatus);
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_WithoutCommitHash()
      throws IOException
  {
    event = getApplicationEvaluationEvent("foo", null, null, 0, 0, 0, 0, null);
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_SourceControlRecordNotFound() throws IOException {
    event = getApplicationEvaluationEvent("INVALID_ID", "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_NoToken() throws IOException {
    setupApplicationSourceControlWithoutToken();
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getId());

    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_NoProvider() throws IOException {
    setupApplicationSourceControlWithoutProvider();
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getId());

    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testGetGitRepositoryInfo_ProviderAndTokenFromApplication() {
    SourceControl sourceControl =
        new SourceControl(application.getParentOwnerId(), VALID_URL, TOKEN, SourceControlProvider.GITHUB);
    sourceControl.setBaseBranch("base-branch");
    sourceControl.setEnablePullRequests(true);
    sourceControl.setEnableStatusChecks(true);
    when(
        mockSourceControlService
            .getSourceControlByOwnerDecrypted(eq(OwnerType.APPLICATION), eq(application.getId())))
        .thenReturn(sourceControl);

    GitRepositoryInfo value = gitApiService.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value).isNotNull();
    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    verify(mockSourceControlService, never())
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getOrganizationId());
    verify(mockSourceControlService, never())
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetGitRepositoryInfo_ProviderAndTokenFromOrganization() {
    SourceControl sourceControl =
        new SourceControl(application.getId(), VALID_URL, null, null);
    sourceControl.setOwnerId(application.getOrganizationId());
    sourceControl.setBaseBranch("base-branch");
    sourceControl.setEnablePullRequests(true);
    sourceControl.setEnableStatusChecks(true);
    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(OwnerType.APPLICATION), eq(application.getId())))
        .thenReturn(sourceControl);

    SourceControl orgSourceControl =
        new SourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    when(mockSourceControlService
        .getSourceControlByOwnerDecrypted(eq(OwnerType.ORGANIZATION), eq(application.getOrganizationId())))
        .thenReturn(orgSourceControl);

    GitRepositoryInfo value = gitApiService.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value).isNotNull();
    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    verify(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getOrganizationId());
    verify(mockSourceControlService, never())
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, org.getParentOrganizationId());
  }

  @Test
  public void testGetGitRepositoryInfo_ProviderAndTokenFromRootOrganization() {
    SourceControl sourceControl =
        new SourceControl(application.getId(), VALID_URL, null, null);
    sourceControl.setOwnerId(application.getOrganizationId());
    sourceControl.setBaseBranch("base-branch");
    sourceControl.setEnablePullRequests(true);
    sourceControl.setEnableStatusChecks(true);
    when(mockSourceControlService.getSourceControlByOwnerDecrypted(eq(OwnerType.APPLICATION), eq(application.getId())))
        .thenReturn(sourceControl);

    // no org source control, only one at the root level
    when(mockSourceControlService
        .getSourceControlByOwnerDecrypted(eq(OwnerType.ORGANIZATION), eq(application.getOrganizationId())))
        .thenReturn(null);

    SourceControl rootOrgSourceControl =
        new SourceControl(org.getParentOrganizationId(), null, TOKEN, SourceControlProvider.GITHUB);
    when(mockSourceControlService
        .getSourceControlByOwnerDecrypted(eq(OwnerType.ORGANIZATION), eq(Organization.ROOT_ORGANIZATION_ID)))
        .thenReturn(rootOrgSourceControl);

    GitRepositoryInfo value = gitApiService.getGitRepositoryInfoForApplication(application.getId());

    assertThat(value.token).isEqualTo(TOKEN);
    assertThat(value.provider).isEqualTo(SourceControlProvider.GITHUB);
    verify(mockSourceControlService).getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    verify(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, application.getOrganizationId());
    verify(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, org.getParentOrganizationId());
  }

  @Test
  public void testGetGitRepositoryInfo_NoApplicationSourceControl() {
    GitRepositoryInfo value = gitApiService.getGitRepositoryInfoForApplication("INVALID");
    assertThat(value).isNull();
  }

  private void assertApplicationEvaluationOutcome(
      final String policyEvaluationOutcome,
      final String gitHubCommitStatus)
      throws IOException
  {
    ProjectUri projectUri = setupPolicyEvaluation(policyEvaluationOutcome);
    StatusRequest statusRequest = createStatusRequest(gitHubCommitStatus);
    doReturn(mockGitApiClient).when(mockGitClientFactory)
        .create(any());
    doReturn(projectUri).when(mockGitApiClient).getProjectUri();
    doReturn(statusRequest).when(mockGitApiClient).createStatusRequest(any(), any(), any(), any());
    doReturn("http://localhost:8070/").when(mockBaseUrl).get();
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
    when(mockGitApiClient.createStatus(any(), any())).thenReturn(status);

    doReturn(application).when(mockApplicationDAO).getByIdNotNull(application.getId());

    gitApiService.maybeRespond(event);

    assertGitHubStatusMessage(event, "commitHash", gitHubCommitStatus, mockGitApiClient);
  }

  private StatusRequest createStatusRequest(final String gitHubCommitStatus) {
    StatusRequest statusRequest = new GithubStatusRequest();
    statusRequest.setState(gitHubCommitStatus);
    statusRequest.setContext("IQ Policy Evaluation");
    statusRequest.setDescription(String
        .format("Components: Critical: %d, Severe: %d, Moderate: %d", event.criticalComponentCount,
            event.severeComponentCount, event.moderateComponentCount));
    statusRequest.setTargetUrl("http://localhost:8070/ui/links/application/app/report/scanId?source=github");
    return statusRequest;
  }

  private ProjectUri setupPolicyEvaluation(final String policyEvaluationOutcome) {
    int affectedComponentsCount = 22;
    int criticalComponentsCount = 12;
    int severeComponentsCount = 6;
    int moderateComponentsCount = 3;

    ProjectUri projectUri = gitApiClientFactory
        .getGitApiClientUtils(com.sonatype.nexus.scm.SourceControlProvider.GITHUB)
        .createProjectUri("https://github.com/owner/repo/");
    sourceControl = new SourceControl(application.getId(), projectUri.getUrl(), TOKEN, SourceControlProvider.GITHUB);
    sourceControl.setEnableStatusChecks(true);
    sourceControl.setEnablePullRequests(true);
    sourceControl.setBaseBranch("base-branch");
    event = getApplicationEvaluationEvent(application.getId(), "release",
        policyEvaluationOutcome, affectedComponentsCount,
        criticalComponentsCount, severeComponentsCount, moderateComponentsCount, "commitHash");
    return projectUri;
  }

  private void assertGitHubStatusMessage(
      final ApplicationEvaluationEvent event,
      final String commitHash, final String status, final GitApiClient mockGitApiClient) throws IOException
  {
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<StatusRequest> statusRequestCaptor = ArgumentCaptor.forClass(StatusRequest.class);
    verify(mockGitApiClient).createStatus(stringCaptor.capture(), statusRequestCaptor.capture());

    List<String> stringArguments = stringCaptor.getAllValues();
    assertThat(stringArguments.get(0)).isEqualTo(commitHash);

    StatusRequest actualStatusRequest = statusRequestCaptor.getValue();
    assertThat(actualStatusRequest.getContext()).isEqualTo("IQ Policy Evaluation");
    assertThat(actualStatusRequest.getDescription()).isEqualTo(String
        .format("Components: Critical: %d, Severe: %d, Moderate: %d", event.criticalComponentCount,
            event.severeComponentCount, event.moderateComponentCount));
    assertThat(actualStatusRequest.getState()).isEqualTo(status);
    assertThat(actualStatusRequest.getTargetUrl())
        .isEqualTo("http://localhost:8070/ui/links/application/app/report/scanId?source=github");
    verify(mockSourceControlService)
        .getSourceControlByOwnerDecrypted(OwnerType.APPLICATION, application.getId());
  }

  private ApplicationEvaluationEvent getApplicationEvaluationEvent(
      final String appId,
      final String stageTypeId,
      final String outcome,
      final int affectedComponentCount,
      final int criticalComponentCount,
      final int severeComponentCount,
      final int moderateComponentCount,
      final String commitHash)
  {
    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    event.outcome = outcome;
    event.ownerId = appId;
    event.policyEvaluationId = "policyEvaluationId";
    event.stageTypeId = stageTypeId;
    event.affectedComponentCount = affectedComponentCount;
    event.criticalComponentCount = criticalComponentCount;
    event.severeComponentCount = severeComponentCount;
    event.moderateComponentCount = moderateComponentCount;
    event.commitHash = commitHash;
    event.reportId = "scanId";
    return event;
  }

  private void setupApplicationSourceControlWithoutToken() {
    ProjectUri projectUri = gitApiClientFactory
        .getGitApiClientUtils(com.sonatype.nexus.scm.SourceControlProvider.GITHUB)
        .createProjectUri("https://github.com/owner/repo/");

    sourceControl = new SourceControl(application.getId(), projectUri.getUrl(), null, SourceControlProvider.GITHUB);
  }

  private void setupApplicationSourceControlWithoutProvider() {
    ProjectUri projectUri = gitApiClientFactory
        .getGitApiClientUtils(com.sonatype.nexus.scm.SourceControlProvider.GITHUB)
        .createProjectUri("https://github.com/owner/repo/");

    sourceControl = new SourceControl(application.getId(), projectUri.getUrl(), TOKEN, null);
  }
}
