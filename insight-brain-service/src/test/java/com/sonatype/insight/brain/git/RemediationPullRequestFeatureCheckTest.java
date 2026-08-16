/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RemediationPullRequestFeatureCheckTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String APP_ID = "app-id";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

  private static final String USERNAME = "username";

  private static final String TOKEN = "token";

  private static final String BASE_BRANCH = "master";

  private static final boolean DEFAULT_REMEDIATION_PULL_REQUESTS_ENABLED = true;

  private static final boolean DEFAULT_MANUAL_PULL_REQUESTS_ENABLED = true;

  private static final boolean DEFAULT_INNER_SOURCE_AUTOMATED_UPDATES_ENABLED = true;

  private static final boolean DEFAULT_STATUS_CHECKS_ENABLED = true;

  private static final boolean DEFAULT_PULL_REQUEST_COMMENTING_ENABLED = true;

  private static final boolean DEFAULT_SOURCE_CONTROL_EVALUATIONS_ENABLED = true;

  private static final boolean DEFAULT_SSH_ENABLED = false;

  private static final String DEFAULT_SOURCE_CONTROL_SCAN_TARGET = null;

  private static final String REPO_URL = "repo-url";

  private static final String REPO_SSH_URL = "ssh-repo-url";

  private RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  @Mock
  private PullRequestRepositoryValidator pullRequestRepositoryValidator;

  @RegisterExtension
  public LogOutput logOutput = new LogOutput(PullRequestFeatureCheck.class);

  @BeforeEach
  public void setup() {
    remediationPullRequestFeatureCheck =
        new RemediationPullRequestFeatureCheck(licenseChecker, pullRequestRepositoryValidator);
  }

  @Test
  public void testLicenseInvalid() {
    when(licenseChecker.isPullRequestRemediationSupported()).thenReturn(false);

    boolean result = remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
        new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), newGitHubRepositoryInfo(), false);

    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel()
        .contains(
            "Remediation pull request feature is not supported for this license");
  }

  @Test
  public void testApplicationNotConfigured() {
    GitRepositoryInfo gitRepositoryInfo = newGitHubRepositoryInfo();

    gitRepositoryInfo.token = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Token");

    gitRepositoryInfo.token = TOKEN;
    gitRepositoryInfo.repositoryUrl = "  ";
    ensureAppNotConfigured(gitRepositoryInfo, "Repository URL");
    logOutput.clear();

    gitRepositoryInfo.repositoryUrl = REPO_URL;
    gitRepositoryInfo.statusChecksEnabled = false;
    gitRepositoryInfo.remediationPullRequestsEnabled = null;
    ensureAppNotConfigured(gitRepositoryInfo, null);
    assertThat(logOutput).atDebugLevel().contains("Pull Requests have been explicitly disabled");
    logOutput.clear();

    gitRepositoryInfo.statusChecksEnabled = true;
    gitRepositoryInfo.remediationPullRequestsEnabled = null;
    ensureAppNotConfigured(gitRepositoryInfo, null);
    assertThat(logOutput).atDebugLevel().contains("Pull Requests have been explicitly disabled");
    logOutput.clear();

    gitRepositoryInfo.remediationPullRequestsEnabled = true;
    gitRepositoryInfo.provider = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Provider");
    logOutput.clear();

    // test multiples
    gitRepositoryInfo.token = null;
    gitRepositoryInfo.provider = null;
    gitRepositoryInfo.repositoryUrl = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Provider, Repository URL, Token");
    logOutput.clear();

    ensureAppNotConfigured(null, null);
  }

  @Test
  public void testBitBucketSupported() {
    GitRepositoryInfo gitRepositoryInfo = newBitBucketRepositoryInfo();

    when(licenseChecker.isPullRequestRemediationSupported()).thenReturn(true);
    when(pullRequestRepositoryValidator.isRepoValidForPRs(eq(gitRepositoryInfo))).thenReturn(true);

    boolean result = remediationPullRequestFeatureCheck
        .isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), gitRepositoryInfo, false);

    assertThat(result).isTrue();
  }

  @Test
  public void testApplicationNotConfiguredBitbucket() {
    GitRepositoryInfo gitRepositoryInfo = newBitBucketRepositoryInfo();

    gitRepositoryInfo.username = null;
    gitRepositoryInfo.token = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Username, Token");

    gitRepositoryInfo.username = "Username";
    ensureAppNotConfigured(gitRepositoryInfo, "Token");
  }

  private void ensureAppNotConfigured(final GitRepositoryInfo gitRepositoryInfo, String missingFields) {
    when(licenseChecker.isPullRequestRemediationSupported()).thenReturn(true);

    Application app = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    boolean result = remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
        app, gitRepositoryInfo, false);

    assertThat(result).isFalse();
    assertThat(logOutput)
        .atDebugLevel()
        .contains(String.format("Pull requests have not been configured for application '%s'", app.getId()));
    if (missingFields != null) {
      assertThat(logOutput)
          .atDebugLevel()
          .contains(String
              .format("Application has not been fully configured for pull requests. Missing: [%s]", missingFields));
    }
    verify(pullRequestRepositoryValidator, never()).isRepoValidForPRs(Mockito.any(GitRepositoryInfo.class));
  }

  @Test
  public void testIsPullRequestAllowed() {
    GitRepositoryInfo gitRepositoryInfo = newGitHubRepositoryInfo();

    when(licenseChecker.isPullRequestRemediationSupported()).thenReturn(true);
    when(pullRequestRepositoryValidator.isRepoValidForPRs(eq(gitRepositoryInfo))).thenReturn(false);

    final Application app = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    app.setId(APP_ID);

    boolean result =
        remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
            app, gitRepositoryInfo, false);

    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel()
        .contains(String.format(
            "Pull requests are not supported for application '%s' and repository '%s'",
            APP_ID, REPO_URL));
  }

  @Test
  public void testHappyPath() {
    GitRepositoryInfo gitRepositoryInfo = newGitHubRepositoryInfo();

    when(licenseChecker.isPullRequestRemediationSupported()).thenReturn(true);
    when(pullRequestRepositoryValidator.isRepoValidForPRs(eq(gitRepositoryInfo))).thenReturn(true);

    boolean result = remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(
        new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), gitRepositoryInfo, false);

    assertThat(result).isTrue();
    assertThat(logOutput).atAnyLevel().isEmpty();
  }

  private GitRepositoryInfo newGitHubRepositoryInfo() {
    return new GitRepositoryInfo(REPO_URL, REPO_SSH_URL, null, TOKEN, SourceControlProvider.GITHUB,
        BASE_BRANCH, DEFAULT_REMEDIATION_PULL_REQUESTS_ENABLED, DEFAULT_INNER_SOURCE_AUTOMATED_UPDATES_ENABLED,
        DEFAULT_MANUAL_PULL_REQUESTS_ENABLED,
        DEFAULT_STATUS_CHECKS_ENABLED,
        DEFAULT_PULL_REQUEST_COMMENTING_ENABLED, DEFAULT_SOURCE_CONTROL_EVALUATIONS_ENABLED,
        DEFAULT_SSH_ENABLED, DEFAULT_SOURCE_CONTROL_SCAN_TARGET);
  }

  private GitRepositoryInfo newBitBucketRepositoryInfo() {
    return new GitRepositoryInfo(REPO_URL, REPO_SSH_URL, USERNAME, TOKEN, SourceControlProvider.BITBUCKET,
        BASE_BRANCH, DEFAULT_REMEDIATION_PULL_REQUESTS_ENABLED, DEFAULT_MANUAL_PULL_REQUESTS_ENABLED,
        DEFAULT_INNER_SOURCE_AUTOMATED_UPDATES_ENABLED,
        DEFAULT_STATUS_CHECKS_ENABLED,
        DEFAULT_PULL_REQUEST_COMMENTING_ENABLED, DEFAULT_SOURCE_CONTROL_EVALUATIONS_ENABLED,
        DEFAULT_SSH_ENABLED, DEFAULT_SOURCE_CONTROL_SCAN_TARGET);
  }
}
