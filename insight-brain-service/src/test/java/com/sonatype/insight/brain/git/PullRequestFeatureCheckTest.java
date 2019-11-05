/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestFeatureCheckTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String APP_ID = "app-id";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

  private static final String TOKEN = "token";

  private static final String BASE_BRANCH = "master";

  private static final boolean DEFAULT_ENABLE_PR = true;

  private static final boolean DEFAULT_ENABLE_STATUS_CHECKS = true;

  private static final String REPO_URL = "repo-url";

  private PullRequestFeatureCheck pullRequestFeatureCheck;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private PullRequestUtils pullRequestUtils;

  @Rule
  public LogOutput logOutput = new LogOutput(PullRequestFeatureCheck.class);

  @Before
  public void setup() {
    pullRequestFeatureCheck =
        new PullRequestFeatureCheck(productLicense, pullRequestUtils);
  }

  @Test
  public void testLicenseInvalid() throws IOException {
    when(productLicense.hasFeature(LicensedFeature.AUTOMATION)).thenReturn(false);

    boolean result = pullRequestFeatureCheck.isPullRequestFeatureSupported(
        new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), newGitRepositoryInfo());

    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains(
        "Pull request feature is not supported for this license");
  }

  @Test
  public void testApplicationNotConfigured() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    gitRepositoryInfo.token = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Token");

    gitRepositoryInfo.token = TOKEN;
    gitRepositoryInfo.repositoryUrl = "  ";
    ensureAppNotConfigured(gitRepositoryInfo, "Repository URL");
    logOutput.clear();

    gitRepositoryInfo.repositoryUrl = REPO_URL;
    gitRepositoryInfo.enableStatusChecks = false;
    gitRepositoryInfo.enablePullRequests = null;
    ensureAppNotConfigured(gitRepositoryInfo, null);
    assertThat(logOutput).atDebugLevel().contains("Pull Requests have been explicitly disabled");
    logOutput.clear();

    gitRepositoryInfo.enableStatusChecks = true;
    gitRepositoryInfo.enablePullRequests = null;
    ensureAppNotConfigured(gitRepositoryInfo, null);
    assertThat(logOutput).atDebugLevel().contains("Pull Requests have been explicitly disabled");
    logOutput.clear();

    gitRepositoryInfo.enablePullRequests = true;
    gitRepositoryInfo.provider = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Provider");
    logOutput.clear();

    // test multiples
    gitRepositoryInfo.token = null;
    gitRepositoryInfo.provider = null;
    gitRepositoryInfo.repositoryUrl = null;
    ensureAppNotConfigured(gitRepositoryInfo, "Token, Repository URL, Provider");
    logOutput.clear();

    ensureAppNotConfigured(null, null);
  }

  @Test
  public void testProviderNotSupported() throws IOException {
    when(productLicense.hasFeature(LicensedFeature.AUTOMATION)).thenReturn(true);

    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();
    gitRepositoryInfo.provider = SourceControlProvider.GITLAB;
    boolean result = pullRequestFeatureCheck
        .isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), gitRepositoryInfo);

    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains("Source provider 'gitlab' is not supported");
  }

  private void ensureAppNotConfigured(final GitRepositoryInfo gitRepositoryInfo, String missingFields)
      throws IOException
  {
    when(productLicense.hasFeature(LicensedFeature.AUTOMATION)).thenReturn(true);

    Application app = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    boolean result = pullRequestFeatureCheck.isPullRequestFeatureSupported(
        app, gitRepositoryInfo);

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
    verify(pullRequestUtils, never()).isPullRequestAllowed(Mockito.any(GitRepositoryInfo.class));
  }

  @Test
  public void testIsPullRequestAllowed() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    when(productLicense.hasFeature(LicensedFeature.AUTOMATION)).thenReturn(true);
    when(pullRequestUtils.isPullRequestAllowed(eq(gitRepositoryInfo))).thenReturn(false);

    final Application app = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    app.setId(APP_ID);

    boolean result =
        pullRequestFeatureCheck.isPullRequestFeatureSupported(
            app, gitRepositoryInfo);

    assertThat(result).isFalse();
    assertThat(logOutput).atDebugLevel().contains(String.format(
        "Pull requests are not supported for application '%s' and repository '%s'",
        APP_ID, REPO_URL));
  }

  @Test
  public void testHappyPath() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    when(productLicense.hasFeature(LicensedFeature.AUTOMATION)).thenReturn(true);
    when(pullRequestUtils.isPullRequestAllowed(eq(gitRepositoryInfo))).thenReturn(true);

    boolean result = pullRequestFeatureCheck.isPullRequestFeatureSupported(
        new Application(PUBLIC_ID, NAME, ORGANIZATION_ID), gitRepositoryInfo);

    assertThat(result).isTrue();
    assertThat(logOutput).atAnyLevel().isEmpty();
  }

  private GitRepositoryInfo newGitRepositoryInfo() {
    return new GitRepositoryInfo(REPO_URL, TOKEN, SourceControlProvider.GITHUB,
        BASE_BRANCH, DEFAULT_ENABLE_PR, DEFAULT_ENABLE_STATUS_CHECKS);
  }
}
