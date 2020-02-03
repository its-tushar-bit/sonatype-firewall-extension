/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.PullRequestResponse;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.git.PullRequestTask.DEFAULT_COMMITTER;
import static com.sonatype.insight.brain.git.PullRequestTask.DEFAULT_COMMITTER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

  private static final String APP_PUBLIC_ID = "sandbox-application";

  private static final String APP_HASH = "-khbn98"; // hash of APP_INTERNAL_ID with leading dash

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(PullRequestTask.class);
  
  @Mock
  private GitApiService gitApiService;
  
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
  
  //Subject
  private PullRequestTask pullRequestTask;

  public static final GitRepositoryInfo INFO =
      new GitRepositoryInfo("localhost", "token", SourceControlProvider.GITHUB, "master", true, true);

  @Before
  public void setup() {
    pullRequestTask = new PullRequestTask(gitApiService, gitClientFactory, insightConfig, fileCleaner, metrics,
        gitApiFactory, auditRecorder);
  }
  
  @Test
  public void test_run_notInited() {
    pullRequestTask.run();
    assertThat(logOutput).atErrorLevel().contains("Missing required PullRequestRemediationDetails");
    verifyNoInteractions(gitApiService, gitClientFactory, insightConfig, fileCleaner,
        app, metrics, auditRecorder, pullRequestRemediationDetails);
  }
  
  @Test
  public void test_run_nothing_remediated() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    configureExpectations(config);

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    assertThat(targetDirectory.exists(), is(true));
    verify(gitApi).cloneOrPullRepository(targetDirectory, INFO.baseBranch);
    verify(gitApi).branch(targetDirectory, BRANCH);
    verifyNoMoreInteractions(gitApi);
    verify(metrics).addResult(anyString(), any(PullRequestResult.class));
    verifyNoInteractions(fileCleaner, gitClient);
  }

  @Test
  public void test_strips_special_characters_from_branch() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    configureExpectations(config, new GitRepositoryInfo("localhost", "token", SourceControlProvider.GITHUB, "d\\e/v_",
        true, true));

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    // then directory is created without special characters
    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-dev" + APP_HASH);
    assertThat(targetDirectory.exists(), is(true));
    // and git branch name is not modified
    verify(gitApi).cloneOrPullRepository(targetDirectory, "d\\e/v_");
  }

  @Test
  public void test_truncates_long_public_app_names() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    configureExpectations(config, new GitRepositoryInfo("localhost", "token", SourceControlProvider.GITHUB,
        "branchname", true, true));

    String longName = StringUtils.repeat("long", 21);
    when(app.getPublicId()).thenReturn(longName);

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    // then directory is created with truncated name and hash appended
    File targetDirectory = new File(config.getCloneDirectory(), "longlonglonglonglonglongl-branchname" + APP_HASH);
    assertThat(targetDirectory.exists(), is(true));
    // and git branch name is not modified
    verify(gitApi).cloneOrPullRepository(targetDirectory, "branchname");
  }

  @Test
  public void test_truncates_long_branch_names() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    configureExpectations(config, new GitRepositoryInfo("localhost", "token", SourceControlProvider.GITHUB,
        StringUtils.repeat("long", 21) + "branchname", true, true));

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    // then directory is created with truncated name and hash appended
    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-longlonglonglon" + APP_HASH);
    assertThat(targetDirectory.exists(), is(true));
    // and git branch name is not modified
    verify(gitApi).cloneOrPullRepository(targetDirectory, StringUtils.repeat("long", 21) + "branchname");
  }

  @Test
  public void test_run_nothing_remediated_custom_directory() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    config.setCloneDirectory(APP_INTERNAL_ID);
    configureExpectations(config);

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    assertThat(targetDirectory.exists(), is(true));
    assertThat(targetDirectory.getParentFile().getName(), is(APP_INTERNAL_ID));
    verify(gitApi).cloneOrPullRepository(targetDirectory, INFO.baseBranch);
    verify(metrics).addResult(anyString(), any(PullRequestResult.class));
    verifyNoInteractions(fileCleaner, gitClient);
  }

  @Test
  public void test_run_existing_content() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);
    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    targetDirectory.mkdirs();
    File pomFile = new File(targetDirectory, "pom.xml");
    FileUtils.copyURLToFile(getClass().getResource("/PullRequestTaskTest/test-pom.xml"), pomFile);

    configureExpectations(config);
    when(gitClient.createPullRequest(BRANCH, INFO.baseBranch, TITLE, CONTENT)).thenReturn(pullRequestResponse);
    when(pullRequestResponse.getUrl()).thenReturn(INFO.repositoryUrl);

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();
    
    verify(gitApi).cloneOrPullRepository(targetDirectory, INFO.baseBranch);
    verify(gitApi).branch(targetDirectory, BRANCH);
    verify(gitApi).commit(targetDirectory, DEFAULT_COMMITTER, DEFAULT_COMMITTER_EMAIL, TITLE);
    verify(gitApi).push(targetDirectory);
    verify(metrics).addResult(anyString(), any(PullRequestResult.class));

    assertThat(logOutput).atDebugLevel().contains("Using existing directory for pull request");
    assertThat(logOutput).atInfoLevel().contains("Pull request task initiated for application");
    assertThat(logOutput).atInfoLevel().contains("Pull request task completed for application");
    assertThat(logOutput).atInfoLevel().contains("successful=true");
  }
  
  @Test
  public void test_run_failure() throws Exception {
    SourceControlConfig config = new SourceControlConfig();
    File sonatypeWorkDir = temporaryFolder.newFolder();
    config.setSonatypeWorkDir(sonatypeWorkDir);

    configureExpectations(config);
    File targetDirectory = new File(config.getCloneDirectory(), APP_PUBLIC_ID + "-" + INFO.baseBranch + APP_HASH);
    when(gitApi.cloneOrPullRepository(targetDirectory, INFO.baseBranch))
        .thenThrow(new GitException("Something bad happened"));

    pullRequestTask.init(pullRequestRemediationDetails, new PullRequestExecutor());
    pullRequestTask.run();

    verify(fileCleaner).delete(targetDirectory);
    assertThat(logOutput).atErrorLevel().contains("Failed to execute pull request, cleaning pull request directory");
  }

  private void configureExpectations(final SourceControlConfig config) {
    configureExpectations(config, INFO);
  }

  private void configureExpectations(final SourceControlConfig config, final GitRepositoryInfo info) {
    when(insightConfig.getSourceControl()).thenReturn(config);
    when(pullRequestRemediationDetails.getApp()).thenReturn(app);
    when(pullRequestRemediationDetails.getPullRequestBranchName()).thenReturn(BRANCH);
    when(pullRequestRemediationDetails.getContents()).thenReturn(CONTENT);
    when(pullRequestRemediationDetails.getRemediatedVersion()).thenReturn("1.0.1");
    when(pullRequestRemediationDetails.getToBeRemediated()).thenReturn(MAVEN_COORDINATES);
    when(pullRequestRemediationDetails.getTitle()).thenReturn(TITLE);
    when(pullRequestRemediationDetails.getStage()).thenReturn(Stage.ID_BUILD);
    when(pullRequestRemediationDetails.getScanId()).thenReturn("scan-id");
    when(gitApiService.getGitRepositoryInfoForApplication(APP_INTERNAL_ID)).thenReturn(info);
    when(gitApiFactory.createGitApi(info)).thenReturn(gitApi);
    when(gitClientFactory.create(info)).thenReturn(gitClient);
    when(app.getId()).thenReturn(APP_INTERNAL_ID);
    when(app.getPublicId()).thenReturn(APP_PUBLIC_ID);
  }
}
