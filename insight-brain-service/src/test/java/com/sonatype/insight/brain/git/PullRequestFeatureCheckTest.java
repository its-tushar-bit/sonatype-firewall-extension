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
import com.sonatype.nexus.scm.SourceControlProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestFeatureCheckTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

  private static final String TOKEN = "token";

  private static final String BASE_BRANCH = "master";

  private static final boolean DEFAULT_ENABLE_PR = true;

  private static final boolean DEFAULT_ENABLE_STATUS_CHECKS = true;

  private static final String REPO_URL = "repo-url";

  private ListAppender<ILoggingEvent> listAppender;

  private PullRequestFeatureCheck pullRequestFeatureCheck;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private GitApiService gitApiService;

  @Mock
  private PullRequestUtils pullRequestUtils;

  @Before
  public void setup() {
    pullRequestFeatureCheck =
        new PullRequestFeatureCheck(productLicense, gitApiService, pullRequestUtils);

    Logger log = (Logger) LoggerFactory.getLogger(PullRequestFeatureCheck.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
  }

  @After
  public void cleanup() {
    Logger log = (Logger) LoggerFactory.getLogger(PullRequestFeatureCheck.class);
    log.detachAppender(listAppender);
  }

  @Test
  public void testLicenseInvalid() throws IOException {
    when(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)).thenReturn(false);

    boolean result =
        pullRequestFeatureCheck.isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID));

    assertThat(result).isFalse();
    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getMessage())
        .isEqualTo("Pull request feature is not supported for this license");

    verifyZeroInteractions(gitApiService);
  }

  @Test
  public void testApplicationNotConfigured() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    gitRepositoryInfo.token = null;
    ensureAppNotConfigured(gitRepositoryInfo);

    gitRepositoryInfo.token = TOKEN;
    gitRepositoryInfo.repositoryUrl = "  ";
    ensureAppNotConfigured(gitRepositoryInfo);

    gitRepositoryInfo.repositoryUrl = REPO_URL;
    gitRepositoryInfo.enableStatusChecks = true;
    gitRepositoryInfo.enablePullRequests = null;
    ensureAppNotConfigured(gitRepositoryInfo);

    gitRepositoryInfo.enablePullRequests = true;
    gitRepositoryInfo.provider = null;
    ensureAppNotConfigured(gitRepositoryInfo);
  }

  private void ensureAppNotConfigured(GitRepositoryInfo gitRepositoryInfo) throws IOException {
    when(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)).thenReturn(true);
    when(gitApiService.getGitRepositoryInfoForApplication(eq(PUBLIC_ID))).thenReturn(gitRepositoryInfo);
    listAppender.list.clear();

    boolean result =
        pullRequestFeatureCheck.isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID));

    assertThat(result).isFalse();
    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .isEqualTo(String.format("Pull requests have not been configured for application '%s'", PUBLIC_ID));
  }

  @Test
  public void testIsPullRequestAllowed() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    when(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)).thenReturn(true);
    when(gitApiService.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestUtils.isPullRequestAllowed(eq(gitRepositoryInfo))).thenReturn(false);

    boolean result =
        pullRequestFeatureCheck.isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID));

    assertThat(result).isFalse();
    assertThat(listAppender.list.size()).isEqualTo(1);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getMessage())
        .isEqualTo("Pull requests are not supported for application '{}' and repository '{}'");
  }

  @Test
  public void testHappyPath() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo();

    when(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)).thenReturn(true);
    when(gitApiService.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestUtils.isPullRequestAllowed(eq(gitRepositoryInfo))).thenReturn(true);

    boolean result =
        pullRequestFeatureCheck.isPullRequestFeatureSupported(new Application(PUBLIC_ID, NAME, ORGANIZATION_ID));

    assertThat(result).isTrue();
    assertThat(listAppender.list.size()).isEqualTo(0);
  }

  private GitRepositoryInfo newGitRepositoryInfo() {
    GitRepositoryInfo sourceControlDTO =
        new GitRepositoryInfo(REPO_URL, TOKEN, SourceControlProvider.GITHUB, BASE_BRANCH, DEFAULT_ENABLE_PR,
            DEFAULT_ENABLE_STATUS_CHECKS);
    return sourceControlDTO;
  }
}
