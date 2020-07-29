/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableMap;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestRepositoryValidatorTest
{
  private static final String TEST_REPO_URL = "%s/sonatype/repo/";

  @Mock
  private GitClientFactory gitClientFactory;

  @Mock
  private GitApiClient gitApiClient;

  private PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private ListAppender<ILoggingEvent> listAppender;

  @Before
  public void setup() {
    pullRequestRepositoryValidator = new PullRequestRepositoryValidator(gitClientFactory, getTestInsightConfig());

    Logger log = (Logger) LoggerFactory.getLogger(PullRequestRepositoryValidator.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
  }

  @Test
  public void isPullRequestAllowed_RepoDisabled() {
    String repoUrl = String.format(TEST_REPO_URL, GITHUB_COM);

    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoUrl, GITHUB);
    gitRepositoryInfo.enablePullRequests = false;

    assertThat(
        pullRequestRepositoryValidator.isRepoValidForPRs(gitRepositoryInfo))
        .isFalse();

    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .isEqualTo("Pull requests have not been enabled for repository URL '" + repoUrl + "'");

  }

  @Test
  public void isPullRequestAllowed_GitHubEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com/");
    assertThat(pullRequestRepositoryValidator
        .isRepoValidForPRs(newGitRepositoryInfo(repoName, GITHUB)))
        .isTrue();
  }

  @Test
  public void isPullRequestAllowed_GitLabEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgitlab.com/");
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    assertThat(pullRequestRepositoryValidator
        .isRepoValidForPRs(newGitRepositoryInfo(repoName, GITLAB)))
        .isFalse();
  }

  @Test
  public void isPullRequestAllowed_BitBucketFlow() throws IOException {
    String repoName = String.format(TEST_REPO_URL, "https://foo.org/");
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate: isPrivateValues) {
      when(gitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(newGitRepositoryInfo(repoName, BITBUCKET)))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void isPullRequestAllowed_PrivateGithub() throws IOException {
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://github.com/");

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate: isPrivateValues) {
      when(gitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(newGitRepositoryInfo(repoName, GITHUB)))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void isPullRequestAllowed_PrivateGitlab() throws IOException {
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://gitlab.com/");

    boolean[] isPrivateValues = {true, false};
    for (boolean isPrivate: isPrivateValues) {
      when(gitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);
      assertThat(pullRequestRepositoryValidator.isRepoValidForPRs(newGitRepositoryInfo(repoName, GITLAB)))
          .isEqualTo(isPrivate);
    }
  }

  @Test
  public void isPullRequestAllowed_ClientError() throws IOException {
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(gitApiClient);
    when(gitApiClient.isRepositoryPrivate()).thenThrow(new IOException());

    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() ->
        pullRequestRepositoryValidator
            .isRepoValidForPRs(newGitRepositoryInfo(String.format(TEST_REPO_URL, GITHUB_COM), GITHUB))
    );
  }

  @Test
  public void isInternalRepository_GitHubEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com/");
    assertThat(pullRequestRepositoryValidator
        .isInternalRepository(newGitRepositoryInfo(repoName, GITHUB)))
        .isTrue();
  }

  @Test
  public void isInternalRepository_GitLabEnterpriseFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgitlab.com/");
    assertThat(pullRequestRepositoryValidator
        .isInternalRepository(newGitRepositoryInfo(repoName, GITLAB)))
        .isFalse();
  }

  @Test
  public void isInternalRepository_GitHubCloudFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://github.com/");
    assertThat(pullRequestRepositoryValidator
        .isInternalRepository(newGitRepositoryInfo(repoName, GITHUB)))
        .isFalse();
  }

  @Test
  public void isInternalRepository_GitLabCloudFlow() {
    String repoName = String.format(TEST_REPO_URL, "https://gitlab.com/");
    assertThat(pullRequestRepositoryValidator
        .isInternalRepository(newGitRepositoryInfo(repoName, GITLAB)))
        .isFalse();
  }

  @Test
  public void isInternalRepository_NotGithubOrGitLab() {
    String repoName = String.format(TEST_REPO_URL, "https://repo.com/");
    Arrays.stream(SourceControlProvider.values())
        .filter(sourceControlProvider -> sourceControlProvider != GITHUB && sourceControlProvider != GITLAB)
        .forEach(sourceControlProvider -> {
          assertThat(pullRequestRepositoryValidator
              .isInternalRepository(newGitRepositoryInfo(repoName, sourceControlProvider)))
              .isFalse();
        });
  }

  private GitRepositoryInfo newGitRepositoryInfo(final String repoUrl, final SourceControlProvider provider) {
    boolean enablePullRequests = true;
    boolean enableStatusChecks = true;
    String username = provider.requiresUsername() ? "username" : null;
    return new GitRepositoryInfo(repoUrl, username, "token", provider, "baseBranch", enablePullRequests,
        enableStatusChecks);
  }

  private InsightConfig getTestInsightConfig() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setExperimentalFeatures(
        new ImmutableMap.Builder<String, Boolean>().put("automaticMergeRequests", true).build());
    return insightConfig;
  }
}
