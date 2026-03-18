/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.UncheckedIOException;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.git.PullRequestRepositoryValidator.GITHUB_COM;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestRepositoryValidatorTest
{
  private static final String TEST_REPO_URL = "%s/sonatype/repo/";

  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  private PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void setup() {
    pullRequestRepositoryValidator = new PullRequestRepositoryValidator(mockScmRepoVisibilityService);

    Logger log = (Logger) LoggerFactory.getLogger(PullRequestRepositoryValidator.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
  }

  @Test
  public void testIsRepoValidForPRs_RepoDisabled() {
    String repoUrl = String.format(TEST_REPO_URL, GITHUB_COM);

    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoUrl, GITHUB);
    gitRepositoryInfo.remediationPullRequestsEnabled = false;
    gitRepositoryInfo.innerSourceAutomatedUpdatesEnabled = false;

    assertThat(
        pullRequestRepositoryValidator.isRepoValidForPRs(gitRepositoryInfo))
            .isFalse();

    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .isEqualTo("Pull requests have not been enabled for repository URL '" + repoUrl + "'");

  }

  @Test
  public void testIsRepoValidForPRs_GitHubEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com/");
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoName, GITHUB);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo))).thenReturn(true);
    assertThat(pullRequestRepositoryValidator
        .isRepoValidForPRs(gitRepositoryInfo))
            .isTrue();
  }

  @Test
  public void testIsRepoValidForPRs_GitLabEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgitlab.com/");
    assertThat(pullRequestRepositoryValidator
        .isRepoValidForPRs(newGitRepositoryInfo(repoName, GITLAB)))
            .isFalse();
  }

  @Test
  public void testIsRepoValidForPRs_BitBucketFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://foo.org/");
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoName, BITBUCKET);

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate : isPrivateValues) {
      when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo)))
          .thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(gitRepositoryInfo))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void testIsRepoValidForPRs_PrivateGithub() {
    String repoName = String.format(TEST_REPO_URL, "https://github.com/");
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoName, GITHUB);

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate : isPrivateValues) {
      when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo)))
          .thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(gitRepositoryInfo))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void testIsRepoValidForPRs_PrivateGitlab() {
    String repoName = String.format(TEST_REPO_URL, "https://gitlab.com/");
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoName, GITLAB);

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate : isPrivateValues) {
      when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo)))
          .thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(gitRepositoryInfo))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void testIsRepoValidForPRs_ClientError() {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(String.format(TEST_REPO_URL, GITHUB_COM), GITHUB);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo)))
        .thenThrow(UncheckedIOException.class);

    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> pullRequestRepositoryValidator
        .isRepoValidForPRs(gitRepositoryInfo));
  }

  private GitRepositoryInfo newGitRepositoryInfo(final String repoUrl, final SourceControlProvider provider) {
    boolean remediationPullRequestEnabled = true;
    boolean manualPullRequestEnabled = true;
    boolean innerSourceUpdatesEnabled = true;
    boolean statusChecksEnabled = true;
    boolean pullRequestCommentingEnabled = true;
    boolean sourceControlEvaluationsEnabled = true;
    boolean sshEnabled = false;

    String sourceControlScanTarget = null;
    String username = provider.requiresUsername() ? "username" : null;
    return new GitRepositoryInfo(repoUrl, null, username, "token", provider, "baseBranch",
        remediationPullRequestEnabled, manualPullRequestEnabled, innerSourceUpdatesEnabled, statusChecksEnabled,
        pullRequestCommentingEnabled,
        sourceControlEvaluationsEnabled, sshEnabled, sourceControlScanTarget);
  }
}
