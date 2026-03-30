/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Named
@Singleton
public class SourceControlAuthenticationTransitionHandler
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlAuthenticationTransitionHandler.class);

  private final GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public SourceControlAuthenticationTransitionHandler(final GitHubAppDeletionService gitHubAppDeletionService) {
    this.gitHubAppDeletionService = gitHubAppDeletionService;
  }

  /**
   * Cleans up authentication data when source control authentication type changes.
   *
   * @param storedSourceControl the existing source control record from database
   * @param newSourceControl the incoming source control update
   */
  public void handleAuthTransition(
      final SourceControl storedSourceControl,
      final SourceControl newSourceControl)
  {
    AuthenticationType oldAuthType =
        Optional.ofNullable(storedSourceControl).map(SourceControl::getAuthenticationType).orElse(null);
    AuthenticationType newAuthType = newSourceControl.getAuthenticationType();

    log.debug("Authentication type changing from {} to {} for owner {}",
        oldAuthType, newAuthType, newSourceControl.getOwnerId());

    if (AuthenticationType.PAT.equals(oldAuthType) && AuthenticationType.GITHUB_APP.equals(newAuthType)) {
      clearPatToken(newSourceControl);
    }
    if (!AuthenticationType.GITHUB_APP.equals(newAuthType)) {
      deleteGitHubAppInstallation(newSourceControl);
    }
  }

  private void clearPatToken(final SourceControl sourceControl) {
    log.debug("Clearing PAT token for owner {}", sourceControl.getOwnerId());
    sourceControl.setToken(null);
  }

  public void deleteGitHubAppInstallation(final SourceControl sourceControl) {
    gitHubAppDeletionService.delete(sourceControl.getOwnerId());
  }
}
