/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.metrics.ScmOperationMetrics;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
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

import com.google.inject.Binder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.git.PullRequestTask.DEFAULT_COMMITTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PullRequestTaskTest
    extends AbstractComponentTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0.0", "", "jar");

  public static final GitRepositoryInfo INFO = new GitRepositoryInfo("localhost", null, null, "token",
      SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, false, null);

  private static final String BRANCH = "testBranch";

  private static final String TITLE = "PR title";

  private static final String CONTENT = "PR content";

  private static final String APP_INTERNAL_ID = "8f9a4a2973804402ab5c6bd0ee453ed9";

  @Rule
  public LogOutput logOutput = new LogOutput(PullRequestTask.class);

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private GitApiFactory mockGitApiFactory;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Mock
  private FileCleaner mockFileCleaner;

  @Mock
  private Application mockApplication;

  @Mock
  private GitApi mockGitApi;

  @Mock
  private PullRequestRemediationDetails mockPullRequestRemediationDetails;

  @Mock
  private GitApiClient mockGitClient;

  @Mock
  private PullRequestResponse mockPullRequestResponse;

  @Mock
  private SourceControlPullRequestMetrics mockSourceControlPullRequestMetrics;

  @Mock
  private AuditRecorder mockAuditRecorder;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ScmOperationMetrics mockScmOperationMetrics;

  @Mock
  private PullRequestExecutor mockPullRequestExecutor;

  // Subject
  @Inject
  private PullRequestTask pullRequestTask;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private InsightWork insightWork;

  private GitRepositoryInfo gitRepositoryInfo;

  @Inject
  private Configuration configuration;

  @Inject
  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Override
  public void configure(Binder binder) {
    lenient().when(mockPullRequestRemediationDetails.getApp()).thenReturn(mockApplication);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    binder.bind(SourceControlPullRequestMetrics.class).toInstance(mockSourceControlPullRequestMetrics);
    binder.bind(GitApiFactory.class).toInstance(mockGitApiFactory);
    binder.bind(AuditRecorder.class).toInstance(mockAuditRecorder);
    binder.bind(SourceControlUtils.class).toInstance(mockSourceControlUtils);
    binder.bind(ScmOperationMetrics.class).toInstance(mockScmOperationMetrics);

    binder.bind(FileCleaner.class).toInstance(mockFileCleaner);
    super.configure(binder);
  }

  @Before
  public void before() {
    gitRepositoryInfo = new GitRepositoryInfo("http://localhost", null, null, "token", SourceControlProvider.GITHUB,
        "master", true, true, true, true, true, true, false, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRun_notInited_nullDetails() {
    pullRequestTask.run(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRun_notInited_nullExecutor() {
    pullRequestTask.run(mockPullRequestRemediationDetails, null);
  }

  @Test(expected = RuntimeException.class)
  public void testRun_nothing_remediated() throws Exception {
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    tempEntity.newSourceControlConfiguration();
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    pullRequestTask.run(mockPullRequestRemediationDetails, new PullRequestExecutor());

    verify(mockGitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(mockGitApi).branch(targetDirectory, BRANCH);
    verifyNoMoreInteractions(mockGitApi);
    verify(mockSourceControlPullRequestMetrics).addResult(anyString(), any(EnhancedPullRequestResult.class));
    verifyNoInteractions(mockFileCleaner, mockGitClient);
  }

  @Test(expected = RuntimeException.class)
  public void testRun_nothing_remediated_custom_directory() throws Exception {
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(APP_INTERNAL_ID);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    pullRequestTask.run(mockPullRequestRemediationDetails, new PullRequestExecutor());

    verify(mockGitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(mockSourceControlPullRequestMetrics).addResult(anyString(), any(EnhancedPullRequestResult.class));
    verifyNoInteractions(mockFileCleaner, mockGitClient);
  }

  @Test
  public void testRun_existing_content() throws Exception {
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    tempEntity.newSourceControlConfiguration();
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    targetDirectory.mkdirs();
    File pomFile = new File(targetDirectory, "pom.xml");
    FileUtils.copyURLToFile(getClass().getResource("/PullRequestTaskTest/test-pom.xml"), pomFile);

    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockGitClient.createPullRequest(BRANCH, gitRepositoryInfo.baseBranch, TITLE, CONTENT))
        .thenReturn(mockPullRequestResponse);
    when(mockPullRequestResponse.getUrl()).thenReturn("https://github.com/someOrg/someRepo/pull/1");
    when(mockGitApi.cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch))
        .thenReturn("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");

    pullRequestTask.run(mockPullRequestRemediationDetails, new PullRequestExecutor());

    verify(mockGitApi).cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch);
    verify(mockGitApi).branch(targetDirectory, BRANCH);
    verify(mockGitApi).commit(targetDirectory, DEFAULT_COMMITTER, GitApi.DEFAULT_COMMITTER_EMAIL, TITLE);
    verify(mockGitApi).push(targetDirectory);
    verify(mockAuditRecorder).recordSystemEvent(eq(AuditEvent.CREATE_PULL_REQUEST));
    verify(mockSourceControlPullRequestMetrics).addResult(anyString(), any(EnhancedPullRequestResult.class));

    assertThat(logOutput).atInfoLevel().contains("Pull request task initiated for application");
    assertThat(logOutput).atInfoLevel().contains("Pull request task completed for application");
    assertThat(logOutput).atInfoLevel().contains("successful=true");
  }

  @Test
  public void testRun_failure() throws Exception {
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());

    tempEntity.newSourceControlConfiguration();
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockGitApi.cloneOrPullRepository(targetDirectory, gitRepositoryInfo.baseBranch))
        .thenThrow(new GitException("Something bad happened"));

    assertThatThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, new PullRequestExecutor()))
        .isInstanceOf(SourceControlException.class)
        .satisfies(ex -> assertThat(((SourceControlException) ex).getCategory())
            .isEqualTo(PullRequestFailureCategory.SCM_ERROR));

    verify(mockScmOperationMetrics).startPrCreationTimer(anyString());
    verify(mockScmOperationMetrics).recordPrCreationFailed(any());
  }

  @Test
  public void testRun_default_committer() throws Exception {
    tempEntity.newSourceControlConfiguration();

    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());

    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    ArgumentCaptor<PullRequestCommand> prCommandCaptor = ArgumentCaptor.forClass(PullRequestCommand.class);
    verify(mockPullRequestExecutor).execute(prCommandCaptor.capture());
    PullRequestCommand pullRequestCommand = prCommandCaptor.getValue();
    assertThat(pullRequestCommand.getCommitter()).isEqualTo(DEFAULT_COMMITTER);
    assertThat(pullRequestCommand.getCommitterEmail()).isEqualTo(GitApi.DEFAULT_COMMITTER_EMAIL);
    assertThat(gitRepositoryInfo.getRepositoryUrl()).isEqualTo("http://localhost"); // repo url is unchanged
    verify(mockScmOperationMetrics).startPrCreationTimer(anyString());
    verify(mockScmOperationMetrics).recordPrCreationCompleted(any());
  }

  @Test
  public void testRun_custom_committer() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setCommitUsername("bar");
    sourceControlConfiguration.setCommitEmail("foo@bar.com");
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());

    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    ArgumentCaptor<PullRequestCommand> prCommandCaptor = ArgumentCaptor.forClass(PullRequestCommand.class);
    verify(mockPullRequestExecutor).execute(prCommandCaptor.capture());
    PullRequestCommand pullRequestCommand = prCommandCaptor.getValue();
    assertThat(pullRequestCommand.getCommitter()).isEqualTo("bar");
    assertThat(pullRequestCommand.getCommitterEmail()).isEqualTo("foo@bar.com");
  }

  @Test
  public void testRun_use_username_in_repo_url() throws Exception {
    gitRepositoryInfo.provider = SourceControlProvider.BITBUCKET;
    gitRepositoryInfo.username = "foo";
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setUseUsernameInRepositoryCloneUrl(true);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());

    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    assertThat(gitRepositoryInfo.getRepositoryUrl()).isEqualTo("http://foo@localhost");
  }

  @Test
  public void testRun_unsuccessful() throws Exception {
    tempEntity.newSourceControlConfiguration();

    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());

    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);

    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(false));

    assertThatThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor))
        .isInstanceOf(SourceControlException.class)
        // The outer catch must preserve the inner SourceControlException's message
        // so the user-actionable "could not find a direct dependency in the project
        // configuration" hint reaches the UI tooltip via PullRequestCreationFailedDTO.reason.
        .hasMessageContaining("Pull request creation failed")
        .hasMessageContaining("could not find a direct dependency in the project configuration")
        .satisfies(ex -> assertThat(((SourceControlException) ex).getCategory())
            .isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND));

    verify(mockScmOperationMetrics).startPrCreationTimer(anyString());
    verify(mockScmOperationMetrics).recordPrCreationFailed(any());
  }

  @Test
  public void testRun_Success_PersistsAutomaticPR() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    PullRequestResult pullRequestResult = createPullRequestResult(true);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(pullRequestResult);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
    Date now = new Date();

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestDAO.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequests.get(0);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("http://localhost");
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(1);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(BRANCH);
    assertThat(sourceControlPullRequest.getCreateTime()).isAfterOrEqualTo(now);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(
        sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    assertThat(sourceControlPullRequest.getSource()).isEqualTo(PullRequestSource.AUTOMATIC);
  }

  @Test
  public void testRun_Success_PersistsManualPR() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    PullRequestResult pullRequestResult = createPullRequestResult(true);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(pullRequestResult);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
    Date now = new Date();
    when(mockPullRequestRemediationDetails.isManualPullRequest()).thenReturn(true);

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestDAO.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequests.get(0);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("http://localhost");
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(1);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(BRANCH);
    assertThat(sourceControlPullRequest.getCreateTime()).isAfterOrEqualTo(now);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(
        sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    assertThat(sourceControlPullRequest.getSource()).isEqualTo(PullRequestSource.MANUAL);
  }

  @Test
  public void testRun_Success_PersistsAutomaticInnerSourcePR() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    PullRequestResult pullRequestResult = createPullRequestResult(true);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(pullRequestResult);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
    Date now = new Date();
    when(mockPullRequestRemediationDetails.isInnerSource()).thenReturn(true);

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestDAO.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequests.get(0);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("http://localhost");
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(1);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(BRANCH);
    assertThat(sourceControlPullRequest.getCreateTime()).isAfterOrEqualTo(now);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(
        sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    assertThat(sourceControlPullRequest.getSource()).isEqualTo(PullRequestSource.AUTOMATIC_INNER_SOURCE);
  }

  @Test
  public void testRun_Success_PersistsManualInnerSourcePR() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    PullRequestResult pullRequestResult = createPullRequestResult(true);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(pullRequestResult);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
    Date now = new Date();
    when(mockPullRequestRemediationDetails.isManualPullRequest()).thenReturn(true);
    when(mockPullRequestRemediationDetails.isInnerSource()).thenReturn(true);

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    List<SourceControlPullRequest> sourceControlPullRequests = sourceControlPullRequestDAO.getAll();
    assertThat(sourceControlPullRequests).hasSize(1);
    SourceControlPullRequest sourceControlPullRequest = sourceControlPullRequests.get(0);
    assertThat(sourceControlPullRequest.getRepositoryUrl()).isEqualTo("http://localhost");
    assertThat(sourceControlPullRequest.getPullRequestId()).isEqualTo(1);
    assertThat(sourceControlPullRequest.getHeadCommitHash()).isEqualTo("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");
    assertThat(sourceControlPullRequest.getBranchName()).isEqualTo(BRANCH);
    assertThat(sourceControlPullRequest.getCreateTime()).isAfterOrEqualTo(now);
    assertThat(sourceControlPullRequest.getLastCheckTime()).isEqualTo(sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getLastDetectedUpdateTime()).isEqualTo(
        sourceControlPullRequest.getCreateTime());
    assertThat(sourceControlPullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    assertThat(sourceControlPullRequest.getSource()).isEqualTo(PullRequestSource.MANUAL_INNER_SOURCE);
  }

  @Test
  public void testRun_Failure_DoesNotPersistPR() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    PullRequestResult pullRequestResult = createPullRequestResult(false);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(pullRequestResult);
    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();

    assertThatExceptionOfType(SourceControlException.class)
        .isThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor))
        .satisfies(ex -> assertThat(ex.getCategory())
            .isEqualTo(PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND));

    assertThat(sourceControlPullRequestDAO.getAll()).isEmpty();
  }

  @Test
  public void testRun_Success_PersistsTraceFieldsForPat() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockGitClientFactory.resolveAuthContext(gitRepositoryInfo))
        .thenReturn(ResolvedAuthContext.forPat("owner-PAT-1"));
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    when(mockPullRequestRemediationDetails.isManualPullRequest()).thenReturn(true);
    when(mockPullRequestRemediationDetails.getSourceControlEventId()).thenReturn("event-id-1");

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    SourceControlPullRequest persisted = sourceControlPullRequestDAO.getAll().get(0);
    assertThat(persisted.getSourceControlEventId()).isEqualTo("event-id-1");
    assertThat(persisted.getAuthenticationType()).isEqualTo("PAT");
    assertThat(persisted.getAuthOwnerId()).isEqualTo("owner-PAT-1");
    assertThat(persisted.getGithubAppId()).isNull();
    assertThat(persisted.getInstallationId()).isNull();
  }

  @Test
  public void testRun_Success_PersistsTraceFieldsForGithubApp() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockGitClientFactory.resolveAuthContext(gitRepositoryInfo))
        .thenReturn(ResolvedAuthContext.forGithubApp("owner-O", 12345, 99999L));
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    when(mockPullRequestRemediationDetails.isManualPullRequest()).thenReturn(false);
    when(mockPullRequestRemediationDetails.getSourceControlEventId()).thenReturn("event-id-2");

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    SourceControlPullRequest persisted = sourceControlPullRequestDAO.getAll().get(0);
    assertThat(persisted.getSourceControlEventId()).isEqualTo("event-id-2");
    assertThat(persisted.getAuthenticationType()).isEqualTo("GITHUB_APP");
    assertThat(persisted.getAuthOwnerId()).isEqualTo("owner-O");
    assertThat(persisted.getGithubAppId()).isEqualTo("12345");
    assertThat(persisted.getInstallationId()).isEqualTo("99999");
  }

  @Test
  public void testRun_Success_SetsTraceFieldsOnEventEntityWhenProvided() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockGitClientFactory.resolveAuthContext(gitRepositoryInfo))
        .thenReturn(ResolvedAuthContext.forGithubApp("owner-O", 12345, 99999L));
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(true));
    when(mockPullRequestRemediationDetails.isManualPullRequest()).thenReturn(false);
    when(mockPullRequestRemediationDetails.getSourceControlEventId()).thenReturn("event-id-3");

    SourceControlEvent liveEvent = new SourceControlEvent().forRemediationPullRequest();
    when(mockPullRequestRemediationDetails.getSourceControlEvent()).thenReturn(liveEvent);

    pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor);

    assertThat(liveEvent.getAuthenticationType()).isEqualTo("GITHUB_APP");
    assertThat(liveEvent.getAuthOwnerId()).isEqualTo("owner-O");
    assertThat(liveEvent.getGithubAppId()).isEqualTo("12345");
    assertThat(liveEvent.getInstallationId()).isEqualTo("99999");
    assertThat(liveEvent.getOutcome()).isEqualTo("SUCCESS");
    assertThat(liveEvent.getFailureReason()).isNull();
  }

  @Test
  public void testRun_Failure_NoFakeSecretLeaksAnywhere() throws Exception {
    final String fakePrivateKey =
        "-----BEGIN RSA PRIVATE KEY-----AAAAB3NzaC1yc2EFAKEKEYBLOCK-----END RSA PRIVATE KEY-----";
    final String fakeOauthToken = "ghp_FakeOauthTokenForTestingZZZ123";
    final String fakeProviderBody =
        "{\"message\":\"401 Unauthorized\",\"token\":\"" + fakeOauthToken + "\"}";

    Application app = tempEntity.newApplicationWithParent();
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(app.getId())
        .forRemediationPullRequest();
    sourceControlEventDAO.insert(event);

    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestRemediationDetails.getSourceControlEventId()).thenReturn(event.getId());

    // Force a hard exception whose message and stack trace contain fake secrets.
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class)))
        .thenThrow(new RuntimeException(fakePrivateKey + " " + fakeProviderBody));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor));

    // The real failure-path artifact is the source_control_event row written by the catch block.
    // None of the fake secret byte sequences must appear there.
    SourceControlEvent reloaded = sourceControlEventDAO.getById(event.getId());
    assertThat(reloaded.getFailureReason()).doesNotContain(fakePrivateKey);
    assertThat(reloaded.getFailureReason()).doesNotContain(fakeOauthToken);
    assertThat(reloaded.getFailureReason()).doesNotContain("PRIVATE KEY");
    assertThat(reloaded.getOutcome()).doesNotContain(fakePrivateKey);
    assertThat(reloaded.getOutcome()).doesNotContain(fakeOauthToken);
    assertThat(reloaded.getAuthOwnerId()).isNull();
  }

  @Test
  public void testRun_HardException_PersistsCategoricalReasonOntoSourceControlEvent() throws Exception {
    final String fakeSecret = "ghp_FakeOauthTokenZZZ_should_never_appear_in_persisted_data";

    Application app = tempEntity.newApplicationWithParent();
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(app.getId())
        .forRemediationPullRequest();
    sourceControlEventDAO.insert(event);

    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestRemediationDetails.getSourceControlEventId()).thenReturn(event.getId());
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class)))
        .thenThrow(new RuntimeException(fakeSecret));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor));

    SourceControlEvent reloaded = sourceControlEventDAO.getById(event.getId());
    assertThat(reloaded.getOutcome()).isEqualTo("FAILURE");
    // Categorical token from the closed vocabulary, never the raw exception message.
    assertThat(reloaded.getFailureReason()).isEqualTo("unknown_provider_error");
    assertThat(reloaded.getFailureReason()).doesNotContain(fakeSecret);
    assertThat(reloaded.getAuthenticationType()).isEqualTo("PAT");
    assertThat(reloaded.getAuthOwnerId()).isNull(); // gitRepositoryInfo from configureExpectations has null authOwnerId
  }

  @Test
  public void testRun_Failure_EmitsCreatePullRequestAuditWithFailureReason() throws Exception {
    tempEntity.newSourceControlConfiguration();
    File sonatypeWorkDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    configuration.sourceControlConfigurationChanged();
    File targetDirectory = insightWork.getSourceControlDir(APP_INTERNAL_ID);
    configureExpectations();
    when(mockSourceControlUtils.getCheckoutDirectory(mockApplication)).thenReturn(targetDirectory);
    when(mockPullRequestExecutor.execute(any(PullRequestCommand.class))).thenReturn(createPullRequestResult(false));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> pullRequestTask.run(mockPullRequestRemediationDetails, mockPullRequestExecutor));

    // The audit row is still emitted on the soft-failure path (matches design Decision D4 — single event, both
    // outcomes).
    verify(mockAuditRecorder).recordSystemEvent(eq(AuditEvent.CREATE_PULL_REQUEST));
  }

  private void configureExpectations() {
    configureExpectations(gitRepositoryInfo);
  }

  private void configureExpectations(final GitRepositoryInfo info) {
    lenient().when(mockPullRequestRemediationDetails.getApp()).thenReturn(mockApplication);
    lenient().when(mockPullRequestRemediationDetails.getPullRequestBranchName()).thenReturn(BRANCH);
    lenient().when(mockPullRequestRemediationDetails.getContents()).thenReturn(CONTENT);
    lenient().when(mockPullRequestRemediationDetails.getRemediatedVersion()).thenReturn("1.0.1");
    lenient().when(mockPullRequestRemediationDetails.getToBeRemediated()).thenReturn(MAVEN_COORDINATES);
    lenient().when(mockPullRequestRemediationDetails.getTitle()).thenReturn(TITLE);
    lenient().when(mockPullRequestRemediationDetails.getStage()).thenReturn(Stage.ID_BUILD);
    lenient().when(mockPullRequestRemediationDetails.getScanId()).thenReturn("scan-id");
    lenient().when(mockSourceControlUtils.getGitRepositoryInfoForApplication(APP_INTERNAL_ID)).thenReturn(info);
    lenient().when(mockGitApiFactory.createGitApi(info)).thenReturn(mockGitApi);
    lenient().when(mockGitClientFactory.createApiClient(info)).thenReturn(mockGitClient);
    lenient().when(mockGitClientFactory.resolveAuthContext(info))
        .thenReturn(ResolvedAuthContext.forPat(info.authOwnerId));
    lenient().when(mockApplication.getId()).thenReturn(APP_INTERNAL_ID);
  }

  private PullRequestResult createPullRequestResult(final boolean success) {
    PullRequestResult pullRequestResult = new PullRequestResult();
    pullRequestResult.setSuccessful(success);
    pullRequestResult.setPullRequestUrl("https://github.com/someOrg/someRepo/pull/1");
    pullRequestResult.setHeadRef("88cbe4cc92408f3d53e4ca120cf51b60c9757ddd");
    return pullRequestResult;
  }
}
