/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.security.PrivateKey;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.dataaccess.TransactionContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.git.GitHubAppKeyUtils;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.nexus.scm.github.GitHubApiClient;
import com.sonatype.nexus.scm.github.auth.GitHubAppJwtAuthStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for deleting GitHub App entities and cleaning up associated GitHub installations.
 *
 * <ol>
 * <li>Deletes pending installation state records</li>
 * <li>Attempts to delete the GitHub App installation via GitHub API (if installation ID exists)</li>
 * <li>Deletes the GitHubApp database record</li>
 * </ol>
 *
 */
@Named
@Singleton
public class GitHubAppDeletionService
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppDeletionService.class);

  private static final String DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com";

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppInstallationStateDAO installationStateDAO;

  private final PasswordHandler passwordHandler;

  private final InsightProxy insightProxy;

  private final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache;

  private final String githubApiBaseUrl;

  @Inject
  public GitHubAppDeletionService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache)
  {
    this(gitHubAppDAO, installationStateDAO, passwordHandler, insightProxy, gitHubAppAuthStrategyCache,
        DEFAULT_GITHUB_API_BASE_URL);
  }

  public GitHubAppDeletionService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache,
      final String githubApiBaseUrl)
  {
    this.gitHubAppDAO = gitHubAppDAO;
    this.installationStateDAO = installationStateDAO;
    this.passwordHandler = passwordHandler;
    this.insightProxy = insightProxy;
    this.gitHubAppAuthStrategyCache = gitHubAppAuthStrategyCache;
    this.githubApiBaseUrl = githubApiBaseUrl;
  }

  public void delete(final String ownerId) {
    log.info("Deleting GitHubApp and related data for owner {}", ownerId);

    GitHubApp gitHubApp = gitHubAppDAO.getByOwnerId(ownerId);
    if (gitHubApp == null) {
      log.debug("No GitHubApp found for owner {} during cleanup - may have been deleted already", ownerId);
      return;
    }
    deleteGitHubAppInstallationViaApi(gitHubApp);
    deleteGitHubAppInstallation(gitHubApp);
  }

  public void delete(final GitHubApp gitHubApp) {
    if (gitHubApp == null) {
      log.debug("No GitHubApp provided - may have been deleted already");
      return;
    }
    log.info("Deleting GitHubApp and related data for owner {}", gitHubApp.getOwnerId());
    deleteGitHubAppInstallationViaApi(gitHubApp);
    deleteGitHubAppInstallation(gitHubApp);
  }

  private void deleteGitHubAppInstallation(final GitHubApp gitHubApp) {
    installationStateDAO.deleteByGitHubAppId(gitHubApp.getId());
    gitHubAppDAO.delete(gitHubApp);
  }

  private void deleteGitHubAppInstallationViaApi(final GitHubApp gitHubApp) {
    if (gitHubApp.getInstallationId() == null) {
      log.debug("No installation ID found for GitHubApp {}, skipping API deletion",
          gitHubApp.getId());
      return;
    }

    try {
      log.info("Attempting to delete GitHub App installation {} via API for owner {}",
          gitHubApp.getInstallationId(), gitHubApp.getOwnerId());

      GitHubApiClient apiClient = createGitHubApiClient(gitHubApp);
      apiClient.deleteInstallation(gitHubApp.getInstallationId());
      log.info("Successfully deleted installation from GitHub API");
    }
    catch (Exception e) {
      log.error("Failed to delete GitHub App installation via API for owner", e);
    }
  }

  private GitHubApiClient createGitHubApiClient(final GitHubApp gitHubApp) throws Exception {
    HttpClientUtils.Configuration config = new HttpClientUtils.Configuration();
    config.setServerUrl(githubApiBaseUrl);
    insightProxy.contextualize(config, githubApiBaseUrl);
    GitHubAppJwtAuthStrategy authStrategy = createAuthStrategy(gitHubApp);
    return new GitHubApiClient(config, githubApiBaseUrl, authStrategy);
  }

  private GitHubAppJwtAuthStrategy createAuthStrategy(final GitHubApp gitHubApp) {
    String decryptedBase64Key = passwordHandler.decryptPassword(gitHubApp.getPrivateKey());
    PrivateKey privateKey = GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(decryptedBase64Key);
    return new GitHubAppJwtAuthStrategy(
        privateKey,
        gitHubApp.getAppId().longValue());
  }

  /**
   * Deactivates all GitHub Apps for the given owner.
   *
   * @param ownerId the owner ID whose GitHub Apps should be deactivated
   */
  public void deactivateGitHubApps(final TransactionContext tx, final String ownerId) {
    gitHubAppDAO.deactivateAllForOwner(tx, ownerId);
    gitHubAppAuthStrategyCache.invalidate(ownerId);
    log.debug("Successfully deactivated all GitHub Apps and invalidated cache for owner {}", ownerId);
  }

  /**
   * Deactivates all GitHub Apps for the given owner.
   *
   * @param ownerId the owner ID whose GitHub Apps should be deactivated
   */
  public void deactivateGitHubApps(final String ownerId) {
    gitHubAppDAO.deactivateAllForOwner(ownerId);
    gitHubAppAuthStrategyCache.invalidate(ownerId);
    log.debug("Successfully deactivated all GitHub Apps and invalidated cache for owner {}", ownerId);
  }
}
