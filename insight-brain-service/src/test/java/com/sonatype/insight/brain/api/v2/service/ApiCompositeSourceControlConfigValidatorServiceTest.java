/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestRepositoryValidator;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ValidationResult;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiCompositeSourceControlConfigValidatorServiceTest
{
  private SourceControlUtils sourceControlUtils;

  private GitClientFactory gitClientFactory;

  private PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private ApiCompositeSourceControlConfigValidatorService service;

  @Before
  public void setup() throws Exception {
    sourceControlUtils = mock(SourceControlUtils.class);
    gitClientFactory = mock(GitClientFactory.class);
    pullRequestRepositoryValidator = mock(PullRequestRepositoryValidator.class);
    service = new ApiCompositeSourceControlConfigValidatorService(sourceControlUtils, gitClientFactory,
        pullRequestRepositoryValidator);
  }

  @Test
  public void testValidateSourceControlConfig_ValidApplication() throws Exception {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestRepositoryValidator.isInternalRepository(any())).thenReturn(false);
    when(pullRequestRepositoryValidator.isPrivateRepository(any())).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(true));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getTokenPermissions().isValid()).isTrue();
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
  public void testValidateSourceControlConfig_privateRepo() {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestRepositoryValidator.isInternalRepository(any())).thenReturn(false);
    when(pullRequestRepositoryValidator.isPrivateRepository(any())).thenReturn(false);

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isFalse();
    assertThat(result.getRepoPrivate().getMessage()).isEqualTo("Repository must be private or internal");
    assertThat(result.getTokenPermissions()).isNull();
  }

  @Test
  public void testValidateSourceControlConfig_privateRepoUncheckedException() throws Exception {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestRepositoryValidator.isInternalRepository(any())).thenReturn(false);
    when(pullRequestRepositoryValidator.isPrivateRepository(any()))
        .thenThrow(new UncheckedIOException(new IOException("Unauthorized")));
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(false, "Invalid permissions"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isFalse();
    assertThat(result.getRepoPrivate().getMessage())
        .isEqualTo("Unable to connect to repo: java.io.IOException: Unauthorized");
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage()).isEqualTo("Invalid permissions");
  }

  @Test
  public void testValidateSourceControlConfig_invalidPermissions() throws Exception {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(null, null, null, SourceControlProvider.GITHUB, null, true, true);
    when(sourceControlUtils.getGitRepositoryInfoForApplication(anyString())).thenReturn(gitRepositoryInfo);
    when(pullRequestRepositoryValidator.isInternalRepository(any())).thenReturn(false);
    when(pullRequestRepositoryValidator.isPrivateRepository(any())).thenReturn(true);
    GitApiClient mockClient = mock(GitApiClient.class);
    when(gitClientFactory.createApiClient(any(GitRepositoryInfo.class))).thenReturn(mockClient);
    when(mockClient.validateTokenPermissions()).thenReturn(new ValidationResult(false, "Invalid permissions"));

    ConfigurationValidationResult result = service.validateSourceControlConfig("1234");

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
    assertThat(result.getRepoPrivate().isValid()).isTrue();
    assertThat(result.getTokenPermissions().isValid()).isFalse();
    assertThat(result.getTokenPermissions().getMessage()).isEqualTo("Invalid permissions");
  }
}
