/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.github;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.nexus.github.GitHubApiClient;
import com.sonatype.nexus.github.model.ProjectUri;
import com.sonatype.nexus.github.model.Status;
import com.sonatype.nexus.github.model.StatusRequest;
import com.sonatype.nexus.github.model.User;

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

public class GitHubApiServiceTest
    extends AbstractComponentTest
{
  private static final String TOKEN = "token";

  @Inject
  private GitHubApiService gitHubApiService;

  private ApiSourceControlService mockSourceControlService;

  private GitHubApiClientFactory mockGitHubApiClientFactory;

  private BaseUrl mockBaseUrl;

  private Application application;

  private SourceControl sourceControl;

  private ApplicationEvaluationEvent event;

  private GitHubApiClient mockGitHubApiClient = mock(GitHubApiClient.class);

  private Status status;
  
  @Override
  public void configure(Binder binder) {
    mockSourceControlService = mock(ApiSourceControlService.class);
    binder.bind(ApiSourceControlService.class).toInstance(mockSourceControlService);
    mockGitHubApiClientFactory = mock(GitHubApiClientFactory.class);
    binder.bind(GitHubApiClientFactory.class).toInstance(mockGitHubApiClientFactory);
    mockBaseUrl = mock(BaseUrl.class);
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }
  
  @Before
  public void setup() {
    application = tempEntity.newApplicationWithParent("app", "appId", "orgId");
    User creator = new User();
    creator.login = "foo";
    status = new Status();
    status.url = "http://example.com";
    status.creator = creator;
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
    gitHubApiService.maybeRespond(event);

    verify(mockGitHubApiClient, never()).createStatus(any(), any(), any(), any());
  }

  @Test
  public void testMaybeRespondToApplicationEvaluationEvent_SourceControlRecordNotFound() throws IOException {
    doReturn(null).when(mockSourceControlService).getSourceControlByApplicationIdDecrypted(application.getId());
    event = getApplicationEvaluationEvent(application.getId(), "release", "failure", 1, 1, 0, 0, "commitHash");
    gitHubApiService.maybeRespond(event);

    verify(mockGitHubApiClient, never()).createStatus(any(), any(), any(), any());
  }

  private void assertApplicationEvaluationOutcome(final String policyEvaluationOutcome, final String gitHubCommitStatus)
      throws IOException
  {
    ProjectUri projectUri = setupPolicyEvaluation(policyEvaluationOutcome);
    doReturn(sourceControl).when(mockSourceControlService)
        .getSourceControlByApplicationIdDecrypted(application.getId());
    doReturn(mockGitHubApiClient).when(mockGitHubApiClientFactory).create(sourceControl.getRepositoryUrl(), TOKEN);
    doReturn("http://localhost:8070/").when(mockBaseUrl).get();
    when(mockGitHubApiClient.createStatus(any(), any(), any(), any())).thenReturn(status);
    
    gitHubApiService.maybeRespond(event);

    assertGitHubStatusMessage(projectUri, event, "commitHash", gitHubCommitStatus, mockGitHubApiClient);
  }

  private ProjectUri setupPolicyEvaluation(final String policyEvaluationOutcome) {
    int affectedComponentsCount = 22;
    int criticalComponentsCount = 12;
    int severeComponentsCount = 6;
    int moderateComponentsCount = 3;

    ProjectUri projectUri = new ProjectUri("https://github.com/owner/repo/");
    sourceControl = new SourceControl(application.getId(), projectUri.getUrl(), TOKEN);
    event =
        getApplicationEvaluationEvent(application.getId(), "release", policyEvaluationOutcome, affectedComponentsCount,
            criticalComponentsCount, severeComponentsCount, moderateComponentsCount, "commitHash");
    return projectUri;
  }

  private void assertGitHubStatusMessage(
      final ProjectUri projectUri,
      final ApplicationEvaluationEvent event,
      final String commitHash, final String status, final GitHubApiClient mockGitHubApiClient) throws IOException
  {
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<StatusRequest> statusRequestCaptor = ArgumentCaptor.forClass(StatusRequest.class);
    verify(mockGitHubApiClient).createStatus(stringCaptor.capture(), stringCaptor.capture(), stringCaptor.capture(),
        statusRequestCaptor.capture());

    List<String> stringArguments = stringCaptor.getAllValues();
    assertThat(stringArguments.get(0)).isEqualTo(projectUri.getOrganization());
    assertThat(stringArguments.get(1)).isEqualTo(projectUri.getProject());
    assertThat(stringArguments.get(2)).isEqualTo(commitHash);

    StatusRequest actualStatusRequest = statusRequestCaptor.getValue();
    assertThat(actualStatusRequest.context).isEqualTo("IQ Policy Evaluation");
    assertThat(actualStatusRequest.description).isEqualTo(String
        .format("Components: Critical: %d, Severe: %d, Moderate: %d", event.criticalComponentCount,
            event.severeComponentCount, event.moderateComponentCount));
    assertThat(actualStatusRequest.state).isEqualTo(status);
    assertThat(actualStatusRequest.targetUrl).isEqualTo("http://localhost:8070/ui/links/application/app/report/scanId");
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
}
