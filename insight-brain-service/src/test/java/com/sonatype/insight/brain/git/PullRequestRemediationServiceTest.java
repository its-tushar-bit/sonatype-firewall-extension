/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import javax.inject.Provider;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

  // subject
  private PullRequestRemediationService pullRequestRemediationService;

  public PullRequestRemediationServiceTest() {
    super(PullRequestRemediationService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    pullRequestRemediationService = new PullRequestRemediationService(mockPullRequestExecutor, mockGitClientFactory,
        mockApplicationDAO, mockSourceControlUtils, mockPullRequestTaskProvider);
  }

  private Application setupApplication(String appId) {
    Application application = new Application();
    application.setId(appId);
    when(mockApplicationDAO.getById(appId)).thenReturn(application);
    return application;
  }

  private void setupGitRepositoryInfoForApp(String appId) {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);

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

    // given: a repo branch that does not already exist
    Application application = setupApplication(appId);
    setupBranchExistence(branchName, false);
    setupGitRepositoryInfoForApp(appId);

    when(mockPullRequestTaskProvider.get()).thenReturn(mockPullRequestTask);

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
  }

  @Test
  public void testOnRemediateComponent_branchExistsOnServer() throws Exception {
    // given: a repo branch that already exists
    final String branchName = "branch/already/exists";
    setupBranchExistence(branchName, true);
    SourceControlEvent event = new SourceControlEvent().setBranchName(branchName);
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("repoUrl", "username", "token", SourceControlProvider.GITLAB, "baseBranch", true, true);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    // when: try to remediate a component for this same branch
    pullRequestRemediationService.onRemediateComponent(event);

    // then:
    assertThatLogMessagesEqual(
        info("Branch already exists on remote server for remediation [branch/already/exists]")
    );
  }

  @Test
  public void testIsFormatSupportedForPullRequestRemediation_notSupported() {
    // when we check format support for a format we know is not currently supported
    boolean supported =
        pullRequestRemediationService.isFormatSupportedForPullRequestRemediation(ComponentIdentifier.FORMAT_NUGET);

    // then we see that the format is not supported
    assertThat(supported).isFalse();
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

  private void setupBranchExistence(String branchName, boolean exists) throws IOException {
    when(mockGitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isBranchOnServer(branchName)).thenReturn(exists);
  }
}
