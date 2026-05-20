/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.Date;

import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.github.auth.InstallationToken;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.git.utils.api.GitApi;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.git.GitApiFactory.X_BITBUCKET_API_TOKEN_AUTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitApiFactoryTest
    extends AbstractComponentTest
{
  private static final String GIT_EXECUTABLE = "/usr/bin/git";

  private static final GitRepositoryInfo GIT_REPOSITORY_INFO = new GitRepositoryInfo("localhost", null, null, "token",
      SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, false, null);

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  private GitApiFactory gitApiFactory;

  @Inject
  private Configuration configuration;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private InsightWork insightWork;

  @Inject
  private InsightProxy insightProxy;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private GitHubAppAuthStrategyCache mockAuthStrategyCache;

  @Inject
  private SourceControlUtils sourceControlUtils;

  private GitApiFactory testGitApiFactory;

  private GitApiFactory spyGitApiFactory;

  @Before
  public void setup() {
    // Initialize mocks
    MockitoAnnotations.openMocks(this);

    // Create GitApiFactory with mocked auth strategy cache
    testGitApiFactory = new GitApiFactory(
        configuration,
        insightWork,
        passwordHandler,
        gitHubAppDAO,
        insightProxy,
        mockAuthStrategyCache,
        sourceControlUtils);

    // Note usage of spy in order to override isNativeGitAvailable
    spyGitApiFactory = spy(testGitApiFactory);
  }

  @Test
  public void test_noNativeAvailable_noConfig() {
    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_noConfig() {
    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_nativeAvailable_gitExecutable_noConfig() {
    assumeThat(new File(GIT_EXECUTABLE)).exists();
    when(spyGitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitExecutable(GIT_EXECUTABLE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_noNativeAvailable_forceNativeViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_nativeAvailable_forceJavaViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
  }

  @Test
  public void test_NativeGit_gitTimeoutViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitTimeoutSeconds(600);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("timeout")
        .isEqualTo(600);
  }

  @Test
  public void test_JGit_gitTimeoutViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitTimeoutSeconds(600);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("timeout")
        .isEqualTo(600);
  }

  @Test
  public void test_noNativeAvailable_gitExecutable_forceViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(GIT_EXECUTABLE)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfiguration.setGitExecutable(GIT_EXECUTABLE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
  }

  @Test
  public void test_sshEnabledButNoSshUrl() {
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", null, null, "token",
        SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(sshGitRepositoryInfo))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("SSH is enabled for repository")
        .hasMessageContaining("but no SSH clone URL was");
  }

  @Test
  public void test_sshEnabledWithSshUrl() {
    String sshUrl = "git@github.com:foo/bar.git";
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", sshUrl, null,
        "token", SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(sshGitRepositoryInfo);
    assertThat(gitApi).hasFieldOrPropertyWithValue("repositoryUrl", sshUrl);
  }

  @Test
  public void test_sshEnabledButJgitConfigured() {
    String sshUrl = "git@github.com:foo/bar.git";
    GitRepositoryInfo sshGitRepositoryInfo = new GitRepositoryInfo("localhost", sshUrl, null, "token",
        SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, true, null);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(sshGitRepositoryInfo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application with URL " + sshUrl + " is configured to use SSH with JGit");
  }

  @Test
  public void test_JGit_gpgSigningKeyViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfiguration.setGpgPassphrase(passwordHandler.encryptPassword("test-passphrase"));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey")
        .isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase")
        .isEqualTo("test-passphrase".toCharArray());
  }

  @Test
  public void test_NativeGit_gpgSigningKeyViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfiguration.setGpgPassphrase(passwordHandler.encryptPassword("test-passphrase"));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgSigningKey")
        .isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgPassphrase")
        .isEqualTo("test-passphrase");
  }

  @Test
  public void test_JGit_nullGpgPassphraseViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key-id");
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey")
        .isEqualTo("test-gpg-key-id");
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase")
        .isNull();
  }

  @Test
  public void test_JGit_nullGpgInfoViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgSigningKey")
        .isNull();
    assertThat(gitApi).isInstanceOf(JGitApi.class)
        .extracting("gpgPassphrase")
        .isNull();
  }

  @Test
  public void test_NativeGit_nullGpgInfoViaConfig() {
    lenient().when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(GIT_REPOSITORY_INFO);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgSigningKey")
        .isNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .extracting("gpgPassphrase")
        .isNull();
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_GeneratesInstallationToken() throws Exception {
    // Setup GitHub App in database
    GitHubApp githubApp = createTestGitHubApp("test-owner-id");
    githubApp = tempEntity.newGitHubApp(githubApp);

    // Mock the auth strategy cache to return a mocked strategy
    GitHubAppAuthStrategy mockAuthStrategy = mock(GitHubAppAuthStrategy.class);
    InstallationToken mockToken = mock(InstallationToken.class);

    when(mockAuthStrategyCache.getOrCreate(githubApp.getId()))
        .thenReturn(mockAuthStrategy);
    when(mockAuthStrategy.getInstallationToken())
        .thenReturn(mockToken);
    when(mockToken.getToken())
        .thenReturn("ghs_mocked_installation_token");

    // Create GitRepositoryInfo with GitHub App authentication
    GitRepositoryInfo gitInfo = new GitRepositoryInfo(
        "https://github.com/test/repo",
        null, null, null, SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "test-owner-id";
    gitInfo.githubAppId = githubApp.getId();

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute
    GitApi gitApi = spyGitApiFactory.createGitApi(gitInfo);

    // Verify
    assertThat(gitApi).isNotNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class);

    // Verify the cache and auth strategy were called
    verify(mockAuthStrategyCache).getOrCreate(githubApp.getId());
    verify(mockAuthStrategy).getInstallationToken();
  }

  @Test
  public void testCreateGitApi_WithPATAuth_UsesTokenFromGitInfo() {
    // Create GitRepositoryInfo with PAT authentication
    GitRepositoryInfo gitInfo = new GitRepositoryInfo("https://github.com/test/repo", null,
        "testuser", "test-pat-token", SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.PAT;
    gitInfo.authOwnerId = "test-owner-id";

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute
    GitApi gitApi = spyGitApiFactory.createGitApi(gitInfo);

    // Verify
    assertThat(gitApi).isNotNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
    assertThat(gitApi).hasFieldOrPropertyWithValue("token", "test-pat-token".toCharArray());
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_MissingConfig_ThrowsException() {
    // Create GitRepositoryInfo with GitHub App auth but no config in database
    GitRepositoryInfo gitInfo = createGitInfoWithGitHubApp("non-existent-owner-id");

    // Mock the cache to throw NotFoundException wrapped in UncheckedExecutionException
    when(mockAuthStrategyCache.getOrCreate("non-existent-owner-id"))
        .thenThrow(new UncheckedExecutionException(
            new NotFoundException("GitHub App not found for ownerId: non-existent-owner-id")));

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute and verify exception
    // The NotFoundException is wrapped in UncheckedExecutionException by Guava's cache
    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(gitInfo))
        .isInstanceOf(UncheckedExecutionException.class)
        .hasCauseInstanceOf(NotFoundException.class)
        .hasMessageContaining("GitHub App not found for ownerId: non-existent-owner-id");
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_JGitImplementation() throws Exception {
    // Setup GitHub App
    GitHubApp githubApp = createTestGitHubApp("test-owner-id");
    githubApp = tempEntity.newGitHubApp(githubApp);

    // Mock the auth strategy cache to return a mocked strategy
    GitHubAppAuthStrategy mockAuthStrategy = mock(GitHubAppAuthStrategy.class);
    InstallationToken mockToken = mock(InstallationToken.class);

    when(mockAuthStrategyCache.getOrCreate(githubApp.getId()))
        .thenReturn(mockAuthStrategy);
    when(mockAuthStrategy.getInstallationToken())
        .thenReturn(mockToken);
    when(mockToken.getToken())
        .thenReturn("ghs_mocked_installation_token");

    // Create GitRepositoryInfo with GitHub App authentication
    GitRepositoryInfo gitInfo = createGitInfoWithGitHubApp("test-owner-id");
    gitInfo.githubAppId = githubApp.getId();

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    // Execute
    GitApi gitApi = spyGitApiFactory.createGitApi(gitInfo);

    // Verify
    assertThat(gitApi).isNotNull();
    assertThat(gitApi).isInstanceOf(JGitApi.class);

    // Verify the cache and auth strategy were called
    verify(mockAuthStrategyCache).getOrCreate(githubApp.getId());
    verify(mockAuthStrategy).getInstallationToken();
  }

  @Test
  public void testCreateGitApi_WithNullAuthenticationType_UsesTokenFromGitInfo() {
    // Create GitRepositoryInfo with null authenticationType (legacy behavior)
    GitRepositoryInfo gitInfo = new GitRepositoryInfo("https://github.com/test/repo", null,
        "testuser", "legacy-token", SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = null; // Null should use legacy PAT behavior
    gitInfo.authOwnerId = "test-owner-id";

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute
    GitApi gitApi = spyGitApiFactory.createGitApi(gitInfo);

    // Verify uses PAT
    assertThat(gitApi).isNotNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class);
    assertThat(gitApi).hasFieldOrPropertyWithValue("token", "legacy-token".toCharArray());
  }

  @Test
  public void testGetEffectiveUsername_bitbucketCloud_returnsBitbucketTokenAuth() {
    GitRepositoryInfo bitbucketCloudInfo = new GitRepositoryInfo(
        "https://bitbucket.org/workspace/repo",
        null, null, "token", SourceControlProvider.BITBUCKET, "main",
        true, true, true, true, true, true, false, null);

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(bitbucketCloudInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .hasFieldOrPropertyWithValue("username", X_BITBUCKET_API_TOKEN_AUTH);
  }

  @Test
  public void testGetEffectiveUsername_bitbucketCloud_jgit_returnsBitbucketTokenAuth() {
    GitRepositoryInfo bitbucketCloudInfo = new GitRepositoryInfo(
        "https://bitbucket.org/workspace/repo",
        null, null, "token", SourceControlProvider.BITBUCKET, "main",
        true, true, true, true, true, true, false, null);

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(false);

    GitApi gitApi = spyGitApiFactory.createGitApi(bitbucketCloudInfo);

    assertThat(gitApi).isInstanceOf(JGitApi.class);
    assertThat(gitApi).extracting("credentialsProvider")
        .hasFieldOrPropertyWithValue("username", X_BITBUCKET_API_TOKEN_AUTH);
  }

  @Test
  public void testGetEffectiveUsername_bitbucketServer_returnsXAccessToken() {
    GitRepositoryInfo bitbucketServerInfo = new GitRepositoryInfo(
        "https://bitbucket.example.com/scm/project/repo",
        null, null, "token", SourceControlProvider.BITBUCKET, "main",
        true, true, true, true, true, true, false, null);

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(bitbucketServerInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .hasFieldOrPropertyWithValue("username", "x-access-token");
  }

  @Test
  public void testGetEffectiveUsername_nonBitbucket_noUsername_returnsXAccessToken() {
    GitRepositoryInfo githubNoUsernameInfo = new GitRepositoryInfo(
        "https://github.com/org/repo",
        null, null, "token", SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    GitApi gitApi = spyGitApiFactory.createGitApi(githubNoUsernameInfo);

    assertThat(gitApi).isInstanceOf(NativeGitApi.class)
        .hasFieldOrPropertyWithValue("username", "x-access-token");
  }

  /**
   * Helper method to create a test GitHub App
   */
  private GitHubApp createTestGitHubApp(String ownerId) {
    GitHubApp githubApp = new GitHubApp();
    githubApp.setOwnerId(ownerId);
    githubApp.setGithubOrganizationName("test-org");
    githubApp.setAppId(123456);
    githubApp.setInstallationId(789012L);
    githubApp.setSlug("test-github-app");
    githubApp.setClientId("Iv1.test-client-id");
    githubApp.setClientSecret("test-client-secret");
    githubApp.setLastUpdatedAt(new Date());
    // Use a valid 2048-bit RSA private key in PKCS#8 format for testing
    // This is a test-only key generated specifically for GitHub App JWT signing (RS256) - NOT for production use
    githubApp.setPrivateKey(
        "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCVsrVrXls0IWh5\n" +
            "ck+58RCytTi1nByt+YiOgsRQ9kB+Iy4OmTiQq8UjIUQJW/sxC2M9FMucWNmK9btQ\n" +
            "NqoLOay/JvOp5zIrBCjv9MwOyJOvx0QY5Jq2Gq9clA8eY3pOB+b/LdbtMypzi7bq\n" +
            "O5ncq5Wf4f8+8q3qEWj9FADgJTvV0jvItP6eIoZfl12SNWBHGjo0gnaltHr/WI98\n" +
            "KIlMCqYmTTmg1ncoZlN1RnDAJh0C1+QEL40vqTD1m6iEzURA3HG8QQhD4n+z+ofb\n" +
            "rSxfYe+LNpBfngRPzjR+aECYhZZ1W0nMGDv1uYe5G19+nw1x9ZXbjkkKFZ27L4j/\n" +
            "G+TA9R3DAgMBAAECggEAAs285dFTKIkTErM4PVNIyDShQiDsqJV8+4m8A4grcZ8N\n" +
            "6TODJyA1BZEgyaeD7yTuUAaM0tVgT/MX9d00zYWXAhjtO+zRuEo98OUiiK19lp00\n" +
            "y5TX7F7qbnO8Anf6fdujdZ92KVH8AGlteCfhCdWRbGZM48xaDFzLryiXm5sW6qf3\n" +
            "JfSoBR6W9ivd3BliCK7jfnk2y/trzX/1hgBnymgIXHXSk7bNU8EGxCLOdTG+7TKJ\n" +
            "K1ugFkrjrdgSj4FkOo9ckApRs+jNkZkCH9/VxUZsB/HqvJzzi3ytTebrqoNXHLuQ\n" +
            "UKDjGErnL3rLFfMTeW2Gv6p8jMIj2t5DRhYKDRk8AQKBgQDFin0MsAyMCNrM/1r5\n" +
            "goe8r5w52bkbAmdOIDsYOeMmUfO2a75F3awrxGaMUxRMxC1QdO6z2Sr5a0AuNCBq\n" +
            "dHRX5YDyjBOGoWOqX4mtw7EpNkOET02rAm2tOVEIhOOqhwz1VVBKm9Wk4AuhbO+a\n" +
            "wH6njGOoaeplwvpVJO8Wyst0IQKBgQDB/7CAVoopfqsJ6Bsl+rnm8sFiU9yr1U4P\n" +
            "94hcOUhK7f6oyU5SXiOzP1Mx5K850iUyRVCT0CbNyx/Nl1v7iWS1YAqRFPY7jSpZ\n" +
            "fK7zSvcOqFO9O/+/8czRVs09BYm/Go9NoW9zAxFIm6DYnFF5nqnnRGvGNLPo+xpq\n" +
            "uMTZs7CVYwKBgQCShRAPsxz7WS4BU35FB15qw86a0jUMJZI+ToXGiFlFeQ/NxMjS\n" +
            "xYMIy5pMhurNrcz2mmTbHT9U1Qo7uwo4K7yH3YDxZpitCVQFcOuL6VSkfs1BfBjd\n" +
            "uOVk0Nib+wVq3NTtu6PcUw36RvwZddWa8SCAYg8hQb5MUHyhXs3AGBckQQKBgCOz\n" +
            "BavYQPx5zse36qcGiIczTNrnS8hjLEZL6s/typvfR+mPgdYudKtbj9eymXwua6Hg\n" +
            "l39b4ogkROn0XHzhP6MQ1WD1VoqG47Ar/ZXPyb7swtwj2mBcArDTJFmCV2LPZGeI\n" +
            "uZWUju2plePGgEe9Js7kDGEg+ap56taQwci+BFS5AoGBALD//nynCo8oBGqVOBCp\n" +
            "e6X36qLcHE8YkM//FplnhsKPrzqdSXiP2T+BNrzj/rcHdPrA4Js5mggEtXk47/Vk\n" +
            "LoPyDbBvEvkkOnmTjwfmKtFkVykt4q1etctaUyKkzGz6ICKxC73ET/hFlN9r0LXM\n" +
            "JYwq8nvsGtyZSCMRwEVmvb+h\n" +
            "-----END PRIVATE KEY-----");
    return githubApp;
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_UsesAuthOwnerIdWhenAvailable() throws Exception {
    // Setup GitHub App in database for parent owner
    GitHubApp githubApp = createTestGitHubApp("parent-owner-id");
    githubApp = tempEntity.newGitHubApp(githubApp);

    // Mock the auth strategy cache
    GitHubAppAuthStrategy mockAuthStrategy = mock(GitHubAppAuthStrategy.class);
    InstallationToken mockToken = mock(InstallationToken.class);

    when(mockAuthStrategyCache.getOrCreate(githubApp.getId()))
        .thenReturn(mockAuthStrategy);
    when(mockAuthStrategy.getInstallationToken())
        .thenReturn(mockToken);
    when(mockToken.getToken())
        .thenReturn("ghs_mocked_installation_token");

    // Create GitRepositoryInfo with authOwnerId and githubAppId
    GitRepositoryInfo gitInfo = new GitRepositoryInfo(
        "https://github.com/test/repo",
        null, null, null, SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "parent-owner-id";
    gitInfo.githubAppId = githubApp.getId();

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute
    GitApi gitApi = spyGitApiFactory.createGitApi(gitInfo);

    // Verify
    assertThat(gitApi).isNotNull();
    assertThat(gitApi).isInstanceOf(NativeGitApi.class);

    // Verify the cache was called with githubAppId
    verify(mockAuthStrategyCache).getOrCreate(githubApp.getId());
    verify(mockAuthStrategy).getInstallationToken();
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_ThrowsExceptionWhenGithubAppIdIsNull() {
    // Create GitRepositoryInfo with null githubAppId
    GitRepositoryInfo gitInfo = new GitRepositoryInfo(
        "https://github.com/test/repo",
        null, null, null, SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "some-owner-id";
    gitInfo.githubAppId = null; // null - should throw exception

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute and verify exception
    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(gitInfo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub App authentication is configured but no GitHub App ID found");
  }

  @Test
  public void testCreateGitApi_WithGitHubAppAuth_ThrowsExceptionWhenGithubAppIdIsBlank() {
    // Create GitRepositoryInfo with GitHub App auth but githubAppId is blank
    GitRepositoryInfo gitInfo = new GitRepositoryInfo(
        "https://github.com/test/repo",
        null, null, null, SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "some-owner-id";
    gitInfo.githubAppId = "";

    when(spyGitApiFactory.isNativeGitAvailable(null)).thenReturn(true);

    // Execute and verify exception
    assertThatThrownBy(() -> spyGitApiFactory.createGitApi(gitInfo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub App authentication is configured but no GitHub App ID found");
  }

  /**
   * Helper method to create GitRepositoryInfo with GitHub App authentication
   */
  private GitRepositoryInfo createGitInfoWithGitHubApp(String ownerId) {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo("https://github.com/test/repo", null,
        "testuser", "placeholder-token", SourceControlProvider.GITHUB, "main",
        true, true, true, true, true, true, false, null);
    gitInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = ownerId;
    gitInfo.githubAppId = ownerId;
    return gitInfo;
  }
}
