/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.security.PrivateKey;
import java.util.List;
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
import com.sonatype.insight.brain.relay.RelayRegistrationService;
import com.sonatype.insight.brain.service.InsightProxy;
import jakarta.inject.Provider;
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

  private final GitHubAppSelectionCache gitHubAppSelectionCache;

  // Provider-wrapped to break the eager Guice instantiation graph: RelayRegistrationService
  // depends on RelayClient which requires a populated relayUrl, and tests that exercise
  // unrelated services (e.g. EnterpriseReportingServiceTest) would otherwise fail to provision
  // when no relay config is supplied.
  private final Provider<RelayRegistrationService> relayRegistrationServiceProvider;

  private final String githubApiBaseUrl;

  @Inject
  public GitHubAppDeletionService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache,
      final GitHubAppSelectionCache gitHubAppSelectionCache,
      final Provider<RelayRegistrationService> relayRegistrationServiceProvider)
  {
    this(gitHubAppDAO, installationStateDAO, passwordHandler, insightProxy, gitHubAppAuthStrategyCache,
        gitHubAppSelectionCache, relayRegistrationServiceProvider, DEFAULT_GITHUB_API_BASE_URL);
  }

  public GitHubAppDeletionService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache,
      final GitHubAppSelectionCache gitHubAppSelectionCache,
      final Provider<RelayRegistrationService> relayRegistrationServiceProvider,
      final String githubApiBaseUrl)
  {
    this.gitHubAppDAO = gitHubAppDAO;
    this.installationStateDAO = installationStateDAO;
    this.passwordHandler = passwordHandler;
    this.insightProxy = insightProxy;
    this.gitHubAppAuthStrategyCache = gitHubAppAuthStrategyCache;
    this.gitHubAppSelectionCache = gitHubAppSelectionCache;
    this.relayRegistrationServiceProvider = relayRegistrationServiceProvider;
    this.githubApiBaseUrl = githubApiBaseUrl;
  }

  public void delete(final String ownerId) {
    log.info("Deleting GitHubApp and related data for owner {}", ownerId);

    List<GitHubApp> gitHubApps = gitHubAppDAO.getAllByOwnerId(ownerId);
    if (gitHubApps.isEmpty()) {
      log.debug("No GitHubApp found for owner {} during cleanup - may have been deleted already", ownerId);
      return;
    }
    for (GitHubApp gitHubApp : gitHubApps) {
      deleteGitHubAppInstallationViaApi(gitHubApp);
      deleteRelayInstallationIfRegistered(gitHubApp);
      deleteGitHubAppInstallation(gitHubApp);
      gitHubAppAuthStrategyCache.invalidate(gitHubApp.getId());
    }
    gitHubAppSelectionCache.invalidateAll();
    deregisterRelayIfNoAppsRemain(ownerId);
  }

  public void delete(final GitHubApp gitHubApp) {
    if (gitHubApp == null) {
      log.debug("No GitHubApp provided - may have been deleted already");
      return;
    }
    log.info("Deleting GitHubApp and related data for owner {}", gitHubApp.getOwnerId());
    String ownerId = gitHubApp.getOwnerId();
    deleteGitHubAppInstallationViaApi(gitHubApp);
    deleteRelayInstallationIfRegistered(gitHubApp);
    deleteGitHubAppInstallation(gitHubApp);
    gitHubAppSelectionCache.invalidateAll();
    gitHubAppAuthStrategyCache.invalidate(gitHubApp.getId());
    deregisterRelayIfNoAppsRemain(ownerId);
  }

  /**
   * Removes the App's installation from the relay's installation index before the local row is
   * deleted. The customer-wide deregister fires only on the last-App-removed transition (see
   * {@link #deregisterRelayIfNoAppsRemain}); for non-last deletes the relay would otherwise
   * keep routing webhooks for this installation into the customer's queue, where IQ drops
   * them as unmatched and they pollute the dedup log. Best-effort: a relay-side failure is
   * swallowed inside {@code RelayRegistrationService.deleteRelayInstallation} so the local
   * deletion completes regardless.
   */
  private void deleteRelayInstallationIfRegistered(final GitHubApp gitHubApp) {
    if (relayRegistrationServiceProvider == null || gitHubApp.getInstallationId() == null) {
      return;
    }
    try {
      relayRegistrationServiceProvider.get().deleteRelayInstallation(gitHubApp.getInstallationId());
    }
    catch (RuntimeException e) {
      log.warn("Relay installation cleanup for installationId={} failed; the relay-side index "
          + "may still route webhooks for this installation. Subsequent deregisters will recover.",
          gitHubApp.getInstallationId(), e);
    }
  }

  /**
   * After removing one or more GitHub Apps, deregister the relay registration if it exists and
   * no GitHub Apps remain for the owner. The relay's GitHub App mode routes by installation id;
   * a registered relay with zero local Apps continues to forward webhooks for the (now
   * defunct) installations into the customer's queue, where IQ's poller drains them and counts
   * them as unmatched. Deregistering on the last-App-removed transition is symmetric to
   * registration on the first-App install (auto-register via {@code GitHubAppRelayLinker}).
   *
   * <p>
   * The deregister call is best-effort: a relay-side failure is logged and swallowed so the
   * local deletion remains successful. The next manual {@code POST /sourceControl/relay/deregister}
   * (or the next auto-register after a fresh App install) will re-converge the state.
   */
  private void deregisterRelayIfNoAppsRemain(final String ownerId) {
    if (relayRegistrationServiceProvider == null) {
      return;
    }
    try {
      // IS_ACTIVE filter: getAllByOwnerId returns deactivated rows too (deactivateGitHubApps
      // soft-disables them without deleting). The deregister-on-last-App-removed transition
      // hinges on no ACTIVE App remaining; counting deactivated rows would silently keep the
      // relay-side customer record alive after the last active App is gone, leaving an
      // orphaned customer with no IQ-side route.
      boolean anyActive = gitHubAppDAO.getAllByOwnerId(ownerId).stream().anyMatch(GitHubApp::isActive);
      if (anyActive) {
        return;
      }
      // Only fire the relay-side deregister when the relay is currently registered as a
      // GitHub App customer. After a cross-mode flip (App → PAT) the github_app rows are
      // orphaned: the relay is now routing PAT webhooks for this tenant, and tearing it
      // down because the (already-orphan) App rows are now empty would silently break PAT.
      // The relay's PAT customer is deregistered only via explicit POST /relay/deregister.
      com.sonatype.insight.brain.relay.RelayRegistrationService relayService =
          relayRegistrationServiceProvider.get();
      com.sonatype.insight.brain.model.relay.RelayConfiguration cfg = relayService.getConfiguration();
      if (cfg != null && org.apache.commons.lang3.StringUtils.isNotBlank(cfg.getWebhookUrl())) {
        log.debug("Relay is in PAT mode; skipping last-App deregister for owner {}", ownerId);
        return;
      }
      relayService.deregisterIfRegistered();
    }
    catch (com.sonatype.insight.brain.relay.RelayRegistrationService.RelayFeatureDisabledException e) {
      // Feature gate is closed — there is nothing to deregister on the relay side. Logging
      // a warning here would be misleading noise on every App delete in IQ instances that
      // have never enabled the relay.
      log.debug("Relay deregister skipped after GitHub App deletion for owner {}: feature gate closed", ownerId);
    }
    catch (RuntimeException e) {
      log.warn("Relay deregister after GitHub App deletion for owner {} failed; the relay-side "
          + "registration may still exist. Run POST /sourceControl/relay/deregister to re-converge.",
          ownerId, e);
    }
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
    List<GitHubApp> apps = gitHubAppDAO.getAllByOwnerId(tx, ownerId);
    gitHubAppDAO.deactivateAllForOwner(tx, ownerId);
    gitHubAppSelectionCache.invalidateAll();
    apps.forEach(app -> gitHubAppAuthStrategyCache.invalidate(app.getId()));
    log.debug("Successfully deactivated all GitHub Apps and invalidated caches for owner {}", ownerId);
  }

  /**
   * Deactivates all GitHub Apps for the given owner.
   *
   * @param ownerId the owner ID whose GitHub Apps should be deactivated
   */
  public void deactivateGitHubApps(final String ownerId) {
    List<GitHubApp> apps = gitHubAppDAO.getAllByOwnerId(ownerId);
    gitHubAppDAO.deactivateAllForOwner(ownerId);
    gitHubAppSelectionCache.invalidateAll();
    apps.forEach(app -> gitHubAppAuthStrategyCache.invalidate(app.getId()));
    log.debug("Successfully deactivated all GitHub Apps and invalidated caches for owner {}", ownerId);
  }

  public void reactivateGitHubApps(final TransactionContext tx, final String ownerId) {
    List<GitHubApp> apps = gitHubAppDAO.getAllByOwnerId(tx, ownerId);
    gitHubAppDAO.activateInstalledForOwner(tx, ownerId);
    gitHubAppSelectionCache.invalidateAll();
    apps.forEach(app -> gitHubAppAuthStrategyCache.invalidate(app.getId()));
    log.debug("Reactivated installed GitHub Apps and invalidated caches for owner {}", ownerId);
  }
}
