/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitHubAppRelayLinkerTest
{
  @Mock
  private RelayRegistrationService relayRegistrationService;

  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private PasswordHandler passwordHandler;

  @Mock
  private TransactionContext tx;

  private GitHubAppRelayLinker linker;

  @Before
  public void before() {
    linker = new GitHubAppRelayLinker(relayRegistrationService, gitHubAppDAO, passwordHandler);
    lenient().when(gitHubAppDAO.createTransactionContext()).thenReturn(tx);
    lenient().when(passwordHandler.decryptPassword(anyString()))
        .thenAnswer(inv -> "plain-" + inv.getArgument(0));
  }

  @Test
  public void tryRegister_success_marksOkAndResetsAttempts() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.ERROR, 5, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isTrue();
    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.OK);
    assertThat(app.getRelayLinkAttempts()).isZero();
    verify(relayRegistrationService).registerGitHubAppOnDemand("42", "plain-encrypted-secret");
    verify(gitHubAppDAO).update(eq(tx), eq(app));
  }

  @Test
  public void tryRegister_failure_belowCap_transitionsToError() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    org.mockito.Mockito.doThrow(new RuntimeException("relay 502"))
        .when(relayRegistrationService)
        .registerGitHubAppOnDemand(anyString(), any());
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isFalse();
    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.ERROR);
    assertThat(app.getRelayLinkAttempts()).isEqualTo(1);
  }

  @Test
  public void tryRegister_failure_atCap_transitionsToFailed() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    org.mockito.Mockito.doThrow(new RuntimeException("relay 502"))
        .when(relayRegistrationService)
        .registerGitHubAppOnDemand(anyString(), any());
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.ERROR, RelayLinkState.MAX_ATTEMPTS - 1, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isFalse();
    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.FAILED);
    assertThat(app.getRelayLinkAttempts()).isEqualTo(RelayLinkState.MAX_ATTEMPTS);
  }

  @Test
  public void tryRegister_featureGateClosed_isNoopReturnsFalse() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isFalse();
    verify(relayRegistrationService, never()).registerGitHubAppOnDemand(anyString(), any());
    verify(gitHubAppDAO, never()).update(any(), any());
  }

  @Test
  public void tryRegister_relayInPatMode_isNoopReturnsFalse() {
    // After cross-mode flip App → PAT, the local github_app rows are orphaned. The polling-
    // cycle pre-flight retry must NOT auto-flip the relay back to GitHub App mode by
    // calling registerGitHubAppOnDemand (which would deregister the active PAT customer).
    // Cross-mode is a deliberate admin choice; only an explicit re-register should change it.
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setWebhookUrl("https://relay.example.com/webhook/abc/github");
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isFalse();
    verify(relayRegistrationService, never()).registerGitHubAppOnDemand(anyString(), any());
    verify(gitHubAppDAO, never()).update(any(), any());
  }

  @Test
  public void tryRegister_relayInGitHubAppMode_proceedsToRegister() {
    // Same-mode (App → second-or-later App) retry: the mode-guard does NOT block when the
    // relay is already in GitHub App mode (webhookUrl blank).
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setWebhookUrl(null);
    when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    when(passwordHandler.decryptPassword("encrypted-secret")).thenReturn("plain-secret");
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isTrue();
    verify(relayRegistrationService).registerGitHubAppOnDemand("42", "plain-secret");
  }

  @Test
  public void tryRegisterFromInstall_relayInPatMode_bypassesGuardAndRegisters() {
    // Install-time path: admin just installed a GitHub App through the IQ UI. They are
    // explicitly opting into GitHub App mode, so an existing auto-registered PAT customer
    // must yield. tryRegisterFromInstall bypasses the cross-flip guard that the polling
    // retry path uses.
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setWebhookUrl("https://relay.example.com/webhook/abc/github");
    // getConfiguration may or may not be consulted on the install path; stub leniently.
    org.mockito.Mockito.lenient().when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    when(passwordHandler.decryptPassword("encrypted-secret")).thenReturn("plain-secret");
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegisterFromInstall(app);

    assertThat(ok).isTrue();
    verify(relayRegistrationService).registerGitHubAppOnDemand("42", "plain-secret");
  }

  @Test
  public void tryRegisterFromInstall_featureGateClosed_isStillBlocked() {
    // The install path does NOT bypass the feature gate — only the cross-flip guard.
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegisterFromInstall(app);

    assertThat(ok).isFalse();
    verify(relayRegistrationService, never()).registerGitHubAppOnDemand(anyString(), any());
  }

  @Test
  public void tryRegister_nullInstallationId_isNoopReturnsFalse() {
    GitHubApp app = appWith("app-1", null, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isFalse();
    verify(relayRegistrationService, never()).registerGitHubAppOnDemand(anyString(), any());
  }

  @Test
  public void tryRegister_nullSecret_passesNullToRegistrationService() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, null);

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isTrue();
    // Null secret is intentional during retry: the relay's registerGitHubApp accepts null
    // and creates the customer record without rotating the existing webhook secret.
    verify(relayRegistrationService).registerGitHubAppOnDemand("42", null);
  }

  @Test
  public void tryRegister_decryptFailure_continuesWithNullSecret() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(passwordHandler.decryptPassword("encrypted-secret"))
        .thenThrow(new RuntimeException("key material lost"));
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.UNREGISTERED, 0, "encrypted-secret");

    boolean ok = linker.tryRegister(app);

    assertThat(ok).isTrue();
    verify(relayRegistrationService).registerGitHubAppOnDemand("42", null);
  }

  @Test
  public void markSuccess_persistsOkAndResetsAttempts() {
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.ERROR, 7, null);

    linker.markSuccess(app);

    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.OK);
    assertThat(app.getRelayLinkAttempts()).isZero();
    verify(gitHubAppDAO).update(eq(tx), eq(app));
  }

  @Test
  public void markFailure_belowCap_persistsError() {
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.OK, 0, null);

    linker.markFailure(app);

    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.ERROR);
    assertThat(app.getRelayLinkAttempts()).isEqualTo(1);
  }

  @Test
  public void markFailure_atCap_persistsFailed() {
    GitHubApp app = appWith("app-1", 42L, RelayLinkState.ERROR, RelayLinkState.MAX_ATTEMPTS - 1, null);

    linker.markFailure(app);

    assertThat(app.getRelayLinkState()).isEqualTo(RelayLinkState.FAILED);
    assertThat(app.getRelayLinkAttempts()).isEqualTo(RelayLinkState.MAX_ATTEMPTS);
  }

  private static GitHubApp appWith(String id, Long installationId, String state, int attempts, String secret) {
    GitHubApp app = new GitHubApp();
    app.setId(id);
    app.setInstallationId(installationId);
    app.setRelayLinkState(state);
    app.setRelayLinkAttempts(attempts);
    app.setWebhookSecret(secret);
    app.setActive(true);
    return app;
  }
}
