/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.File;
import java.nio.file.Files;

import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.service.githubapp.GitHubAppSelectionService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlUtilsTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  private static final String SCM_USERNAME = "username";

  private SourceControlDataService mockSourceControlDataService;

  private GitClientFactory mockGitClientFactory;

  private GitApiClient mockGitClientApi;

  private GitHubAppDAO mockGitHubAppDAO;

  private GitHubAppSelectionService mockGitHubAppSelectionService;

  private Application application;

  private Organization org;

  @Inject
  private SourceControlUtils sourceControlUtils;

  @Inject
  private InsightWork insightWork;

  @Override
  public void configure(Binder binder) {
    mockSourceControlDataService = mock(SourceControlDataService.class);
    mockGitClientFactory = mock(GitClientFactory.class);
    mockGitClientApi = mock(GitApiClient.class);
    mockGitHubAppDAO = mock(GitHubAppDAO.class);
    mockGitHubAppSelectionService = mock(GitHubAppSelectionService.class);
    binder.bind(SourceControlDataService.class).toInstance(mockSourceControlDataService);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    binder.bind(GitApiClient.class).toInstance(mockGitClientApi);
    binder.bind(GitHubAppDAO.class).toInstance(mockGitHubAppDAO);
    binder.bind(GitHubAppSelectionService.class).toInstance(mockGitHubAppSelectionService);
    super.configure(binder);
  }

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    application = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_repositoryValuesDefinedInApplication() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(false)
        .setStatusChecksEnabled(false)
        .setPullRequestCommentingEnabled(true)
        .setSourceControlEvaluationsEnabled(true)
        .setSshEnabled(true)
        .setSourceControlScanTarget("/target/*")
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    verify(mockSourceControlDataService).getCompositeSourceControlByOwnerDecrypted(application.getId());
    assertGitRepositoryInfoValues(value);
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_noAppLevelSourceControlRecord() {
    SourceControl orgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getId())
        .setRepositoryUrl(VALID_URL)
        .setStatusChecksEnabled(false)
        .setBaseBranch("base-branch")
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(orgSourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());
    assertThat(value).isNull();
  }

  private void assertGitRepositoryInfoValues(GitRepositoryInfo gitRepositoryInfo) {
    assertThat(gitRepositoryInfo).isNotNull();
    // values from application
    assertThat(gitRepositoryInfo.remediationPullRequestsEnabled).isFalse();
    assertThat(gitRepositoryInfo.statusChecksEnabled).isFalse();
    // values inherited from organization/root organization if applicable
    assertThat(gitRepositoryInfo.token).isEqualTo(TOKEN);
    assertThat(gitRepositoryInfo.provider).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(gitRepositoryInfo.pullRequestCommentingEnabled).isTrue();
    assertThat(gitRepositoryInfo.sourceControlEvaluationsEnabled).isTrue();
    assertThat(gitRepositoryInfo.sourceControlScanTarget).isEqualTo("/target/*");
    assertThat(gitRepositoryInfo.baseBranch).isEqualTo("base-branch");

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_defaultBranch() {
    // given : composite source control with null base branch
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setBaseBranch(null)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    // when : get repo info for app
    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    // then : expect result to have default base branch
    assertThat(value.baseBranch).isEqualTo(SourceControlUtils.DEFAULT_BASE_BRANCH);
    verify(mockSourceControlDataService).getCompositeSourceControlByOwnerDecrypted(application.getId());

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_NoApplicationSourceControl() {
    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForApplication("INVALID");
    assertThat(value).isNull();
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoRepositoryUrl() {
    SourceControl sourceControl = new SourceControl.Builder().setOwnerId(application.getId()).build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoProvider() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_NoToken() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();
  }

  @Test
  public void testIsScmEnabled_RequiresUsername() {
    // given: source control is setup for a provider that requires username, but does not set the username
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.BITBUCKET)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();
    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    // expect: source control is not enabled
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isFalse();

    // given: we provide a username
    sourceControl.setUsername("username");

    // expect: source control is enabled
    assertThat(sourceControlUtils.isScmEnabled(application.getId())).isTrue();
  }

  @Test
  public void testGetCheckoutDirectory() {
    File expectedSourceControlDir = insightWork.getSourceControlDir(application.getId());
    assertThat(expectedSourceControlDir).doesNotExist();

    assertThat(sourceControlUtils.getCheckoutDirectory(application)).isEqualTo(expectedSourceControlDir);
    assertThat(expectedSourceControlDir).isDirectory();
  }

  @Test
  public void testDeleteCheckoutDirectory() throws Exception {
    File expectedSourceControlDir = insightWork.getSourceControlDir(application.getId());
    Files.createDirectories(expectedSourceControlDir.toPath());
    new File(expectedSourceControlDir, "foo.txt").createNewFile();

    sourceControlUtils.deleteCheckoutDirectory(application);
    assertThat(expectedSourceControlDir).doesNotExist();
  }

  @Test
  public void testIsBitbucketCloud() {
    // BB cloud
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("https://bitbucket.org/organization/project",
        null,
        "user",
        TOKEN,
        BITBUCKET,
        "base-branch",
        true,
        true,
        true,
        null,
        true,
        true,
        true,
        false,
        null);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isTrue();

    // BB server
    gitRepositoryInfo =
        new GitRepositoryInfo("https://my.domain.com/organization/project",
            null,
            "user",
            TOKEN,
            BITBUCKET,
            "base-branch",
            true,
            true,
            true,
            null,
            true,
            true,
            true,
            false,
            null);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isFalse();

    // Not BB
    gitRepositoryInfo =
        new GitRepositoryInfo("https://my.domain.com/organization/project",
            null,
            "user",
            TOKEN,
            GITHUB,
            "base-branch",
            true,
            true,
            true,
            null,
            true,
            true,
            true,
            false,
            null);
    assertThat(sourceControlUtils.isBitbucketCloud(gitRepositoryInfo)).isFalse();
  }

  @Test
  public void testGetUserId() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getParentOwnerId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("base-branch")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();

    when(mockGitClientFactory.createApiClient(any())).thenReturn(mockGitClientApi);
    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);
    when(mockGitClientApi.getSynchronizationKey()).thenReturn("scmUser");

    String userId = sourceControlUtils.getScmUserIdForApplication(application.getId());

    verify(mockSourceControlDataService, times(1)).getCompositeSourceControlByOwnerDecrypted(application.getId());
    verify(mockGitClientFactory, times(1)).createApiClient(any());
    verify(mockGitClientApi, times(1)).getSynchronizationKey();
    assertThat(userId).isNotNull();
    assertThat(userId).isEqualTo("scmUser");
  }

  @Test
  public void testGetGitRepositoryInfoForRepository_inheritRepositoryValuesFromOrganization() {
    SourceControl orgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getId())
        .setBaseBranch("base-branch")
        .setToken(TOKEN)
        .setUsername(SCM_USERNAME)
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .setPullRequestCommentingEnabled(true)
        .setSourceControlEvaluationsEnabled(true)
        .setSshEnabled(true)
        .setSourceControlScanTarget("/target/*")
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(org.getId())))
        .thenReturn(orgSourceControl);

    GitRepositoryInfo value = sourceControlUtils.getGitRepositoryInfoForRepository(org.getId(), VALID_URL, GITHUB);

    verify(mockSourceControlDataService).getCompositeSourceControlByOwnerDecrypted(org.getId());
    assertGitRepositoryInfoValuesForRepository(value);
  }

  private void assertGitRepositoryInfoValuesForRepository(GitRepositoryInfo gitRepositoryInfo) {
    assertThat(gitRepositoryInfo).isNotNull();
    // values for URL and provider
    assertThat(gitRepositoryInfo.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(gitRepositoryInfo.provider).isEqualTo(SourceControlProvider.GITHUB);
    // values inherited from organization/root organization if applicable
    assertThat(gitRepositoryInfo.token).isEqualTo(TOKEN);
    assertThat(gitRepositoryInfo.username).isEqualTo(SCM_USERNAME);
    assertThat(gitRepositoryInfo.pullRequestCommentingEnabled).isTrue();
    assertThat(gitRepositoryInfo.sourceControlEvaluationsEnabled).isTrue();
    assertThat(gitRepositoryInfo.sourceControlScanTarget).isEqualTo("/target/*");
    assertThat(gitRepositoryInfo.baseBranch).isEqualTo("base-branch");
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_WithGitHubApp_PopulatesAuthOwnerId() {
    // Given: Application with GitHub App authentication
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setProvider(SourceControlProvider.GITHUB)
        .setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP)
        .setBaseBranch("main")
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .build();

    GitHubApp mockGitHubApp = new GitHubApp();
    mockGitHubApp.setOwnerId(org.getId());
    mockGitHubApp.setInstallationId(12345L);

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);
    when(mockGitHubAppSelectionService.select(eq(application.getId())))
        .thenReturn(mockGitHubApp); // GitHub App found at org level

    // When
    GitRepositoryInfo result = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.authenticationType).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(result.authOwnerId).isEqualTo(org.getId()); // authOwnerId populated from hierarchy
    assertThat(result.token).isNull(); // No token for GitHub App

    verify(mockSourceControlDataService).getCompositeSourceControlByOwnerDecrypted(application.getId());
    verify(mockGitHubAppSelectionService).select(application.getId());
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_WithGitHubApp_NoGitHubAppInHierarchy() {
    // Given: GitHub App authentication configured but no GitHub App exists
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setProvider(SourceControlProvider.GITHUB)
        .setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP)
        .setBaseBranch("main")
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);
    when(mockGitHubAppSelectionService.select(eq(application.getId())))
        .thenReturn(null); // No GitHub App found

    // When
    GitRepositoryInfo result = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.authenticationType).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    // When GitHub App auth is configured but no app is found, authOwnerId falls back to sourceControl.ownerId
    assertThat(result.authOwnerId).isEqualTo(application.getId());

    verify(mockGitHubAppSelectionService).select(application.getId());
  }

  @Test
  public void testGetGitRepositoryInfoForApplication_WithToken_NoAuthOwnerIdPopulated() {
    // Given: Token-based authentication (should NOT populate authOwnerId)
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(VALID_URL)
        .setToken(TOKEN)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("main")
        .build();

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(application.getId())))
        .thenReturn(sourceControl);

    // When
    GitRepositoryInfo result = sourceControlUtils.getGitRepositoryInfoForApplication(application.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.authenticationType).isNull(); // No auth type specified (legacy)
    assertThat(result.token).isEqualTo(TOKEN);
    // When using PAT/token authentication, authOwnerId is still populated with sourceControl.ownerId
    assertThat(result.authOwnerId).isEqualTo(application.getId());

    // Verify GitHubAppSelectionService was NOT called for token-based authentication
    verify(mockGitHubAppSelectionService, never()).select(any());
  }

  @Test
  public void testGetGitRepositoryInfoForRepository_WithGitHubApp_PopulatesAuthOwnerId() {
    // Given: Organization with GitHub App authentication
    SourceControl orgSourceControl = new SourceControl.Builder()
        .setOwnerId(org.getId())
        .setBaseBranch("base-branch")
        .setUsername(SCM_USERNAME)
        .setProvider(SourceControlProvider.GITHUB)
        .setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP)
        .setRemediationPullRequestsEnabled(true)
        .setStatusChecksEnabled(true)
        .setPullRequestCommentingEnabled(true)
        .setSourceControlEvaluationsEnabled(true)
        .build();

    GitHubApp mockOrgGitHubApp = new GitHubApp();
    mockOrgGitHubApp.setOwnerId(org.getId());
    mockOrgGitHubApp.setInstallationId(54321L);

    when(mockSourceControlDataService.getCompositeSourceControlByOwnerDecrypted(eq(org.getId())))
        .thenReturn(orgSourceControl);
    when(mockGitHubAppSelectionService.select(eq(org.getId())))
        .thenReturn(mockOrgGitHubApp); // GitHub App at org level

    // When
    GitRepositoryInfo result = sourceControlUtils.getGitRepositoryInfoForRepository(
        org.getId(), VALID_URL, GITHUB);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.authenticationType).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
    assertThat(result.authOwnerId).isEqualTo(org.getId()); // authOwnerId populated
    assertThat(result.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(result.provider).isEqualTo(GITHUB);

    verify(mockGitHubAppSelectionService).select(org.getId());
  }

  @Test
  public void testIsScmEnabled_WithGitHubApp_ValidOwnerId() {
    // Given: GitRepositoryInfo with GitHub App and valid ownerId
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.provider = SourceControlProvider.GITHUB;
    gitInfo.repositoryUrl = VALID_URL;
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = application.getId(); // Valid ownerId

    // When
    boolean enabled = sourceControlUtils.isScmEnabled(gitInfo);

    // Then - should be enabled (ownerId is valid credential for GitHub App)
    assertThat(enabled).isTrue();
  }

  @Test
  public void testIsScmEnabled_WithGitHubApp_NoOwnerId() {
    // Given: GitRepositoryInfo with GitHub App but no ownerId
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.provider = SourceControlProvider.GITHUB;
    gitInfo.repositoryUrl = VALID_URL;
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = null; // No ownerId

    // When
    boolean enabled = sourceControlUtils.isScmEnabled(gitInfo);

    // Then - should NOT be enabled (no valid credentials)
    assertThat(enabled).isFalse();
  }

  @Test
  public void testIsScmEnabled_WithGitHubApp_EmptyOwnerId() {
    // Given: GitRepositoryInfo with GitHub App but empty ownerId
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.provider = SourceControlProvider.GITHUB;
    gitInfo.repositoryUrl = VALID_URL;
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = ""; // Empty ownerId

    // When
    boolean enabled = sourceControlUtils.isScmEnabled(gitInfo);

    // Then - should NOT be enabled (empty string is invalid)
    assertThat(enabled).isFalse();
  }
}
