/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationValidatorTest
{
  @Test
  public void testHasValidCredentials_WithGitHubApp_ValidOwnerId() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "valid-owner-id";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isTrue();
  }

  @Test
  public void testHasValidCredentials_WithGitHubApp_NullOwnerId() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = null;

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }

  @Test
  public void testHasValidCredentials_WithGitHubApp_EmptyOwnerId() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.GITHUB_APP;
    gitInfo.authOwnerId = "";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }

  @Test
  public void testHasValidCredentials_WithPAT_ValidToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.PAT;
    gitInfo.token = "valid-token";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isTrue();
  }

  @Test
  public void testHasValidCredentials_WithPAT_NullToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.PAT;
    gitInfo.token = null;

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }

  @Test
  public void testHasValidCredentials_WithPAT_EmptyToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = AuthenticationType.PAT;
    gitInfo.token = "";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }

  @Test
  public void testHasValidCredentials_WithNullAuthType_ValidToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = null;
    gitInfo.token = "valid-token";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isTrue();
  }

  @Test
  public void testHasValidCredentials_WithNullAuthType_NullToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = null;
    gitInfo.token = null;

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }

  @Test
  public void testHasValidCredentials_WithNullAuthType_EmptyToken() {
    GitRepositoryInfo gitInfo = new GitRepositoryInfo();
    gitInfo.authenticationType = null;
    gitInfo.token = "";

    assertThat(AuthenticationValidator.hasValidCredentials(gitInfo)).isFalse();
  }
}
