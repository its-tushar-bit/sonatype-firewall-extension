/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.google.common.base.Strings;

/**
 * Validates authentication credentials based on authentication type.
 */
public final class AuthenticationValidator
{
  private AuthenticationValidator() {
    // Utility class - prevent instantiation
  }

  /**
   * Validates that authentication credentials are present.
   * For PAT authentication, checks token. For GitHub App authentication, checks authOwnerId.
   *
   * @param gitRepoInfo the GitRepositoryInfo to validate
   * @return true if required credentials are present, false otherwise
   * @throws IllegalArgumentException if authentication type is unknown
   */
  public static boolean hasValidCredentials(final GitRepositoryInfo gitRepoInfo) {
    if (AuthenticationType.PAT.equals(gitRepoInfo.authenticationType) || gitRepoInfo.authenticationType == null) {
      return !Strings.isNullOrEmpty(gitRepoInfo.token);
    }
    else if (AuthenticationType.GITHUB_APP.equals(gitRepoInfo.authenticationType)) {
      return !Strings.isNullOrEmpty(gitRepoInfo.authOwnerId);
    }
    else {
      throw new IllegalArgumentException("Unknown authentication type: " + gitRepoInfo.authenticationType);
    }
  }
}
