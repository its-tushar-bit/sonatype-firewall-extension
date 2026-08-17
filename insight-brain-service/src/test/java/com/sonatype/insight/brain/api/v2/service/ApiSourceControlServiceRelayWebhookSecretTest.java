/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.relay.RelayRegistrationService;
import com.sonatype.insight.brain.relay.RelayRegistrationService.RelayFeatureDisabledException;
import com.sonatype.insight.brain.relay.dto.RelayWebhookSecretResponse;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.sourcecontrol.SourceControlDataService;
import com.sonatype.insight.brain.sourcecontrol.SourceControlRepositoryUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;

import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-unit coverage for {@link ApiSourceControlService#getRelayWebhookSecret()}. Authz is
 * already covered by {@code ApiSourceControlServiceAuthzTest} (in the component-h2 variant module); this class focuses
 * on the
 * branch logic (feature gate, registration shape, decryption).
 */
@ExtendWith(MockitoExtension.class)
public class ApiSourceControlServiceRelayWebhookSecretTest
{
  private static final String ENCRYPTED_SECRET = "encrypted-blob";

  private static final String DECRYPTED_SECRET = "plaintext-secret";

  private static final String WEBHOOK_URL = "https://relay.example.com/webhook/abc/github";

  @Mock
  private RelayRegistrationService relayRegistrationService;

  @Mock
  private PasswordHandler passwordHandler;

  private ApiSourceControlService service;

  @BeforeEach
  public void setUp() {
    // The @Authorize aspect is compile-time-woven into ApiSourceControlService bytecode
    // by aspectj-maven-plugin; direct construction does NOT bypass it (only Spring proxies
    // are bypassed). Without disabling enforcement, every @Authorize-annotated method
    // throws UnauthenticatedException before reaching the branch logic these tests
    // exercise. Authz coverage proper lives in ApiSourceControlServiceAuthzTest. Same
    // pattern as LdapUserAndGroupMappingTest, ScanResourceUnitTest, etc.
    SecurityAspectControl.disableEnforcement();

    service = new ApiSourceControlService(
        passwordHandler,
        mock(SourceControlDAO.class),
        mock(OwnerDAO.class),
        mock(ApplicationDAO.class),
        mock(AutomaticSourceControlConfigurationDAO.class),
        mock(SourceControlConfigurationDAO.class),
        mock(IqForScmLicenseChecker.class),
        mock(TelemetrySender.class),
        mock(SourceControlPullRequestMetrics.class),
        mock(SourceControlEventDAO.class),
        mock(InsightWork.class),
        mock(FileCleaner.class),
        mock(SourceControlRepositoryUtils.class),
        mock(GitClientFactory.class),
        mock(SourceControlUserActivityService.class),
        mock(TelemetryUtils.class),
        mock(ScmRepoVisibilityService.class),
        mock(ApiSourceControlAdapter.class),
        mock(SourceControlDataService.class),
        mock(GitHubAppDeletionService.class),
        relayRegistrationService,
        mock(com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO.class),
        mock(com.sonatype.insight.brain.relay.GitHubAppRelayLinker.class));
  }

  @AfterEach
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  @Test
  public void returnsDecryptedSecret_whenRegisteredInPatMode() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setWebhookUrl(WEBHOOK_URL);
    cfg.setWebhookSigningSecret(ENCRYPTED_SECRET);
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    when(passwordHandler.decryptPassword(ENCRYPTED_SECRET)).thenReturn(DECRYPTED_SECRET);

    RelayWebhookSecretResponse response = service.getRelayWebhookSecret();

    assertThat(response).isNotNull();
    assertThat(response.getWebhookSecret()).isEqualTo(DECRYPTED_SECRET);
    verify(passwordHandler).decryptPassword(ENCRYPTED_SECRET);
  }

  @Test
  public void returnsNull_whenRegisteredInGitHubAppMode() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    // App-mode registrations have no webhookUrl (App-level webhook config) and -- critically --
    // no per-customer signing secret. Treat them the same as no row: 404.
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setWebhookUrl(null);
    cfg.setWebhookSigningSecret(null);
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);

    assertThat(service.getRelayWebhookSecret()).isNull();
    verify(passwordHandler, never()).decryptPassword(any(String.class));
  }

  @Test
  public void returnsNull_whenNotRegistered() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayRegistrationService.getConfiguration()).thenReturn(null);

    assertThat(service.getRelayWebhookSecret()).isNull();
    verify(passwordHandler, never()).decryptPassword(any(String.class));
  }

  @Test
  public void throwsFeatureDisabled_whenFeatureGateClosed() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);

    assertThatExceptionOfType(RelayFeatureDisabledException.class)
        .isThrownBy(() -> service.getRelayWebhookSecret());
    verify(relayRegistrationService, never()).getConfiguration();
    verify(passwordHandler, never()).decryptPassword(any(String.class));
  }

  // -------------------------------------------------------------------------
  // deregisterFromRelay routing
  //
  // The admin-triggered REST path must call deregisterIfRegistered() (which honors the
  // feature gate) and not deregisterTenant() (gate-bypassing, reserved for
  // DeleteTenantsJob and similar tenant-lifecycle paths). Without this routing the
  // /relay/deregister endpoint silently returns 200 when the gate is closed, breaking
  // symmetry with /relay/register, /relay/rotate-key, and /relayWebhookSecret.
  // -------------------------------------------------------------------------

  @Test
  public void deregisterFromRelay_callsDeregisterIfRegistered_notDeregisterTenant() {
    service.deregisterFromRelay();

    verify(relayRegistrationService).deregisterIfRegistered();
    verify(relayRegistrationService, never()).deregisterTenant();
  }
}
