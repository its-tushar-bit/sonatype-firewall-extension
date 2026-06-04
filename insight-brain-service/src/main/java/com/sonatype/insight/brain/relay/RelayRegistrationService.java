/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.relay.dto.RelayRegisterResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateKeyResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateWebhookSecretResponse;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.lifecycle.Managed;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotAuthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.product.util.LicenseContent;

/**
 * Orchestrates SCM webhook relay registration. Entry points short-circuit when the
 * {@link SystemConfigurationPropertyFeature#SCM_RELAY_INTEGRATION} flag is off, leaving
 * existing SCM polling logic untouched. The {@code relayUrl} property mirrors {@code hdsUrl}:
 * defaulted to the production CLM gateway, overridable per deployment.
 */
@Named
@Singleton
public class RelayRegistrationService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(RelayRegistrationService.class);

  private final RelayClient relayClient;

  private final RelayConfigurationDAO relayConfigurationDAO;

  private final LicenseContent licenseContent;

  private final PasswordHandler passwordHandler;

  private final Configuration configuration;

  private final GitHubAppDAO gitHubAppDAO;

  private final TenantUtil tenantUtil;

  @Inject
  public RelayRegistrationService(
      RelayClient relayClient,
      RelayConfigurationDAO relayConfigurationDAO,
      @Nullable LicenseContent licenseContent,
      PasswordHandler passwordHandler,
      Configuration configuration,
      GitHubAppDAO gitHubAppDAO,
      TenantUtil tenantUtil)
  {
    this.relayClient = relayClient;
    this.relayConfigurationDAO = relayConfigurationDAO;
    this.licenseContent = licenseContent;
    this.passwordHandler = passwordHandler;
    this.configuration = configuration;
    this.gitHubAppDAO = gitHubAppDAO;
    this.tenantUtil = tenantUtil;
  }

  /**
   * Returns true if the {@link SystemConfigurationPropertyFeature#SCM_RELAY_INTEGRATION} flag is
   * enabled. {@code relayUrl} is not part of the gate: it defaults to the production CLM
   * gateway (mirroring {@code hdsUrl}) and is admin-overridable.
   */
  public boolean isFeatureGateOpen() {
    return SystemConfigurationPropertyFeature.SCM_RELAY_INTEGRATION.isEnabled();
  }

  /**
   * Returns true if a relay registration row exists. Does not check the feature gate; callers
   * that want the gate-aware view should pair this with {@link #isFeatureGateOpen()}.
   */
  public boolean isRegistered() {
    return relayConfigurationDAO.get() != null;
  }

  /**
   * Returns the persisted relay configuration row or {@code null} if not registered.
   */
  public RelayConfiguration getConfiguration() {
    return relayConfigurationDAO.get();
  }

  /**
   * Returns the App-level webhook URL the customer-facing frontend should display, or
   * {@code null} when it cannot be derived. The relay exposes two distinct base URLs:
   * <ul>
   * <li>{@code relayUrl} — the IQ↔relay API endpoint (typically a Lambda Function URL);
   * used by IQ to call {@code /api/register}, {@code /api/events}, etc.</li>
   * <li>The customer-facing webhook prefix — usually a CDN/proxy in front of the relay
   * (e.g. {@code https://clm-staging.sonatype.com/scm-relay/webhook/...}); the URL
   * SCM providers must POST events to.</li>
   * </ul>
   *
   * <p>
   * The relay returns the customer-facing URL on registration as {@code webhookUrl}. We derive
   * the App-level URL from that prefix. If no registration exists yet (the customer hasn't
   * registered against the relay), we fall back to {@code relayUrl} — which may be the lambda
   * URL but is the only thing IQ knows about; the customer can re-fetch this URL after
   * registering to get the correct CDN-fronted value.
   */
  public String getGitHubAppWebhookUrl() {
    RelayConfiguration cfg = relayConfigurationDAO.get();
    if (cfg != null && StringUtils.isNotBlank(cfg.getWebhookUrl())) {
      String webhookUrl = cfg.getWebhookUrl();
      int webhookIndex = webhookUrl.indexOf("/webhook/");
      if (webhookIndex > 0) {
        return webhookUrl.substring(0, webhookIndex) + "/webhook/github-app";
      }
    }
    String relayUrl = configuration.getRelayUrl();
    if (StringUtils.isBlank(relayUrl)) {
      return null;
    }
    return StringUtils.removeEnd(relayUrl, "/") + "/webhook/github-app";
  }

  @Override
  public void start() {
    registerOnStartup();
  }

  @Override
  public void stop() {
  }

  /**
   * Idempotent register-on-startup hook. No-op when the feature gate is closed or a registration
   * already exists. Failures are logged and swallowed so a relay outage does not break startup.
   *
   * <p>
   * HA note: the check-then-register sequence is racy across nodes booting in parallel.
   * The {@code RelayConfigurationDAO.set} call is transactional so the DB ends up consistent,
   * and the relay's register endpoint is idempotent on license fingerprint, so duplicate calls
   * collapse to one customer record. Worst case is a discarded extra registration response.
   */
  public void registerOnStartup() {
    // Wrap the entire body so any startup failure — including reading the feature flag
    // before its DAO has been wired (e.g. during Spring bean-creation in tests where the
    // static SystemConfigurationPropertyFeature DAO has not been injected yet) — logs and
    // moves on rather than aborting bean wiring. The polling cycle will figure out the
    // actual registration state on its first tick once the application is fully started.
    try {
      // In MTIQ, Managed.start() runs once in the global-tenant context where there is no
      // relay_configuration table; per-tenant register() drives this method instead.
      if (tenantUtil.isMultiTenant() && tenantUtil.isGlobalTenant()) {
        log.debug("Skipping relay registerOnStartup in global tenant context");
        return;
      }
      if (!isFeatureGateOpen()) {
        log.debug("Relay integration gate closed; skipping registerOnStartup");
        return;
      }
      if (isRegistered()) {
        log.debug("Relay configuration already present; skipping registerOnStartup");
        return;
      }
      doRegister();
    }
    catch (RuntimeException e) {
      log.warn("Relay registration on startup failed; will retry on next admin trigger: {}", e.getMessage());
    }
  }

  /**
   * Best-effort tenant deregistration. Decrypts the stored API key, asks the relay to drop the
   * tenant's SQS queue and DynamoDB record, and removes the local {@code relay_configuration}
   * row. Best-effort: the local row drop is attempted regardless of whether the relay returns
   * 404, 5xx, or a network error — a dead relay never blocks tenant deletion. If the local DAO
   * delete itself throws, the failure is logged and swallowed; in the tenant-deletion flow the
   * caller (DeleteTenantsJob) drops the entire schema immediately afterwards, which removes the
   * row. Returns silently if no row exists.
   *
   * <p>
   * Callers outside the tenant-deletion flow MUST NOT rely on the local row being unconditionally
   * removed; if the DAO delete fails the row may persist until the next tenant-deletion attempt.
   *
   */
  public void deregisterTenant() {
    RelayConfiguration configurationRow = relayConfigurationDAO.get();
    if (configurationRow == null) {
      log.debug("No relay_configuration row to deregister; skipping");
      return;
    }
    String apiKey = decryptForDeregister(configurationRow.getApiKey());
    if (apiKey != null) {
      try {
        relayClient.deregister(apiKey);
        log.info("Relay deregistration succeeded (customerId={})", configurationRow.getCustomerId());
      }
      catch (NotFoundException e) {
        log.info("Relay reported customer already gone; dropping local row (customerId={})",
            configurationRow.getCustomerId());
      }
      catch (RuntimeException e) {
        log.warn("Relay deregister failed; dropping local row anyway (customerId={}): {}",
            configurationRow.getCustomerId(), e.getMessage());
      }
    }
    try {
      relayConfigurationDAO.delete();
    }
    catch (RuntimeException e) {
      log.warn("Failed to delete local relay_configuration row: {}", e.getMessage());
    }
  }

  private String decryptForDeregister(String encryptedApiKey) {
    if (StringUtils.isBlank(encryptedApiKey)) {
      return null;
    }
    try {
      return passwordHandler.decryptPassword(encryptedApiKey);
    }
    catch (RuntimeException e) {
      log.warn("Failed to decrypt relay API key for deregister; skipping remote call: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Admin-triggered (re-)registration. Throws on errors so the REST layer can map status codes.
   * If a registration row already exists in PAT mode, the call is treated as an in-place
   * license change: the existing api key is sent as proof-of-possession so that customer_id
   * and webhook_url stay stable. If the existing row is in GitHub App mode (no per-customer
   * webhook URL), the relay's contract requires explicit deregister + fresh register because
   * cross-mode re-registration is rejected with 409.
   */
  public RelayConfiguration registerOnDemand() {
    requireFeatureGateOpen();
    deregisterIfExistingMode(RelayMode.GITHUB_APP);
    byte[] licenseBytes = loadLicenseBytes();
    RelayRegisterResponse response = doRegister(licenseBytes);
    return persist(response);
  }

  /**
   * Admin-triggered GitHub App registration. The webhook secret here is the App-level HMAC secret
   * the customer copied into the GitHub App settings. If the existing row is in PAT mode (has a
   * per-customer webhook URL), the relay's contract requires explicit deregister + fresh register
   * because cross-mode re-registration is rejected with 409.
   */
  public RelayConfiguration registerGitHubAppOnDemand(String installationId, String webhookSecret) {
    requireFeatureGateOpen();
    if (StringUtils.isBlank(installationId)) {
      throw new IllegalArgumentException("installationId is required.");
    }
    deregisterIfExistingMode(RelayMode.PAT);
    // After potential cross-mode deregister, an existing App-mode row means this is the
    // second-or-later App for the same tenant. The relay's contract for that case is
    // "in-place re-registration with X-Relay-Key as proof of possession"; without the
    // header the relay rejects with 401 (ANONYMOUS_LICENSE_REUSED). Surface the existing
    // api key so the new installation_id is added to the customer's installation-index
    // rather than starting a competing customer record.
    String existingApiKey = decryptExistingAppApiKey();
    byte[] licenseBytes = loadLicenseBytes();
    RelayRegisterResponse response = relayClient.registerGitHubApp(licenseBytes, installationId, existingApiKey);
    if (response == null) {
      throw new IllegalStateException("Relay returned an empty registration response.");
    }
    RelayConfiguration cfg = persist(response);
    if (StringUtils.isNotBlank(webhookSecret)) {
      // Two-step: register first (creates customer + queue), then upload the App-level webhook
      // secret. If this second call fails, the customer record on the relay still exists; the
      // relay's /api/register is idempotent for the same license fingerprint + installation
      // id, so the caller can retry without rolling anything back.
      try {
        relayClient.setGitHubAppWebhookSecret(response.getApiKey(), webhookSecret);
      }
      catch (RuntimeException e) {
        log.warn("Relay accepted GitHub App registration but rejected the webhook secret;"
            + " retry the admin call (customerId={}): {}", response.getCustomerId(), e.getMessage());
        throw e;
      }
    }
    return cfg;
  }

  /**
   * Admin-triggered rotation of the IQ→relay api key. Decrypts the current api key, asks the
   * relay to mint a new one, and overwrites only the {@code api_key} column on the existing
   * {@link RelayConfiguration} row — {@code customer_id}, {@code webhook_url},
   * {@code webhook_signing_secret}, and {@code registered_at} stay untouched. The relay
   * keeps the previous key valid for a 5-minute grace window so in-flight polls do not fail.
   *
   * <p>
   * The new plaintext is returned to the caller exactly once so it can be surfaced in the
   * admin UI; it is never logged.
   *
   * <p>
   * Persistence runs after the relay call so a relay failure leaves the local row pointing
   * at the previous (still-valid) key. If persistence itself fails after a successful relay
   * call, both keys remain valid for the relay's grace window so a retry of the rotate call
   * recovers cleanly.
   *
   * @throws IllegalStateException if no relay registration exists.
   * @throws RelayFeatureDisabledException if the relay-integration feature gate is closed.
   */
  public RelayRotateKeyResponse rotateApiKeyOnDemand() {
    requireFeatureGateOpen();
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null) {
      throw new IllegalStateException("No relay registration exists; call register first.");
    }
    String currentApiKey = decryptApiKeyOrThrow(existing);
    RelayRotateKeyResponse response = relayClient.rotateApiKey(currentApiKey);
    if (response == null || StringUtils.isBlank(response.getApiKey())) {
      throw new IllegalStateException("Relay returned an empty rotate-key response.");
    }
    String encryptedNewKey = encrypt(response.getApiKey());
    try {
      existing.setApiKey(encryptedNewKey);
      relayConfigurationDAO.set(existing);
    }
    catch (RuntimeException e) {
      log.error("Persisting rotated relay api key failed; the local row still references the"
          + " previous key. The relay accepted both keys for the 5-minute grace window;"
          + " retry the rotate call (customerId={}): {}", existing.getCustomerId(), e.getMessage());
      throw e;
    }
    log.info("Relay api key rotated (customerId={}, previousKeyExpiresAt={})",
        existing.getCustomerId(), response.getPreviousKeyExpiresAt());
    return response;
  }

  /**
   * Admin-triggered rotation of the per-customer PAT webhook signing secret. Decrypts the
   * current api key (used as proof-of-possession on the relay call), asks the relay to mint a
   * new webhook secret, and overwrites only the {@code webhook_signing_secret} column on the
   * existing {@link RelayConfiguration} row. The relay accepts both old and new signatures
   * during a 5-minute grace window so SCM-side reconfiguration can lag without dropping
   * deliveries.
   *
   * <p>
   * The new plaintext is returned to the caller exactly once so it can be surfaced in the
   * admin UI for pasting into the SCM provider's webhook secret field; it is never logged.
   *
   * @throws IllegalStateException if no relay registration exists.
   * @throws RelayFeatureDisabledException if the relay-integration feature gate is closed.
   */
  public RelayRotateWebhookSecretResponse rotateWebhookSecretOnDemand() {
    requireFeatureGateOpen();
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null) {
      throw new IllegalStateException("No relay registration exists; call register first.");
    }
    String currentApiKey = decryptApiKeyOrThrow(existing);
    RelayRotateWebhookSecretResponse response = relayClient.rotateWebhookSecret(currentApiKey);
    if (response == null || StringUtils.isBlank(response.getWebhookSecret())) {
      throw new IllegalStateException("Relay returned an empty rotate-webhook-secret response.");
    }
    String encryptedNewSecret = encrypt(response.getWebhookSecret());
    try {
      existing.setWebhookSigningSecret(encryptedNewSecret);
      relayConfigurationDAO.set(existing);
    }
    catch (RuntimeException e) {
      log.error("Persisting rotated relay webhook secret failed; the local row still"
          + " references the previous secret. The relay accepted both secrets for the 5-minute"
          + " grace window; retry the rotate call (customerId={}): {}",
          existing.getCustomerId(), e.getMessage());
      throw e;
    }
    log.info("Relay webhook signing secret rotated (customerId={}, previousSecretExpiresAt={})",
        existing.getCustomerId(), response.getPreviousSecretExpiresAt());
    return response;
  }

  private String decryptApiKeyOrThrow(RelayConfiguration cfg) {
    String apiKey = decryptApiKey(cfg);
    if (StringUtils.isBlank(apiKey)) {
      throw new IllegalStateException(
          "Stored relay api key could not be decrypted; cannot authenticate rotate call.");
    }
    return apiKey;
  }

  private enum RelayMode
  {
    PAT,
    GITHUB_APP
  }

  /**
   * Detects the mode of the persisted relay_configuration row from its {@code webhook_url}: PAT
   * mode populates the per-customer URL at registration; App mode leaves it null. If the
   * existing mode equals {@code conflictingMode}, deregister at the relay (best-effort: surface
   * a hard failure so the caller can retry) and drop the local row so the next register call is
   * a fresh anonymous registration.
   */
  private void deregisterIfExistingMode(RelayMode conflictingMode) {
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null) {
      return;
    }
    RelayMode existingMode = StringUtils.isBlank(existing.getWebhookUrl()) ? RelayMode.GITHUB_APP : RelayMode.PAT;
    if (existingMode != conflictingMode) {
      return;
    }
    log.info("Existing {} relay registration detected; deregistering before {} re-register",
        existingMode, conflictingMode == RelayMode.PAT ? RelayMode.GITHUB_APP : RelayMode.PAT);
    doDeregister(existing,
        "Relay deregister failed during cross-mode migration; the prior " + existingMode
            + " registration is still in place. Retry once the relay is reachable.");
    // After a successful cross-mode deregister, the relay no longer routes for any of
    // this customer's previously-registered GitHub App installations. Reset every local
    // github_app row's relay_link_state to UNREGISTERED so the UI badge reflects reality
    // and the App rows are not falsely reported as linked. The polling-cycle pre-flight
    // retry guards against re-flipping the relay back to GitHub App mode (see
    // GitHubAppRelayLinker#tryRegister), so this reset is safe even with active App rows.
    try {
      int reset = gitHubAppDAO.resetRelayLinkStateForAllActive(RelayLinkState.UNREGISTERED);
      if (reset > 0) {
        log.info("Reset relay_link_state on {} GitHub App row(s) to UNREGISTERED after cross-mode deregister",
            reset);
      }
    }
    catch (RuntimeException e) {
      // Don't block the cross-mode register flow on a stale-row cleanup failure; the next
      // register cycle will continue regardless.
      log.warn("Failed to reset relay_link_state on GitHub App rows after cross-mode deregister: {}",
          e.getMessage());
    }
  }

  /**
   * Deregister the locally stored relay registration if one exists. No-op when no row is
   * present. Used by the GitHub App deletion path so a tenant that removes their last App row
   * does not leave an orphan relay-side registration that keeps routing webhooks for a
   * defunct installation.
   *
   * <p>
   * Throws on relay-side failure so the caller can decide whether to retry or surface to the
   * admin; per the existing migration semantics, a half-deregistered state is worse than no
   * deregister at all.
   */
  public void deregisterIfRegistered() {
    requireFeatureGateOpen();
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null) {
      return;
    }
    log.info("Deregistering relay (no GitHub Apps remain locally)");
    doDeregister(existing,
        "Relay deregister failed; the prior registration is still in place. Retry once the relay is reachable.");
  }

  /**
   * Removes a single installation from the relay's installation index without touching the
   * customer record. Called by {@code GitHubAppDeletionService} when a non-last GitHub App is
   * deleted: the customer-wide deregister is too aggressive (tears down queue + remaining
   * installations), and not deregistering at all leaves an orphan index entry that keeps
   * routing webhooks for the deleted installation into the customer's queue.
   *
   * <p>
   * Best-effort: a relay-side failure is logged and swallowed because the local deletion has
   * already completed and a retry path exists (re-running this method, or a later last-App
   * delete that triggers the customer-wide deregister, will re-converge the state).
   */
  public void deleteRelayInstallation(Long installationId) {
    if (installationId == null) {
      return;
    }
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null) {
      return;
    }
    String apiKey = decryptApiKey(existing);
    if (StringUtils.isBlank(apiKey)) {
      log.warn("Skipping relay installation cleanup for installationId={}: api key not available",
          installationId);
      return;
    }
    try {
      relayClient.deleteInstallation(apiKey, Long.toString(installationId));
      log.info("Removed installationId={} from relay installation index", installationId);
    }
    catch (RuntimeException e) {
      log.warn("Relay rejected installation cleanup for installationId={}; the relay-side index "
          + "may still route webhooks for this installation until the next deregister: {}",
          installationId, e.getMessage());
    }
  }

  /**
   * Shared deregister + local-cleanup. Decrypts the api key and calls the relay client; on
   * relay-side failure raises {@link IllegalStateException} with {@code failureMessage}. Drops
   * the local row whether or not the remote call succeeds for an already-rotated key so the
   * subsequent register flow has a clean slate.
   */
  private void doDeregister(RelayConfiguration existing, String failureMessage) {
    String apiKey = decryptApiKey(existing);
    if (StringUtils.isNotBlank(apiKey)) {
      try {
        relayClient.deregister(apiKey);
      }
      catch (RuntimeException e) {
        throw new IllegalStateException(failureMessage, e);
      }
    }
    relayConfigurationDAO.delete();
  }

  private RelayRegisterResponse doRegister(byte[] licenseBytes) {
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing != null) {
      // In-place re-registration / license rotation: send the existing api key as
      // proof-of-possession so the relay keeps customer_id and webhook_url stable.
      String apiKey = decryptApiKey(existing);
      if (StringUtils.isNotBlank(apiKey)) {
        try {
          return relayClient.reRegisterWithApiKey(licenseBytes, apiKey);
        }
        catch (NotAuthorizedException e) {
          // api key was rotated/invalidated on the relay side; fall back to webhook-token recovery.
          log.info("Relay rejected api-key re-registration; falling back to webhook-token recovery");
        }
      }
      // Recovery path: api key gone or rejected. Use the webhook token from the stored URL.
      String token = extractWebhookToken(existing.getWebhookUrl());
      if (StringUtils.isNotBlank(token)) {
        return relayClient.recoverWithWebhookToken(licenseBytes, token);
      }
      log.warn("Relay row exists but neither api key nor webhook token is recoverable; "
          + "attempting fresh registration");
    }
    return relayClient.register(licenseBytes);
  }

  private RelayConfiguration doRegister() {
    byte[] licenseBytes = loadLicenseBytes();
    RelayRegisterResponse response = relayClient.register(licenseBytes);
    return persist(response);
  }

  private String decryptApiKey(RelayConfiguration cfg) {
    if (cfg.getApiKey() == null) {
      return null;
    }
    try {
      return passwordHandler.decryptPassword(cfg.getApiKey());
    }
    catch (RuntimeException e) {
      log.warn("Could not decrypt stored relay api key: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Returns the decrypted api key when an existing App-mode registration row is present;
   * {@code null} otherwise (no row, PAT-mode row, or decryption failed). Used by
   * {@link #registerGitHubAppOnDemand(String, String)} so a same-mode (App → second App)
   * register call carries proof of possession via {@code X-Relay-Key} and is treated by
   * the relay as an in-place re-registration rather than an anonymous duplicate.
   *
   * <p>
   * App-mode rows are identified by a blank {@code webhook_url}: PAT registration is the only
   * path that populates a per-customer URL, so its absence on a present row means the prior
   * registration was for a GitHub App.
   */
  private String decryptExistingAppApiKey() {
    RelayConfiguration existing = relayConfigurationDAO.get();
    if (existing == null || StringUtils.isNotBlank(existing.getWebhookUrl())) {
      return null;
    }
    return decryptApiKey(existing);
  }

  /**
   * Extracts the per-customer webhook token from a stored webhook URL of the form
   * <code>{base}/webhook/{token}/{provider-or-placeholder}</code>. Returns {@code null} when
   * the URL doesn't match.
   */
  @VisibleForTesting
  static String extractWebhookToken(String webhookUrl) {
    if (StringUtils.isBlank(webhookUrl)) {
      return null;
    }
    int idx = webhookUrl.indexOf("/webhook/");
    if (idx < 0) {
      return null;
    }
    String tail = webhookUrl.substring(idx + "/webhook/".length());
    int slash = tail.indexOf('/');
    String token = slash > 0 ? tail.substring(0, slash) : tail;
    return token.isEmpty() ? null : token;
  }

  private RelayConfiguration persist(RelayRegisterResponse response) {
    if (response == null) {
      throw new IllegalStateException("Relay returned an empty registration response.");
    }
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setApiKey(encrypt(response.getApiKey()));
    cfg.setWebhookUrl(response.getWebhookUrl());
    cfg.setWebhookSigningSecret(encrypt(response.getWebhookSecret()));
    cfg.setCustomerId(response.getCustomerId());
    cfg.setRegisteredAt(new Date());
    relayConfigurationDAO.set(cfg);
    log.info("Relay registration stored (customerId={}, webhookUrl={})", response.getCustomerId(),
        response.getWebhookUrl());
    return cfg;
  }

  private void requireFeatureGateOpen() {
    if (!isFeatureGateOpen()) {
      throw new RelayFeatureDisabledException("The SCM webhook relay integration is disabled.");
    }
  }

  private byte[] loadLicenseBytes() {
    if (licenseContent == null) {
      throw new IllegalStateException("No product license is installed; cannot register with the relay.");
    }
    byte[] raw = licenseContent.raw();
    if (raw == null || raw.length == 0) {
      throw new IllegalStateException("No product license is installed; cannot register with the relay.");
    }
    return raw;
  }

  @VisibleForTesting
  String encrypt(String secret) {
    if (secret == null) {
      return null;
    }
    return passwordHandler.encryptPassword(secret);
  }

  /**
   * Thrown when the relay integration is gated off; the REST layer maps this to 412.
   */
  public static class RelayFeatureDisabledException
      extends RuntimeException
  {
    private static final long serialVersionUID = 1L;

    public RelayFeatureDisabledException(String message) {
      super(message);
    }
  }
}
