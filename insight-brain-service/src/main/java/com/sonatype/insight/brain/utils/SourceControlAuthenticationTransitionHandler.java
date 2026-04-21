/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.dataaccess.TransactionContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Named
@Singleton
public class SourceControlAuthenticationTransitionHandler
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlAuthenticationTransitionHandler.class);

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache;

  private final GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public SourceControlAuthenticationTransitionHandler(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache,
      final GitHubAppDeletionService gitHubAppDeletionService)
  {
    this.gitHubAppDAO = gitHubAppDAO;
    this.gitHubAppAuthStrategyCache = gitHubAppAuthStrategyCache;
    this.gitHubAppDeletionService = gitHubAppDeletionService;
  }

  /**
   * Handles authentication type transitions for source control.
   * - When switching TO GitHub App: validates and activates the specified GitHub App
   * - When switching FROM GitHub App: deactivates all GitHub Apps for the owner
   *
   * @param storedSourceControl the existing source control record from database
   * @param newSourceControl the incoming source control update
   * @param sourceControlDTO the DTO containing githubAppId and other request data
   */
  public void handleAuthTransition(
      final TransactionContext tx,
      final SourceControl storedSourceControl,
      final SourceControl newSourceControl,
      final ApiSourceControlDTO sourceControlDTO)
  {
    AuthenticationType oldAuthType =
        Optional.ofNullable(storedSourceControl).map(SourceControl::getAuthenticationType).orElse(null);
    AuthenticationType newAuthType = newSourceControl.getAuthenticationType();
    String ownerId = newSourceControl.getOwnerId();

    log.debug("Authentication type changing from {} to {} for owner {}",
        oldAuthType, newAuthType, ownerId);

    if (AuthenticationType.GITHUB_APP.equals(newAuthType) && sourceControlDTO.githubAppId != null) {
      GitHubApp gitHubApp = gitHubAppDAO.getByGithubAppId(sourceControlDTO.githubAppId);
      if (gitHubApp == null) {
        throw new NotFoundException("GitHub App not found: " + sourceControlDTO.githubAppId);
      }
      if (!ownerId.equals(gitHubApp.getOwnerId())) {
        throw new BadRequestException("GitHub App does not belong to owner: " + sourceControlDTO.githubAppId);
      }

      newSourceControl.setUsername(null);
      newSourceControl.setToken(null);

      gitHubAppDAO.activateGitHubApp(tx, ownerId, sourceControlDTO.githubAppId);

      gitHubAppAuthStrategyCache.invalidate(ownerId);

      log.info("Activated GitHub App {} for owner {}", sourceControlDTO.githubAppId, ownerId);
    }
    else if (AuthenticationType.GITHUB_APP.equals(oldAuthType) && !AuthenticationType.GITHUB_APP.equals(newAuthType)) {
      gitHubAppDeletionService.deactivateGitHubApps(tx, ownerId);
      log.info("Deactivated all GitHub Apps for owner {} (switching to {})", ownerId, newAuthType);
    }
  }
}
