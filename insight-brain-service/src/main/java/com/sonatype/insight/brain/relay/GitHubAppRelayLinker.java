/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper that wraps {@link RelayRegistrationService#registerGitHubAppOnDemand} and persists the
 * resulting {@code relay_link_state} / {@code relay_link_attempts} on the GitHub App row.
 *
 * <p>
 * Three call sites need this exact same success/failure contract:
 * <ul>
 * <li>{@code ApiGitHubAppService.autoRegisterRelayForInstallation} \u2014 the post-install hook.</li>
 * <li>{@code ApiSourceControlService.registerWithRelay} \u2014 the manual admin re-register.</li>
 * <li>{@code RelayPollingService.pollOnce} \u2014 the per-cycle retry loop for {@code UNREGISTERED}/{@code ERROR}.</li>
 * </ul>
 *
 * <p>
 * Centralizing the state machine here keeps the four-state contract consistent and avoids the
 * pre-fix bug where {@code autoRegisterRelayForInstallation} logged failures at WARN and left
 * the relay-side state silently missing.
 */
@Named
@Singleton
public class GitHubAppRelayLinker
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppRelayLinker.class);

  private final RelayRegistrationService relayRegistrationService;

  private final GitHubAppDAO gitHubAppDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public GitHubAppRelayLinker(
      final RelayRegistrationService relayRegistrationService,
      final GitHubAppDAO gitHubAppDAO,
      final PasswordHandler passwordHandler)
  {
    this.relayRegistrationService = relayRegistrationService;
    this.gitHubAppDAO = gitHubAppDAO;
    this.passwordHandler = passwordHandler;
  }

  /**
   * Attempts to register {@code gitHubApp}'s installation with the relay and persists the
   * resulting state on the row. Returns {@code true} on success ({@link RelayLinkState#OK}),
   * {@code false} on failure ({@link RelayLinkState#ERROR} or {@link RelayLinkState#FAILED}).
   *
   * <p>
   * Failures are <em>not</em> rethrown \u2014 the four-state machine is the caller's contract,
   * and rethrowing would force callers to choose between failing the surrounding HTTP call
   * (which they don't want during the post-install auto-registration) and swallowing the
   * exception (the pre-fix bug). The state is the durable signal.
   *
   * <p>
   * The webhook secret stored on the entity is decrypted and forwarded so that an initial
   * attempt that failed before {@code setGitHubAppWebhookSecret} succeeded is fully recovered
   * on retry. Apps with no secret on the entity (registered before the relay integration
   * landed) get a {@code null} secret \u2014 the relay's {@code registerGitHubApp} accepts that\n * and just creates
   * the customer record without rotating the secret.
   */
  public boolean tryRegister(final GitHubApp gitHubApp) {
    return doTryRegister(gitHubApp, /* allowCrossFlip= */false);
  }

  /**
   * Variant of {@link #tryRegister(GitHubApp)} that bypasses the PAT-mode guard. Use only
   * from the post-install hook ({@code ApiGitHubAppService.autoRegisterRelayForInstallation}):
   * an admin who just installed a GitHub App through the IQ UI is explicitly opting into
   * GitHub App mode, so cross-flipping the relay from a previously-auto-registered PAT
   * customer to GitHub App is the desired behaviour, not a silent surprise.
   *
   * <p>
   * The polling-cycle retry loop must keep calling {@link #tryRegister(GitHubApp)} (the
   * cross-flip-blocking variant) so a stale {@code UNREGISTERED} row left over from a
   * deliberate App→PAT migration does not pull the relay back to GitHub App mode behind
   * the admin's back.
   */
  public boolean tryRegisterFromInstall(final GitHubApp gitHubApp) {
    return doTryRegister(gitHubApp, /* allowCrossFlip= */true);
  }

  private boolean doTryRegister(final GitHubApp gitHubApp, final boolean allowCrossFlip) {
    if (gitHubApp == null || gitHubApp.getInstallationId() == null) {
      return false;
    }
    if (!relayRegistrationService.isFeatureGateOpen()) {
      log.debug("Relay feature gate closed; skipping retry for GitHub App {}", gitHubApp.getId());
      return false;
    }
    if (!allowCrossFlip) {
      // Background retry path: do NOT auto-flip the relay back to GitHub App mode if the
      // admin has explicitly migrated to PAT. Cross-mode is a deliberate admin choice; a
      // per-App retry must not silently undo it (registerGitHubAppOnDemand would call
      // deregisterIfExistingMode which tears down the active PAT customer). The github_app
      // row stays at its current link state until the admin either re-registers as GitHub
      // App through the install path or deletes the orphaned row. Install-time callers
      // bypass this guard via tryRegisterFromInstall.
      RelayConfiguration cfg = relayRegistrationService.getConfiguration();
      if (cfg != null && StringUtils.isNotBlank(cfg.getWebhookUrl())) {
        log.debug("Relay is in PAT mode; skipping GitHub App retry for {} to avoid silent cross-mode flip",
            gitHubApp.getId());
        return false;
      }
    }
    final String webhookSecret = decryptWebhookSecret(gitHubApp);
    try {
      relayRegistrationService.registerGitHubAppOnDemand(
          String.valueOf(gitHubApp.getInstallationId()), webhookSecret);
      markSuccess(gitHubApp);
      return true;
    }
    catch (RuntimeException e) {
      log.warn("Relay registration failed for GitHub App {} (installationId={}): {}",
          gitHubApp.getId(), gitHubApp.getInstallationId(), e.getMessage());
      markFailure(gitHubApp);
      return false;
    }
  }

  /**
   * Persists {@link RelayLinkState#OK} and resets the attempt counter on the entity. Visible
   * to call sites that already obtained a successful registration synchronously and just need
   * the success bookkeeping.
   */
  public void markSuccess(final GitHubApp gitHubApp) {
    gitHubApp.setRelayLinkState(RelayLinkState.OK);
    gitHubApp.setRelayLinkAttempts(0);
    persist(gitHubApp);
  }

  /**
   * Increments the attempt counter and transitions to {@link RelayLinkState#ERROR} below the
   * cap or {@link RelayLinkState#FAILED} at the cap. Persists the entity.
   */
  public void markFailure(final GitHubApp gitHubApp) {
    int attempts = gitHubApp.getRelayLinkAttempts() + 1;
    gitHubApp.setRelayLinkAttempts(attempts);
    gitHubApp.setRelayLinkState(
        attempts >= RelayLinkState.MAX_ATTEMPTS ? RelayLinkState.FAILED : RelayLinkState.ERROR);
    persist(gitHubApp);
  }

  private void persist(final GitHubApp gitHubApp) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      gitHubAppDAO.update(tx, gitHubApp);
      tx.commit();
    }
    catch (RuntimeException persistError) {
      // A failure to persist the link-state row is not itself fatal \u2014 the next cycle will
      // re-read the row from the DB and retry. Log loudly because it points at a DB-side
      // problem (the registration itself already succeeded or failed at this point).
      log.warn("Failed to persist relay link state for GitHub App {}: {}",
          gitHubApp.getId(), persistError.getMessage());
    }
  }

  private String decryptWebhookSecret(final GitHubApp gitHubApp) {
    final String encrypted = gitHubApp.getWebhookSecret();
    if (StringUtils.isBlank(encrypted)) {
      return null;
    }
    try {
      return passwordHandler.decryptPassword(encrypted);
    }
    catch (RuntimeException e) {
      log.warn("Failed to decrypt webhook secret for GitHub App {}; retrying without secret: {}",
          gitHubApp.getId(), e.getMessage());
      return null;
    }
  }
}
