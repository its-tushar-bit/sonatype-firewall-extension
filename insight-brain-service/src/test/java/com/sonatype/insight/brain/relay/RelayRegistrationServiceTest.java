/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.relay.dto.RelayRegisterResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateKeyResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateWebhookSecretResponse;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.sonatype.licensing.product.util.LicenseContent;

import jakarta.ws.rs.NotAuthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RelayRegistrationServiceTest
{
  @Mock
  private RelayClient relayClient;

  @Mock
  private RelayConfigurationDAO relayConfigurationDAO;

  @Mock
  private LicenseContent licenseContent;

  @Mock
  private PasswordHandler passwordHandler;

  @Mock
  private Configuration configuration;

  @Mock
  private com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO gitHubAppDAO;

  private RelayRegistrationService service;

  @Before
  public void before() {
    service = new RelayRegistrationService(
        relayClient, relayConfigurationDAO, licenseContent, passwordHandler, configuration, gitHubAppDAO);
    Mockito.lenient()
        .when(passwordHandler.encryptPassword(Mockito.anyString()))
        .thenAnswer(inv -> "enc-" + inv.<String>getArgument(0));
    Mockito.lenient().when(licenseContent.raw()).thenReturn("license-bytes".getBytes());
    // Default: feature flag off (mock returns null property -> enabledWhenAbsent=false -> false).
    SystemConfigurationPropertyDAO sysDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    when(sysDao.createTransactionContext()).thenReturn(tx);
    SystemConfigurationPropertyFeature.injectDependencies(sysDao);
  }

  @After
  public void cleanUpStaticState() {
    SystemConfigurationPropertyFeature.injectDependencies(null);
  }

  @Test
  public void registerOnStartup_flagOff_isNoop() {
    service.registerOnStartup();
    verify(relayClient, never()).register(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void registerOnStartup_alreadyRegistered_isNoop() {
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(new RelayConfiguration());

    service.registerOnStartup();

    verify(relayClient, never()).register(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void registerOnStartup_swallowsClientFailures() {
    enableFeature();
    when(relayClient.register(any())).thenThrow(new RuntimeException("relay down"));

    service.registerOnStartup(); // must not throw

    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void registerOnDemand_persistsEncryptedSecrets() {
    enableFeature();
    when(relayClient.register(any())).thenReturn(response("api-key-1", "https://relay.example/webhook/abc/github",
        "signing-1", "cust-1"));

    service.registerOnDemand();

    ArgumentCaptor<RelayConfiguration> captor = ArgumentCaptor.forClass(RelayConfiguration.class);
    verify(relayConfigurationDAO).set(captor.capture());
    RelayConfiguration persisted = captor.getValue();
    assertThat(persisted.getApiKey()).isEqualTo("enc-api-key-1");
    assertThat(persisted.getWebhookSigningSecret()).isEqualTo("enc-signing-1");
    assertThat(persisted.getWebhookUrl()).isEqualTo("https://relay.example/webhook/abc/github");
    assertThat(persisted.getCustomerId()).isEqualTo("cust-1");
    assertThat(persisted.getRegisteredAt()).isNotNull();
  }

  @Test
  public void registerOnDemand_existingRow_usesApiKeyForReRegistration() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old")).thenReturn("old-key");
    when(relayClient.reRegisterWithApiKey(any(), Mockito.eq("old-key")))
        .thenReturn(response("new-key", "https://relay.example/webhook/tok-1/github", "new-sec", "cust-1"));

    service.registerOnDemand();

    verify(relayClient).reRegisterWithApiKey("license-bytes".getBytes(), "old-key");
    verify(relayClient, never()).register(any());
    verify(relayClient, never()).recoverWithWebhookToken(any(), any());
    verify(relayConfigurationDAO).set(any());
  }

  @Test
  public void registerOnDemand_existingRow_apiKeyRejected_fallsBackToWebhookToken() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old")).thenReturn("old-key");
    when(relayClient.reRegisterWithApiKey(any(), any())).thenThrow(new NotAuthorizedException("rotated"));
    when(relayClient.recoverWithWebhookToken(any(), Mockito.eq("tok-1")))
        .thenReturn(response("new-key", "https://relay.example/webhook/tok-1/github", "new-sec", "cust-1"));

    service.registerOnDemand();

    verify(relayClient).reRegisterWithApiKey(any(), Mockito.eq("old-key"));
    verify(relayClient).recoverWithWebhookToken("license-bytes".getBytes(), "tok-1");
    verify(relayClient, never()).register(any());
  }

  @Test
  public void registerOnDemand_existingRow_apiKeyDecryptFails_usesWebhookToken() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-broken");
    existing.setWebhookUrl("https://relay.example/webhook/tok-2/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-broken")).thenThrow(new RuntimeException("bad cipher"));
    when(relayClient.recoverWithWebhookToken(any(), any()))
        .thenReturn(response("new-key", "https://relay.example/webhook/tok-2/github", "new-sec", "cust-1"));

    service.registerOnDemand();

    verify(relayClient, never()).reRegisterWithApiKey(any(), any());
    verify(relayClient).recoverWithWebhookToken("license-bytes".getBytes(), "tok-2");
    verify(relayClient, never()).register(any());
  }

  @Test
  public void registerOnDemand_existingRow_noTokenRecoverable_fallsBackToFreshRegister() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey(null);
    existing.setWebhookUrl(null);
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(relayClient.register(any()))
        .thenReturn(response("new-key", "https://relay.example/webhook/tok-3/github", "new-sec", "cust-1"));

    service.registerOnDemand();

    verify(relayClient, never()).reRegisterWithApiKey(any(), any());
    verify(relayClient, never()).recoverWithWebhookToken(any(), any());
    verify(relayClient).register("license-bytes".getBytes());
  }

  @Test
  public void registerOnDemand_noRow_usesFreshRegister() {
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(null);
    when(relayClient.register(any()))
        .thenReturn(response("k", "https://relay.example/webhook/abc/github", "s", "c"));

    service.registerOnDemand();

    verify(relayClient).register("license-bytes".getBytes());
    verify(relayClient, never()).reRegisterWithApiKey(any(), any());
    verify(relayClient, never()).recoverWithWebhookToken(any(), any());
  }

  @Test
  public void extractWebhookToken_returnsTokenForValidUrl() {
    assertThat(RelayRegistrationService.extractWebhookToken("https://relay.example/webhook/abc-123/github"))
        .isEqualTo("abc-123");
    assertThat(RelayRegistrationService.extractWebhookToken("https://relay.example/webhook/tok/x"))
        .isEqualTo("tok");
    assertThat(RelayRegistrationService.extractWebhookToken("https://relay.example/base/webhook/tok/github"))
        .isEqualTo("tok");
  }

  @Test
  public void extractWebhookToken_returnsNullWhenNoMatch() {
    assertThat(RelayRegistrationService.extractWebhookToken(null)).isNull();
    assertThat(RelayRegistrationService.extractWebhookToken("")).isNull();
    assertThat(RelayRegistrationService.extractWebhookToken("   ")).isNull();
    assertThat(RelayRegistrationService.extractWebhookToken("https://relay.example/no-match")).isNull();
    assertThat(RelayRegistrationService.extractWebhookToken("https://relay.example/webhook/")).isNull();
  }

  @Test
  public void registerOnDemand_flagOff_throws() {
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.registerOnDemand());
    verify(relayClient, never()).register(any());
  }

  @Test
  public void registerOnDemand_relayRejects401_propagatesNotAuthorized() {
    enableFeature();
    when(relayClient.register(any())).thenThrow(new NotAuthorizedException("invalid license"));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> service.registerOnDemand());
  }

  @Test
  public void registerGitHubAppOnDemand_callsRegisterThenSetSecret() {
    enableFeature();
    when(relayClient.registerGitHubApp(any(), any(), any())).thenReturn(
        response("api-key-2", null, null, "cust-2"));

    service.registerGitHubAppOnDemand("42", "hmac-secret");

    ArgumentCaptor<RelayConfiguration> captor = ArgumentCaptor.forClass(RelayConfiguration.class);
    verify(relayConfigurationDAO).set(captor.capture());
    assertThat(captor.getValue().getApiKey()).isEqualTo("enc-api-key-2");
    assertThat(captor.getValue().getCustomerId()).isEqualTo("cust-2");
    verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("42"), any());
    verify(relayClient).setGitHubAppWebhookSecret("api-key-2", "hmac-secret");
  }

  @Test
  public void registerGitHubAppOnDemand_blankSecret_skipsSetSecret() {
    enableFeature();
    when(relayClient.registerGitHubApp(any(), any(), any())).thenReturn(
        response("api-key-2", null, null, "cust-2"));

    service.registerGitHubAppOnDemand("42", "");

    verify(relayConfigurationDAO).set(any());
    verify(relayClient, never()).setGitHubAppWebhookSecret(any(), any());
  }

  @Test
  public void registerGitHubAppOnDemand_setSecretFailure_propagatesAndDoesNotRollback() {
    enableFeature();
    when(relayClient.registerGitHubApp(any(), any(), any())).thenReturn(
        response("api-key-2", null, null, "cust-2"));
    Mockito.doThrow(new NotAuthorizedException("bad key"))
        .when(relayClient)
        .setGitHubAppWebhookSecret(any(), any());

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> service.registerGitHubAppOnDemand("42", "hmac"));

    // Local registration row was still persisted before the secret-set call; the relay-side
    // customer record also still exists. Re-registration is idempotent so callers can retry.
    verify(relayConfigurationDAO).set(any());
  }

  @Test
  public void registerGitHubAppOnDemand_flagOff_throws() {
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.registerGitHubAppOnDemand("42", "hmac-secret"));
  }

  @Test
  public void registerGitHubAppOnDemand_blankInstallationId_throws() {
    enableFeature();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> service.registerGitHubAppOnDemand("", "hmac-secret"));
  }

  @Test
  public void registerGitHubAppOnDemand_existingPatRegistration_deregistersFirst() {
    // Migrate-first contract: an existing PAT row (webhook_url populated) must be deregistered
    // at the relay AND dropped locally before we call the GitHub App register path. Cross-mode
    // re-registration would otherwise be rejected by the relay (401 anonymous, 409 with key).
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-pat-api-key");
    existing.setWebhookUrl("https://relay.example/webhook/old-token/{provider}");
    existing.setCustomerId("cust-old");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old-pat-api-key")).thenReturn("plain-old-pat-api-key");
    when(relayClient.registerGitHubApp(any(), any(), any())).thenReturn(
        response("api-key-app", null, null, "cust-new-app"));

    service.registerGitHubAppOnDemand("42", "hmac");

    InOrder order = inOrder(relayClient, relayConfigurationDAO);
    order.verify(relayClient).deregister("plain-old-pat-api-key");
    order.verify(relayConfigurationDAO).delete();
    order.verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("42"), any());
    order.verify(relayConfigurationDAO).set(any());
  }

  @Test
  public void registerGitHubAppOnDemand_existingGitHubAppRow_doesNotDeregister() {
    // Same-mode re-registration: an existing GitHub App row (no webhook_url) must NOT trigger
    // a deregister; same-mode re-register is accepted by the relay as a license-fingerprint
    // match.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-app-api-key");
    existing.setWebhookUrl(null);
    existing.setCustomerId("cust-old");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old-app-api-key")).thenReturn("plain-old-app-api-key");
    when(relayClient.registerGitHubApp(any(), any(), any())).thenReturn(
        response("api-key-app", null, null, "cust-new-app"));

    service.registerGitHubAppOnDemand("42", "hmac");

    verify(relayClient, never()).deregister(any());
    verify(relayConfigurationDAO, never()).delete();
    verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("42"), any());
  }

  @Test
  public void registerGitHubAppOnDemand_existingGitHubAppRow_passesExistingApiKey() {
    // Same-mode (App → second App) register: relay requires X-Relay-Key as proof of
    // possession; without it the relay rejects with 401 (ANONYMOUS_LICENSE_REUSED).
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-app-api-key");
    existing.setWebhookUrl(null);
    existing.setCustomerId("cust-old");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old-app-api-key")).thenReturn("plain-old-app-api-key");
    when(relayClient.registerGitHubApp(any(), any(), eq("plain-old-app-api-key"))).thenReturn(
        response("api-key-app2", null, null, "cust-old"));

    service.registerGitHubAppOnDemand("99", "hmac");

    verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("99"), eq("plain-old-app-api-key"));
  }

  @Test
  public void registerGitHubAppOnDemand_noExistingRow_passesNullApiKey() {
    // First-ever register: no existing row, so no api key is available for X-Relay-Key.
    // The relay accepts a fresh anonymous register when the license fingerprint is also new.
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(null);
    when(relayClient.registerGitHubApp(any(), any(), eq((String) null))).thenReturn(
        response("api-key-app", null, null, "cust-fresh"));

    service.registerGitHubAppOnDemand("42", "hmac");

    verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("42"), eq((String) null));
  }

  @Test
  public void registerGitHubAppOnDemand_existingAppRow_decryptFails_passesNullApiKey() {
    // Defensive: if the stored api key cannot be decrypted (e.g. cipher migration mid-flight),
    // fall through to anonymous register rather than blocking the call. The relay will
    // reject with 401 if the license is already known, which is observably better than a
    // hard local failure that prevents recovery.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-broken");
    existing.setWebhookUrl(null);
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-broken")).thenThrow(new RuntimeException("bad cipher"));
    when(relayClient.registerGitHubApp(any(), any(), eq((String) null))).thenReturn(
        response("api-key-app", null, null, "cust-x"));

    service.registerGitHubAppOnDemand("42", "hmac");

    verify(relayClient).registerGitHubApp(eq("license-bytes".getBytes()), eq("42"), eq((String) null));
  }

  @Test
  public void registerGitHubAppOnDemand_deregisterFails_doesNotProceed() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-pat-api-key");
    existing.setWebhookUrl("https://relay.example/webhook/old-token/{provider}");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old-pat-api-key")).thenReturn("plain-old-pat-api-key");
    Mockito.doThrow(new RuntimeException("relay down")).when(relayClient).deregister(any());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.registerGitHubAppOnDemand("42", "hmac"))
        .withMessageContaining("Relay deregister failed during cross-mode migration");

    verify(relayClient, never()).registerGitHubApp(any(), any());
    verify(relayConfigurationDAO, never()).delete();
  }

  @Test
  public void registerOnDemand_existingGitHubAppRow_deregistersFirst() {
    // Reverse direction: PAT register over an existing GitHub App row must deregister first.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-app-api-key");
    existing.setWebhookUrl(null); // App mode
    existing.setCustomerId("cust-old");
    when(relayConfigurationDAO.get())
        .thenReturn(existing) // first read: deregisterIfExistingMode
        .thenReturn(null); // second read: doRegister sees fresh state
    when(passwordHandler.decryptPassword("enc-old-app-api-key")).thenReturn("plain-old-app-api-key");
    when(relayClient.register(any())).thenReturn(
        response("api-key-pat", "https://relay.example/webhook/new/{provider}", "signing", "cust-new-pat"));

    service.registerOnDemand();

    InOrder order = inOrder(relayClient, relayConfigurationDAO);
    order.verify(relayClient).deregister("plain-old-app-api-key");
    order.verify(relayConfigurationDAO).delete();
    order.verify(relayClient).register(any());
    order.verify(relayConfigurationDAO).set(any());
  }

  @Test
  public void registerOnDemand_crossModeFromGitHubApp_resetsLocalAppLinkStates() {
    // Cross-mode flip App → PAT must reset every local github_app row's relay_link_state
    // to UNREGISTERED. The relay deregister tears down the App customer + installation index
    // entries, so leaving link_state=OK on local rows would mislead the UI badge into
    // showing a non-existent link.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-app-api-key");
    existing.setWebhookUrl(null);
    existing.setCustomerId("cust-old");
    when(relayConfigurationDAO.get())
        .thenReturn(existing)
        .thenReturn(null);
    when(passwordHandler.decryptPassword("enc-old-app-api-key")).thenReturn("plain-old-app-api-key");
    when(relayClient.register(any())).thenReturn(
        response("api-key-pat", "https://relay.example/webhook/new/{provider}", "signing", "cust-new-pat"));

    service.registerOnDemand();

    verify(gitHubAppDAO).resetRelayLinkStateForAllActive(
        com.sonatype.insight.brain.model.githubapp.RelayLinkState.UNREGISTERED);
  }

  @Test
  public void registerOnDemand_crossModeReset_dbFailureIsSwallowed() {
    // Stale-row cleanup is best-effort; a DAO error must not abort an otherwise-successful
    // cross-mode register.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old-app-api-key");
    existing.setWebhookUrl(null);
    when(relayConfigurationDAO.get())
        .thenReturn(existing)
        .thenReturn(null);
    when(passwordHandler.decryptPassword("enc-old-app-api-key")).thenReturn("plain-old-app-api-key");
    when(relayClient.register(any())).thenReturn(
        response("api-key-pat", "https://relay.example/webhook/new/{provider}", "signing", "cust-new-pat"));
    when(gitHubAppDAO.resetRelayLinkStateForAllActive(anyString()))
        .thenThrow(new RuntimeException("db transient"));

    // Must not throw.
    service.registerOnDemand();

    verify(relayClient).register(any());
  }

  @Test
  public void registerOnDemand_existingPatRow_doesNotDeregister() {
    // Same-mode re-registration: existing PAT row triggers in-place re-register via api key,
    // not a deregister.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-pat-api-key");
    existing.setWebhookUrl("https://relay.example/webhook/token/{provider}");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-pat-api-key")).thenReturn("plain-pat-api-key");
    when(relayClient.reRegisterWithApiKey(any(), any())).thenReturn(
        response("api-key-pat-2", "https://relay.example/webhook/token/{provider}", "signing", "cust"));

    service.registerOnDemand();

    verify(relayClient, never()).deregister(any());
    verify(relayConfigurationDAO, never()).delete();
    verify(relayClient).reRegisterWithApiKey(any(), eq("plain-pat-api-key"));
  }

  @Test
  public void registerOnDemand_licenseBytesComeFromLicenseContent() {
    enableFeature();
    byte[] raw = new byte[]{1, 2, 3, 4};
    when(licenseContent.raw()).thenReturn(raw);
    when(relayClient.register(any())).thenReturn(
        response("k", "https://relay.example/webhook/abc/github", "s", "c"));

    service.registerOnDemand();

    verify(relayClient).register(raw);
  }

  @Test
  public void registerOnDemand_emptyRawBytes_throwsIllegalState() {
    enableFeature();
    when(licenseContent.raw()).thenReturn(new byte[0]);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.registerOnDemand());
  }

  @Test
  public void registerOnDemand_nullLicenseContent_throwsIllegalState() {
    enableFeature();
    service = new RelayRegistrationService(
        relayClient, relayConfigurationDAO, null, passwordHandler, configuration, gitHubAppDAO);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.registerOnDemand());
  }

  @Test
  public void getGitHubAppWebhookUrl_derivesFromRegisteredCustomerWebhookPrefix() {
    // Customer-facing webhook URL is behind a CDN/proxy, NOT the lambda Function URL.
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setWebhookUrl("https://clm-staging.sonatype.com/scm-relay/webhook/abc123/{provider}");
    when(relayConfigurationDAO.get()).thenReturn(cfg);

    assertThat(service.getGitHubAppWebhookUrl())
        .isEqualTo("https://clm-staging.sonatype.com/scm-relay/webhook/github-app");
  }

  @Test
  public void getGitHubAppWebhookUrl_fallsBackToRelayUrlWhenNotRegistered() {
    when(relayConfigurationDAO.get()).thenReturn(null);
    when(configuration.getRelayUrl()).thenReturn("https://relay.example.com");

    assertThat(service.getGitHubAppWebhookUrl()).isEqualTo("https://relay.example.com/webhook/github-app");
  }

  @Test
  public void getGitHubAppWebhookUrl_fallsBackToRelayUrlWhenRegisteredWebhookUrlBlank() {
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setWebhookUrl(null);
    when(relayConfigurationDAO.get()).thenReturn(cfg);
    when(configuration.getRelayUrl()).thenReturn("https://relay.example.com/");

    assertThat(service.getGitHubAppWebhookUrl()).isEqualTo("https://relay.example.com/webhook/github-app");
  }

  @Test
  public void getGitHubAppWebhookUrl_returnsNullWhenNothingConfigured() {
    when(relayConfigurationDAO.get()).thenReturn(null);
    when(configuration.getRelayUrl()).thenReturn(null);
    assertThat(service.getGitHubAppWebhookUrl()).isNull();

    when(configuration.getRelayUrl()).thenReturn("");
    assertThat(service.getGitHubAppWebhookUrl()).isNull();

    when(configuration.getRelayUrl()).thenReturn("   ");
    assertThat(service.getGitHubAppWebhookUrl()).isNull();
  }

  @Test
  public void isFeatureGateOpen_requiresFlag() {
    assertThat(service.isFeatureGateOpen()).isFalse();

    enableFeature();
    assertThat(service.isFeatureGateOpen()).isTrue();
  }

  /**
   * Re-injects a {@link SystemConfigurationPropertyDAO} mock that returns a non-null row for
   * {@code SCM_RELAY_INTEGRATION}, which flips the {@code SCM_RELAY_INTEGRATION} feature on (the
   * feature has {@code enabledWhenAbsent=false}).
   */
  private void enableFeature() {
    SystemConfigurationPropertyDAO sysDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    SystemConfigurationProperty prop =
        new SystemConfigurationProperty(SystemConfigurationProperty.SCM_RELAY_INTEGRATION, "true");
    when(sysDao.createTransactionContext()).thenReturn(tx);
    when(sysDao.getByName(any(), Mockito.eq(SystemConfigurationProperty.SCM_RELAY_INTEGRATION)))
        .thenReturn(prop);
    SystemConfigurationPropertyFeature.injectDependencies(sysDao);
  }

  @Test
  public void rotateApiKeyOnDemand_happyPath_persistsNewEncryptedKeyAndLeavesOtherFields() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    existing.setWebhookSigningSecret("enc-signing");
    existing.setCustomerId("cust-1");
    java.util.Date originalRegisteredAt = new java.util.Date(123L);
    existing.setRegisteredAt(originalRegisteredAt);
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old")).thenReturn("old-key");
    when(relayClient.rotateApiKey("old-key")).thenReturn(rotateKeyResponse("new-key", "2026-01-01T00:05:00Z"));

    RelayRotateKeyResponse response = service.rotateApiKeyOnDemand();

    assertThat(response.getApiKey()).isEqualTo("new-key");
    assertThat(response.getPreviousKeyExpiresAt()).isEqualTo("2026-01-01T00:05:00Z");
    ArgumentCaptor<RelayConfiguration> captor = ArgumentCaptor.forClass(RelayConfiguration.class);
    verify(relayConfigurationDAO).set(captor.capture());
    RelayConfiguration persisted = captor.getValue();
    // Only the api_key column changes; everything else stays.
    assertThat(persisted.getApiKey()).isEqualTo("enc-new-key");
    assertThat(persisted.getWebhookUrl()).isEqualTo("https://relay.example/webhook/tok-1/github");
    assertThat(persisted.getWebhookSigningSecret()).isEqualTo("enc-signing");
    assertThat(persisted.getCustomerId()).isEqualTo("cust-1");
    assertThat(persisted.getRegisteredAt()).isSameAs(originalRegisteredAt);
  }

  @Test
  public void rotateApiKeyOnDemand_noRow_throwsIllegalState() {
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(null);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateApiKeyOnDemand())
        .withMessageContaining("No relay registration");

    verify(relayClient, never()).rotateApiKey(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateApiKeyOnDemand_featureGateClosed_throwsRelayFeatureDisabled() {
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.rotateApiKeyOnDemand());

    verify(relayClient, never()).rotateApiKey(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateApiKeyOnDemand_relayError_doesNotPersistChange() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old")).thenReturn("old-key");
    when(relayClient.rotateApiKey(any())).thenThrow(new BadGatewayException("relay down"));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> service.rotateApiKeyOnDemand());

    // Local row must remain intact — no partial update.
    verify(relayConfigurationDAO, never()).set(any());
    assertThat(existing.getApiKey()).isEqualTo("enc-old");
  }

  @Test
  public void rotateApiKeyOnDemand_apiKeyDecryptFails_throwsIllegalState() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-broken");
    existing.setWebhookUrl("https://relay.example/webhook/tok/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-broken")).thenThrow(new RuntimeException("bad cipher"));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateApiKeyOnDemand())
        .withMessageContaining("could not be decrypted");

    verify(relayClient, never()).rotateApiKey(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateWebhookSecretOnDemand_happyPath_persistsNewEncryptedSecretAndLeavesOtherFields() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    existing.setWebhookSigningSecret("enc-old-signing");
    existing.setCustomerId("cust-1");
    java.util.Date originalRegisteredAt = new java.util.Date(456L);
    existing.setRegisteredAt(originalRegisteredAt);
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");
    when(relayClient.rotateWebhookSecret("plain-key"))
        .thenReturn(rotateWebhookSecretResponse("new-secret", "2026-01-01T00:05:00Z"));

    RelayRotateWebhookSecretResponse response = service.rotateWebhookSecretOnDemand();

    assertThat(response.getWebhookSecret()).isEqualTo("new-secret");
    assertThat(response.getPreviousSecretExpiresAt()).isEqualTo("2026-01-01T00:05:00Z");
    ArgumentCaptor<RelayConfiguration> captor = ArgumentCaptor.forClass(RelayConfiguration.class);
    verify(relayConfigurationDAO).set(captor.capture());
    RelayConfiguration persisted = captor.getValue();
    // Only webhook_signing_secret changes; everything else stays.
    assertThat(persisted.getWebhookSigningSecret()).isEqualTo("enc-new-secret");
    assertThat(persisted.getApiKey()).isEqualTo("enc-key");
    assertThat(persisted.getWebhookUrl()).isEqualTo("https://relay.example/webhook/tok-1/github");
    assertThat(persisted.getCustomerId()).isEqualTo("cust-1");
    assertThat(persisted.getRegisteredAt()).isSameAs(originalRegisteredAt);
  }

  @Test
  public void rotateWebhookSecretOnDemand_noRow_throwsIllegalState() {
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(null);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateWebhookSecretOnDemand())
        .withMessageContaining("No relay registration");

    verify(relayClient, never()).rotateWebhookSecret(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateWebhookSecretOnDemand_featureGateClosed_throwsRelayFeatureDisabled() {
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.rotateWebhookSecretOnDemand());

    verify(relayClient, never()).rotateWebhookSecret(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateWebhookSecretOnDemand_relayError_doesNotPersistChange() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    existing.setWebhookUrl("https://relay.example/webhook/tok-1/github");
    existing.setWebhookSigningSecret("enc-old-signing");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");
    when(relayClient.rotateWebhookSecret(any())).thenThrow(new BadGatewayException("relay down"));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> service.rotateWebhookSecretOnDemand());

    verify(relayConfigurationDAO, never()).set(any());
    assertThat(existing.getWebhookSigningSecret()).isEqualTo("enc-old-signing");
  }

  @Test
  public void rotateWebhookSecretOnDemand_apiKeyDecryptFails_throwsIllegalState() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-broken");
    existing.setWebhookUrl("https://relay.example/webhook/tok/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-broken")).thenThrow(new RuntimeException("bad cipher"));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateWebhookSecretOnDemand())
        .withMessageContaining("could not be decrypted");

    verify(relayClient, never()).rotateWebhookSecret(any());
    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateApiKeyOnDemand_emptyResponse_throwsIllegalState() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-old");
    existing.setWebhookUrl("https://relay.example/webhook/tok/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-old")).thenReturn("old-key");
    when(relayClient.rotateApiKey("old-key")).thenReturn(rotateKeyResponse("", null));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateApiKeyOnDemand())
        .withMessageContaining("empty rotate-key response");

    verify(relayConfigurationDAO, never()).set(any());
  }

  @Test
  public void rotateWebhookSecretOnDemand_emptyResponse_throwsIllegalState() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    existing.setWebhookUrl("https://relay.example/webhook/tok/github");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");
    when(relayClient.rotateWebhookSecret("plain-key")).thenReturn(rotateWebhookSecretResponse(null, null));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.rotateWebhookSecretOnDemand())
        .withMessageContaining("empty rotate-webhook-secret response");

    verify(relayConfigurationDAO, never()).set(any());
  }

  private RelayRotateKeyResponse rotateKeyResponse(String apiKey, String previousKeyExpiresAt) {
    RelayRotateKeyResponse r = new RelayRotateKeyResponse();
    r.setApiKey(apiKey);
    r.setPreviousKeyExpiresAt(previousKeyExpiresAt);
    return r;
  }

  private RelayRotateWebhookSecretResponse rotateWebhookSecretResponse(String secret, String previousSecretExpiresAt) {
    RelayRotateWebhookSecretResponse r = new RelayRotateWebhookSecretResponse();
    r.setWebhookSecret(secret);
    r.setPreviousSecretExpiresAt(previousSecretExpiresAt);
    return r;
  }

  private RelayRegisterResponse response(String apiKey, String webhookUrl, String secret, String customerId) {
    RelayRegisterResponse r = new RelayRegisterResponse();
    r.setApiKey(apiKey);
    r.setWebhookUrl(webhookUrl);
    r.setWebhookSecret(secret);
    r.setCustomerId(customerId);
    return r;
  }

  // -------------------------------------------------------------------------
  // deregisterIfRegistered
  // -------------------------------------------------------------------------

  @Test
  public void deregisterIfRegistered_noRow_isNoOp() {
    enableFeature();
    when(relayConfigurationDAO.get()).thenReturn(null);

    service.deregisterIfRegistered();

    verify(relayClient, never()).deregister(anyString());
    verify(relayConfigurationDAO, never()).delete();
  }

  @Test
  public void deregisterIfRegistered_featureGateClosed_throwsAndDoesNotTouchRelay() {
    // Feature gate must guard deregister symmetrically with register: when scmRelayIntegration
    // is disabled, calling deregister returns 412 (mapped from RelayFeatureDisabledException)
    // rather than silently succeeding with a no-op or hitting a stale relay. Without this
    // guard the endpoint would return 200 from the early-return-on-null path and admins
    // would assume the registration was wiped.
    assertThatExceptionOfType(RelayRegistrationService.RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.deregisterIfRegistered());

    verify(relayClient, never()).deregister(anyString());
    verify(relayConfigurationDAO, never()).delete();
  }

  @Test
  public void deregisterIfRegistered_existingRow_callsRelayAndDropsLocalRow() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");

    service.deregisterIfRegistered();

    verify(relayClient).deregister("plain-key");
    verify(relayConfigurationDAO).delete();
  }

  @Test
  public void deregisterIfRegistered_relayFailure_throwsAndKeepsLocalRow() {
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");
    doThrow(new RuntimeException("relay 502")).when(relayClient).deregister("plain-key");

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.deregisterIfRegistered())
        .withMessageContaining("Retry once the relay is reachable");

    verify(relayConfigurationDAO, never()).delete();
  }

  @Test
  public void deregisterIfRegistered_blankApiKey_skipsRelayCallAndDropsLocalRow() {
    // Defensive: an existing row whose api key fails to decrypt to a usable plaintext should
    // still drop the local row so a fresh register is unblocked.
    enableFeature();
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-blank");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-blank")).thenReturn("");

    service.deregisterIfRegistered();

    verify(relayClient, never()).deregister(anyString());
    verify(relayConfigurationDAO).delete();
  }

  // -------------------------------------------------------------------------
  // deleteRelayInstallation
  // -------------------------------------------------------------------------

  @Test
  public void deleteRelayInstallation_existingRow_callsRelay() {
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");

    service.deleteRelayInstallation(12345L);

    verify(relayClient).deleteInstallation("plain-key", "12345");
  }

  @Test
  public void deleteRelayInstallation_noRow_isNoOp() {
    when(relayConfigurationDAO.get()).thenReturn(null);

    service.deleteRelayInstallation(12345L);

    verify(relayClient, never()).deleteInstallation(anyString(), anyString());
  }

  @Test
  public void deleteRelayInstallation_nullInstallationId_isNoOp() {
    service.deleteRelayInstallation(null);

    verify(relayConfigurationDAO, never()).get();
    verify(relayClient, never()).deleteInstallation(anyString(), anyString());
  }

  @Test
  public void deleteRelayInstallation_blankApiKey_skipsRelayCall() {
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-broken");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-broken")).thenReturn("");

    service.deleteRelayInstallation(12345L);

    verify(relayClient, never()).deleteInstallation(anyString(), anyString());
  }

  @Test
  public void deleteRelayInstallation_relayFailure_isSwallowed() {
    // Best-effort: a relay-side failure must not block local deletion. Subsequent
    // deregisters can re-converge.
    RelayConfiguration existing = new RelayConfiguration();
    existing.setApiKey("enc-key");
    when(relayConfigurationDAO.get()).thenReturn(existing);
    when(passwordHandler.decryptPassword("enc-key")).thenReturn("plain-key");
    doThrow(new RuntimeException("relay 503")).when(relayClient)
        .deleteInstallation("plain-key", "12345");

    // Should not throw.
    service.deleteRelayInstallation(12345L);
  }
}
