/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Arrays;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmRepoVisibilityServiceTest
    extends AbstractComponentTest
{
  private static final String TEST_REPO_URL = "%s/sonatype/repo/";

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Mock
  private GitApiClient mockGitApiClient;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ScmRepoVisibilityService scmRepoVisibilityService;

  @Override
  public void configure(Binder binder) {
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);

    super.configure(binder);
  }

  @Test
  public void testIsRepositoryValidForPullRequestFeatures_GitHubEnterprise() {
    String repoName = String.format(TEST_REPO_URL, "https://NOTgithub.com");
    assertThat(scmRepoVisibilityService
        .isRepositoryValidForPullRequestFeatures(newGitRepositoryInfo(repoName, SourceControlProvider.GITHUB)))
            .isTrue();
  }

  @Test
  public void testIsRepositoryValidForPullRequestFeatures_GitLabEnterprise() {
    testIsRepositoryValidForPullRequestFeatures_GitLabEnterprise(true);

    testProductLicense.setMissingFeatures(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    testIsRepositoryValidForPullRequestFeatures_GitLabEnterprise(false);
  }

  private void testIsRepositoryValidForPullRequestFeatures_GitLabEnterprise(boolean expected) {
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://NOTgitlab.com");
    assertThat(scmRepoVisibilityService
        .isRepositoryValidForPullRequestFeatures(newGitRepositoryInfo(repoName, SourceControlProvider.GITLAB)))
            .isEqualTo(expected);
  }

  @Test
  public void testIsRepositoryValidForPullRequestFeatures_GitHubCloud() {
    testIsRepositoryValidForPullRequestFeatures_GitHubCloud(true);

    testProductLicense.setMissingFeatures(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    testIsRepositoryValidForPullRequestFeatures_GitHubCloud(false);
  }

  private void testIsRepositoryValidForPullRequestFeatures_GitHubCloud(boolean expected) {
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://github.com/");
    assertThat(scmRepoVisibilityService
        .isRepositoryValidForPullRequestFeatures(newGitRepositoryInfo(repoName, SourceControlProvider.GITHUB)))
            .isEqualTo(expected);
  }

  @Test
  public void testIsRepositoryValidForPullRequestFeatures_GitLabCloud() {
    testIsRepositoryValidForPullRequestFeatures_GitLabCloud(true);

    testProductLicense.setMissingFeatures(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    testIsRepositoryValidForPullRequestFeatures_GitLabCloud(false);
  }

  private void testIsRepositoryValidForPullRequestFeatures_GitLabCloud(boolean expected) {
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://gitlab.com/");
    assertThat(scmRepoVisibilityService
        .isRepositoryValidForPullRequestFeatures(newGitRepositoryInfo(repoName, SourceControlProvider.GITLAB)))
            .isEqualTo(expected);
  }

  @Test
  public void testIsRepositoryValidForPullRequestFeatures_NotGithubOrGitLab() {
    testIsRepositoryValidForPullRequestFeatures_NotGithubOrGitLab(true);

    testProductLicense.setMissingFeatures(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    testIsRepositoryValidForPullRequestFeatures_NotGithubOrGitLab(false);
  }

  private GitRepositoryInfo testIsPrivateRepository(boolean isPrivate) throws IOException {
    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    when(mockGitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);

    String repoName = String.format(TEST_REPO_URL, "https://github.com/");
    GitRepositoryInfo gitRepositoryInfo = newGitRepositoryInfo(repoName, SourceControlProvider.GITHUB);

    assertThat(scmRepoVisibilityService.isPrivateRepository(gitRepositoryInfo)).isEqualTo(isPrivate);
    return gitRepositoryInfo;
  }

  @Test
  public void testIsPrivateRepository_PrivateRepo() throws IOException {
    testIsPrivateRepository(true);
  }

  @Test
  public void testIsPrivateRepository_PublicRepo() throws IOException {
    testIsPrivateRepository(false);
  }

  @Test
  public void testIsPrivateRepository_Cached() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = testIsPrivateRepository(true);

    // Create a new GitRepositoryInfo to verify the key of the cache is working as expected
    gitRepositoryInfo = newGitRepositoryInfo(gitRepositoryInfo.normalizedRepositoryUrl, SourceControlProvider.GITHUB);

    // Invoke again and should still return the cached 'true' value
    assertThat(scmRepoVisibilityService.isPrivateRepository(gitRepositoryInfo)).isTrue();

    // Verify only one invocation on the client
    verify(mockGitApiClient, times(1)).isRepositoryPrivate();
  }

  private void testIsRepositoryValidForPullRequestFeatures_NotGithubOrGitLab(boolean expected) {
    lenient().when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitApiClient);
    String repoName = String.format(TEST_REPO_URL, "https://example.com/");
    Arrays.stream(SourceControlProvider.values())
        .filter(sourceControlProvider -> sourceControlProvider != SourceControlProvider.GITHUB
            && sourceControlProvider != SourceControlProvider.GITLAB)
        .forEach(sourceControlProvider -> assertThat(
            scmRepoVisibilityService
                .isRepositoryValidForPullRequestFeatures(newGitRepositoryInfo(repoName, sourceControlProvider)))
                    .isEqualTo(expected));
  }

  private GitRepositoryInfo newGitRepositoryInfo(String repoUrl, SourceControlProvider provider) {
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
