/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.PullRequestCommand;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.PullRequestResponse;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.git.PullRequestTask.DEFAULT_COMMITTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestTaskTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0.0", "", "jar");

  private static final String BRANCH = "testBranch";

  private static final String TITLE = "PR title";

  private static final String CONTENT = "PR content";

  private static final String APP_INTERNAL_ID = "8f9a4a2973804402ab5c6bd0ee453ed9";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(PullRequestTask.class);

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private GitApiFactory gitApiFactory;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private FileCleaner fileCleaner;

  @Mock
  private Application app;

  @Mock
  private GitApi gitApi;

  @Mock
  private PullRequestRemediationDetails pullRequestRemediationDetails;

  @Mock
  private GitApiClient gitClient;

  @Mock
  private PullRequestResponse pullRequestResponse;

  @Mock
  private SourceControlPullRequestMetrics metrics;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private SourceControlUtils sourceControlUtils;

  @Mock
  private PullRequestExecutor pullRequestExecutor;

  private SourceControlConfig sourceControlConfig;

  //Subject
  private PullRequestTask pullRequestTask;

  public static final GitRepositoryInfo INFO =
      new GitRepositoryInfo("localhost", null, "token", SourceControlProvider.GITHUB, "master", true, true, true, true,
          null);

  private GitRepositoryInfo gitRepositoryInfo;

  @Before
  public void setup() {
    sourceControlConfig = new SourceControlConfig();
    gitRepositoryInfo = new GitRepositoryInfo("http://localhost", null, "token", SourceControlProvider.GITHUB, "master",
        true, true, true, true, null);
    when(insightConfig.getSourceControl()).thenReturn(sourceControlConfig);
    pullRequestTask = new PullRequestTask(gitClientFactory, metrics, gitApiFactory, auditRecorder, sourceControlUtils,
        insightConfig);
  }

  @Test
  public void test_run_notInited() {
    pullRequestTask.run(null, null);
    assertThat(logOutput).atErrorLevel().contains("Missing required PullRequestRemediationDetails");
    verifyNoInteractions(sourceControlUtils, gitClientFactory, fileCleaner,
        app, metrics, auditRecorder, pullRequestRemediationDetails);

    pullRequestTask.run(pullRequestRemediationDetails, null);
    assertThat(logOutput).atErrorLevel().contains("Missing required PullRequestRemediationDetails");
    verifyNoInteractions(sourceControlUtils, gitClientFactory, fileCleaner,
        app, metrics, auditRecorder, pullRequestRemediationDetails);
  }

  @Test
  public void test_run_nothing_remediated() throws Exception {
    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);
    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);

    pullRequestTask.run(pullRequestRemediationDetails, new PullRequestExecutor());

    verify(gitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(gitApi).branch(targetDirectory, BRANCH);
    verifyNoMoreInteractions(gitApi);
    verify(metrics).addResult(anyString(), any(EnhancedPullRequestResult.class));
    verifyNoInteractions(fileCleaner, gitClient);
  }

  @Test
  public void test_run_nothing_remediated_custom_directory() throws Exception {
    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);
    sourceControlConfig.setCloneDirectory(APP_INTERNAL_ID);
    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);

    pullRequestTask.run(pullRequestRemediationDetails, new PullRequestExecutor());

    verify(gitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(metrics).addResult(anyString(), any(EnhancedPullRequestResult.class));
    verifyNoInteractions(fileCleaner, gitClient);
  }

  @Test
  public void test_run_existing_content() throws Exception {
    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);
    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    targetDirectory.mkdirs();
    File pomFile = new File(targetDirectory, "pom.xml");
    FileUtils.copyURLToFile(getClass().getResource("/PullRequestTaskTest/test-pom.xml"), pomFile);

    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);
    when(gitClient.createPullRequest(BRANCH, gitRepositoryInfo.baseBranch, TITLE, CONTENT))
        .thenReturn(pullRequestResponse);
    when(pullRequestResponse.getUrl()).thenReturn(gitRepositoryInfo.repositoryUrl);

    pullRequestTask.run(pullRequestRemediationDetails, new PullRequestExecutor());

    verify(gitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(gitApi).branch(targetDirectory, BRANCH);
    verify(gitApi).commit(targetDirectory, DEFAULT_COMMITTER, GitApi.DEFAULT_COMMITTER_EMAIL, TITLE);
    verify(gitApi).push(targetDirectory);
    verify(auditRecorder).recordSystemEvent(eq(AuditEvent.CREATE_PULL_REQUEST));
    verify(metrics).addResult(anyString(), any(EnhancedPullRequestResult.class));

    assertThat(logOutput).atInfoLevel().contains("Pull request task initiated for application");
    assertThat(logOutput).atInfoLevel().contains("Pull request task completed for application");
    assertThat(logOutput).atInfoLevel().contains("successful=true");
  }

  @Test
  public void test_run_failure() throws Exception {
    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);
    when(gitApi.cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch))
        .thenThrow(new GitException("Something bad happened"));

    pullRequestTask.run(pullRequestRemediationDetails, new PullRequestExecutor());

    verify(sourceControlUtils).deleteCheckoutDirectory(app);
    assertThat(logOutput).atErrorLevel().contains("Failed to execute pull request, cleaning pull request directory");
  }

  @Test
  public void test_default_committer() throws Exception {
    sourceControlConfig.setCommitUsername(null); // same as default, none defined
    sourceControlConfig.setCommitEmail(null); // same as default, none defined

    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);

    when(pullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(pullRequestRemediationDetails, pullRequestExecutor);

    ArgumentCaptor<PullRequestCommand> prCommandCaptor = ArgumentCaptor.forClass(PullRequestCommand.class);
    verify(pullRequestExecutor).execute(prCommandCaptor.capture());
    PullRequestCommand pullRequestCommand = prCommandCaptor.getValue();
    assertThat(pullRequestCommand.getCommitter()).isEqualTo(DEFAULT_COMMITTER);
    assertThat(pullRequestCommand.getCommitterEmail()).isEqualTo(GitApi.DEFAULT_COMMITTER_EMAIL);
    assertThat(gitRepositoryInfo.getRepositoryUrl()).isEqualTo("http://localhost"); // repo url is unchanged
  }

  @Test
  public void test_custom_committer() throws Exception {
    sourceControlConfig.setCommitUsername("bar");
    sourceControlConfig.setCommitEmail("foo@bar.com");

    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);

    when(pullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(pullRequestRemediationDetails, pullRequestExecutor);

    ArgumentCaptor<PullRequestCommand> prCommandCaptor = ArgumentCaptor.forClass(PullRequestCommand.class);
    verify(pullRequestExecutor).execute(prCommandCaptor.capture());
    PullRequestCommand pullRequestCommand = prCommandCaptor.getValue();
    assertThat(pullRequestCommand.getCommitter()).isEqualTo("bar");
    assertThat(pullRequestCommand.getCommitterEmail()).isEqualTo("foo@bar.com");
  }

  @Test
  public void test_use_username_in_repo_url() throws Exception {
    gitRepositoryInfo.provider = SourceControlProvider.BITBUCKET;
    gitRepositoryInfo.username = "foo";
    sourceControlConfig.setUseUsernameInRepositoryCloneUrl(true);

    File sonatypeWorkDir = temporaryFolder.newFolder();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWorkDir);

    File targetDirectory = new File(sourceControlConfig.getCloneDirectory(), APP_INTERNAL_ID);
    configureExpectations();
    when(sourceControlUtils.getCheckoutDirectory(app)).thenReturn(targetDirectory);

    when(pullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(pullRequestRemediationDetails, pullRequestExecutor);

    assertThat(gitRepositoryInfo.getRepositoryUrl()).isEqualTo("http://foo@localhost");
  }

  private void configureExpectations() {
    configureExpectations(gitRepositoryInfo);
  }

  private void configureExpectations(final GitRepositoryInfo info) {
    when(pullRequestRemediationDetails.getApp()).thenReturn(app);
    when(pullRequestRemediationDetails.getPullRequestBranchName()).thenReturn(BRANCH);
    when(pullRequestRemediationDetails.getContents()).thenReturn(CONTENT);
    when(pullRequestRemediationDetails.getRemediatedVersion()).thenReturn("1.0.1");
    when(pullRequestRemediationDetails.getToBeRemediated()).thenReturn(MAVEN_COORDINATES);
    when(pullRequestRemediationDetails.getTitle()).thenReturn(TITLE);
    when(pullRequestRemediationDetails.getStage()).thenReturn(Stage.ID_BUILD);
    when(pullRequestRemediationDetails.getScanId()).thenReturn("scan-id");
    when(sourceControlUtils.getGitRepositoryInfoForApplication(APP_INTERNAL_ID)).thenReturn(info);
    when(gitApiFactory.createGitApi(info)).thenReturn(gitApi);
    when(gitClientFactory.createApiClient(info)).thenReturn(gitClient);
    when(app.getId()).thenReturn(APP_INTERNAL_ID);
  }

  private PullRequestResult createPullRequestResult(final boolean success) {
    PullRequestResult pullRequestResult = new PullRequestResult();
    pullRequestResult.setSuccessful(success);
    return pullRequestResult;
  }
}
