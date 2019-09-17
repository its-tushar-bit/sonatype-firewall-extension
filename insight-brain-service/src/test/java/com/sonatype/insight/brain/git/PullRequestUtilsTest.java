/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

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

import static com.sonatype.insight.brain.git.PullRequestUtils.GITHUB_COM;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestUtilsTest
{
  private static final String TEST_REPO_URL = "%s/sonatype/repo/";

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private GitApiClient gitApiClient;

  private PullRequestUtils pullRequestUtils;

  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void setup() {
    pullRequestUtils = new PullRequestUtils(gitClientFactory);

    Logger log = (Logger) LoggerFactory.getLogger(PullRequestUtils.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
  }

  @Test
  public void isPullRequestAllowed_RepoDisabled() throws IOException {
    String repoUrl = String.format(TEST_REPO_URL, GITHUB_COM);

    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoUrl, GITHUB);
    gitRepositoryInfo.enablePullRequests = false;

    assertThat(
        pullRequestUtils.isPullRequestAllowed(gitRepositoryInfo))
        .isFalse();

    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .isEqualTo("Pull requests have not been enabled for repository URL '" + repoUrl + "'");

  }

  @Test
  public void isPullRequestAllowed_GitHubEnterpriseFlow() throws IOException {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com/");
    assertThat(pullRequestUtils
        .isPullRequestAllowed(newGitRepositoryInfo(repoName, GITHUB)))
        .isTrue();
  }

  @Test
  public void isPullRequestAllowed_PrivateGithub() throws IOException {
    when(gitClientFactory.create(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://github.com/");

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate: isPrivateValues) {
      when(gitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);
      assertThat(pullRequestUtils.isPullRequestAllowed(newGitRepositoryInfo(repoName, GITHUB))).isEqualTo(isPrivate);
    }
  }

  @Test
  public void isPullRequestAllowed_ClientError() throws IOException {
    when(gitClientFactory.create(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    when(gitApiClient.isRepositoryPrivate()).thenThrow(new IOException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        pullRequestUtils.isPullRequestAllowed(newGitRepositoryInfo(String.format(TEST_REPO_URL, GITHUB_COM), GITHUB))
    );
  }

  @Test
  public void isPullRequestAllowed_GitLabNotYetSupported() throws IOException {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com/");

    // TODO replace test when we support GitLab for PRs
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      pullRequestUtils
          .isPullRequestAllowed(newGitRepositoryInfo(repoName, GITLAB));
    }).withMessage("'GITLAB' not supported yet");
  }

  private GitRepositoryInfo newGitRepositoryInfo(final String repoUrl, final SourceControlProvider provider) {
    boolean enablePullRequests = true;
    boolean enableStatusChecks = true;
    return new GitRepositoryInfo(repoUrl, "token", provider, "baseBranch", enablePullRequests, enableStatusChecks);
  }
}
