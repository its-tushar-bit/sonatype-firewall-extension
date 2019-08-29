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
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.Application;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitApiServiceTest
    extends AbstractComponentTest
{
  private static final String TOKEN = "token";

  @Inject
  private GitApiService gitApiService;

  private ApiSourceControlService mockSourceControlService;

  private GitClientFactory mockGitClientFactory;

  private BaseUrl mockBaseUrl;

  private Application application;

  private ApiSourceControlDTO sourceControl;

  private ApplicationEvaluationEvent event;

  private GitApiClient mockGitApiClient = mock(GitApiClient.class);

  private Status status;

  private ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();

  private GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  @Override
  public void configure(Binder binder) {
    mockSourceControlService = mock(ApiSourceControlService.class);
    binder.bind(ApiSourceControlService.class).toInstance(mockSourceControlService);
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
    doReturn(null).when(mockSourceControlService).getSourceControlByApplicationIdDecrypted(application.getId());
    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_NoToken() throws IOException {

    setupApplicationSourceControlWithoutToken();
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByApplicationIdDecrypted(application.getId());
    doReturn(sourceControl).when(mockSourceControlService)
        .populateProviderAndTokenFromOrganizationIfNeeded(sourceControl);
    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_NoProvider() throws IOException {

    setupApplicationSourceControlWithoutProvider();
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByApplicationIdDecrypted(application.getId());
    doReturn(sourceControl).when(mockSourceControlService)
        .populateProviderAndTokenFromOrganizationIfNeeded(sourceControl);
    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitApiService.maybeRespond(event);

    verify(mockGitApiClient, never()).createStatus(any(), any());
  }

  private void assertApplicationEvaluationOutcome(final String policyEvaluationOutcome,
                                                  final String gitHubCommitStatus)
      throws IOException
  {
    ProjectUri projectUri = setupPolicyEvaluation(policyEvaluationOutcome);
    StatusRequest statusRequest = createStatusRequest(gitHubCommitStatus);
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByApplicationIdDecrypted(application.getId());
    doReturn(mockGitApiClient).when(mockGitClientFactory).create(sourceControl);
    doReturn(projectUri).when(mockGitApiClient).getProjectUri();
    doReturn(statusRequest).when(mockGitApiClient).createStatusRequest(any(), any(), any(), any());
    doReturn("http://localhost:8070/").when(mockBaseUrl).get();
    doReturn(sourceControl).when(mockSourceControlService)
        .populateProviderAndTokenFromOrganizationIfNeeded(sourceControl);
    when(mockGitApiClient.createStatus(any(), any())).thenReturn(status);
    
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
    sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(application.getId(), projectUri.getUrl(), TOKEN, SourceControlProvider.GITHUB));
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
    verify(mockSourceControlService).populateProviderAndTokenFromOrganizationIfNeeded(sourceControl);
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

    sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(application.getId(), projectUri.getUrl(), null,
            SourceControlProvider.GITHUB));
  }

  private void setupApplicationSourceControlWithoutProvider() {
    ProjectUri projectUri = gitApiClientFactory
        .getGitApiClientUtils(com.sonatype.nexus.scm.SourceControlProvider.GITHUB)
        .createProjectUri("https://github.com/owner/repo/");

    sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(application.getId(), projectUri.getUrl(), TOKEN,
            null));
  }
}
