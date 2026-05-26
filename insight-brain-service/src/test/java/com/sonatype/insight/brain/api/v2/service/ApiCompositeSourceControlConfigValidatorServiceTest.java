/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.SourceControlSshService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.scm.InvalidRepositoryUrlException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ValidationResult;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.After;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiCompositeSourceControlConfigValidatorServiceTest
{
  public static final String SSH_REPOSITORY_URL = "git@localhost:org/repo.git";

  private SourceControlUtils sourceControlUtils;

  private GitClientFactory gitClientFactory;

  private GitApiFactory gitApiFactory;

  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  private ApiCompositeSourceControlConfigValidatorService service;

  private SourceControlSshService sourceControlSshService;

  @Before
  public void setup() {
    sourceControlUtils = mock(SourceControlUtils.class);
    gitClientFactory = mock(GitClientFactory.class);
    gitApiFactory = mock(GitApiFactory.class);
    mockScmRepoVisibilityService = mock(ScmRepoVisibilityService.class);
    sourceControlSshService = mock(SourceControlSshService.class);
    service = new ApiCompositeSourceControlConfigValidatorService(sourceControlUtils, gitClientFactory,
        gitApiFactory, mockScmRepoVisibilityService, sourceControlSshService);
  }

  @Before
  public void bindSecurityManager() {
    SecurityAspectControl.disableEnforcement();
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    SimplePrincipalCollection principals = new SimplePrincipalCollection("admin", "testRealm");
    Subject subject = new Subject.Builder(securityManager)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(subject);
  }

  @After
  public void unbindSecurityManager() {
    SecurityAspectControl.enableEnforcement();
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void testValidateSourceControlConfig_validApplication() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
    assertThat(result.getSshConfiguration()).isNull();
  }

  @Test
  public void testValidateSourceControlConfig_validApplication_withSsh() throws Exception {
    // given a git repo with SSH configured fully
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    // and clone succeeds
    GitApi mockApi = mock(NativeGitApi.class);
    when(gitApiFactory.createGitApi(any(GitRepositoryInfo.class))).thenReturn(mockApi);
    when(mockApi.cloneOrPullRepository(any(File.class), anyString())).thenReturn("headRef");
    File repoDirectory = mock(File.class);
    when(sourceControlUtils.getCheckoutDirectory(anyString())).thenReturn(repoDirectory);

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then everything is a success and SSH is populated
    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
    assertThat(result.getSshConfiguration().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_incompleteConfiguration() {
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(null);

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isFalse();
    assertThat(result.getConfigurationComplete().getMessage()).isEqualTo("Some required values are missing or unsaved");
    assertThat(result.getRepoPrivate()).isNull();
    assertThat(result.getTokenPermissions()).isNull();
  }

  @Test
  public void testValidateSourceControlConfig_privateRepo() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_publicRepo() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo))).thenReturn(false);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isFalse();
    assertThat(result.getRepoPrivate().getMessage()).isEqualTo("Repository must be private or internal to enable all" +
        " SCM features. Support for public repositories is limited.");
    assertThat(result.getTokenPermissions().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_privateRepoException() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo("*/target");
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo)))
        .thenThrow(new UncheckedIOException(new IOException("Unauthorized")));
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(false, "Invalid permissions"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isFalse();
    assertThat(result.getRepoPrivate().getMessage()).isEqualTo("Unable to determine if repository is private.");
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getRepoPublic().getMessage()).isEqualTo("Unable to determine if repository is public.");
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage()).isEqualTo("Invalid permissions");
  }

  @Test
  public void testValidateSourceControlConfig_invalidPermissions() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo("*/target");
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(false, "Invalid permissions"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage()).isEqualTo("Invalid permissions");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_cloneFails() throws Exception {
    // given a git repo with SSH configured fully
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));
    File repoDirectory = mock(File.class);
    when(sourceControlUtils.getCheckoutDirectory(anyString())).thenReturn(repoDirectory);

    // and clone fails with a Git Exception
    GitApi mockApi = mock(NativeGitApi.class);
    when(gitApiFactory.createGitApi(any(GitRepositoryInfo.class))).thenReturn(mockApi);
    when(mockApi.cloneOrPullRepository(any(File.class), anyString())).thenThrow(new GitException("git error"));

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then early config are valid
    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();

    // and SSH has an error
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    // The error message should not contain the low level exception message - in this case "git error".
    // See https://sonatype.atlassian.net/browse/CLM-29901.
    assertThat(result.getSshConfiguration().getMessage())
        .isEqualTo("Unable to clone a repository using SSH, check that "
            + "your SSH keys are configured properly and available to IQ.");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_noNativeGit() throws Exception {
    // given a git repo with SSH configured fully
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));
    File repoDirectory = mock(File.class);
    when(sourceControlUtils.getCheckoutDirectory(anyString())).thenReturn(repoDirectory);

    // and GitApi is not NativeGitApi
    GitApi mockApi = mock(GitApi.class);
    when(gitApiFactory.createGitApi(any(GitRepositoryInfo.class))).thenReturn(mockApi);

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then SSH has an error
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    assertThat(result.getSshConfiguration().getMessage()).contains("SSH requires native git");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_noSshUrl() throws Exception {
    // given a git repo with SSH enabled but no SSH URL
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = null;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then it attempted to retrieve the SSH URL
    verify(sourceControlSshService, times(1)).verifySshUrlAndUpdateIfNeeded(anyString());

    // then SSH has an error
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    assertThat(result.getSshConfiguration().getMessage()).contains("Unable to determine the SSH URL");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_populatesSsh() throws Exception {
    // given a git repo with SSH enabled but no SSH URL, and returns SSH URL when called a second time
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = null;
    gitRepositoryInfo.sshEnabled = true;
    GitRepositoryInfo updatedRepoInfo = getGitRepositoryInfo(null);
    updatedRepoInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString()))
        .thenReturn(gitRepositoryInfo)
        .thenReturn(updatedRepoInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then it attempted to retrieve the SSH URL
    verify(sourceControlSshService, times(1)).verifySshUrlAndUpdateIfNeeded(anyString());

    // then SSH has an error which indicates that it moved on to the next error state
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    assertThat(result.getSshConfiguration().getMessage()).contains("SSH requires native git");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_unknownError() throws Exception {
    // given a git repo with SSH configured fully
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));
    File repoDirectory = mock(File.class);
    when(sourceControlUtils.getCheckoutDirectory(anyString())).thenReturn(repoDirectory);

    // and clone fails with a non-git Exception
    GitApi mockApi = mock(NativeGitApi.class);
    when(gitApiFactory.createGitApi(any(GitRepositoryInfo.class))).thenReturn(mockApi);
    when(mockApi.cloneOrPullRepository(any(File.class), anyString())).thenThrow(new NullPointerException("some error"));

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then early config are valid
    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();

    // and SSH has an error
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    // The error message should not contain the low level exception message - in this case "some error".
    // See https://sonatype.atlassian.net/browse/CLM-29901.
    assertThat(result.getSshConfiguration().getMessage())
        .isEqualTo("Unable to clone a repository using SSH, check that "
            + "your SSH keys are configured properly and available to IQ.");
  }

  @Test
  public void testValidateSourceControlConfig_ssh_illegalArgException() throws Exception {
    // given a git repo with SSH configured fully
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    gitRepositoryInfo.sshRepositoryUrl = SSH_REPOSITORY_URL;
    gitRepositoryInfo.sshEnabled = true;
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);

    // and the repo is configured successfully
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));
    File repoDirectory = mock(File.class);
    when(sourceControlUtils.getCheckoutDirectory(anyString())).thenReturn(repoDirectory);

    // and createGitApi fails with an Illegal Argument Exception
    when(gitApiFactory.createGitApi(any(GitRepositoryInfo.class)))
        .thenThrow(new IllegalArgumentException("Illegal Argument"));

    // when we try to validate
    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    // then early config are valid
    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();

    // and SSH has an error
    assertThat(result.getSshConfiguration().isValid()).isFalse();
    // The error message should not contain the low level exception message - in this case "Illegal Argument".
    // See https://sonatype.atlassian.net/browse/CLM-29901.
    assertThat(result.getSshConfiguration().getMessage())
        .isEqualTo("Unable to clone a repository using SSH, check that "
            + "your SSH keys are configured properly and available to IQ.");
  }

  @Test
  public void testValidateSourceControlConfig_privateRepo_and_allowedOnPublicRepositories() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);
    when(mockScmRepoVisibilityService.isScmAllowedOnPublicRepositories()).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getRepoPublic().isValid()).isFalse();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_publicRepo_and_allowedOnPublicRepositories() throws IOException {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(false);
    when(mockScmRepoVisibilityService.isScmAllowedOnPublicRepositories()).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isFalse();
    assertThat(result.getRepoPublic().isValid()).isTrue();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_tokenPermissions_unauthorized() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenThrow(new HttpResponseException(401, "Unauthorized"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage())
        .isEqualTo("Authentication failed. Please verify your credentials.");
  }

  @Test
  public void testValidateSourceControlConfig_tokenPermissions_forbidden() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenThrow(new HttpResponseException(403, "Forbidden"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage())
        .isEqualTo("Insufficient permissions. Please verify the token has the required scopes.");
  }

  @Test
  public void testValidateSourceControlConfig_tokenPermissions_invalidRepositoryUrl() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions())
        .thenThrow(new InvalidRepositoryUrlException("https://bad..url/repo"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage())
        .isEqualTo("Unable to validate the repository URL. Please verify the URL and credentials are correct.");
  }

  @Test
  public void testValidateSourceControlConfig_createApiClient_invalidRepositoryUrl() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class)))
        .thenThrow(new InvalidRepositoryUrlException("https://bad..url/repo"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage())
        .isEqualTo("Unable to validate the repository URL. Please verify the URL and credentials are correct.");
  }

  @Test
  public void testValidateSourceControlConfig_tokenPermissions_unexpectedHttpStatus() throws Exception {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfo(null);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenThrow(new HttpResponseException(500, "Internal Server Error"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage())
        .isEqualTo("Unable to validate permissions due to an unexpected server response.");
  }

  private GitRepositoryInfo getGitRepositoryInfo(String scanTarget) {
    return new GitRepositoryInfo(null, null, null, null, SourceControlProvider.GITHUB, "main", true, true, true, true,
        true,
        true, false, scanTarget);
  }
}
