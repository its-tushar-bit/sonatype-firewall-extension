/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SourceControlAuthenticationTransitionHandler}.
 */
public class SourceControlAuthenticationTransitionHandlerTest
    extends AbstractComponentTest
{
  private static final Integer TEST_GITHUB_APP_ID = 12345;

  private static final String TEST_GITHUB_APP_SLUG = "test-app";

  private static final String TEST_CLIENT_ID = "test-client-id";

  private static final Long TEST_INSTALLATION_ID = 67890L;

  private static final String TEST_CLIENT_SECRET = "test-client-secret";

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private GitHubAppInstallationStateDAO installationStateDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private InsightProxy insightProxy;

  private SourceControlAuthenticationTransitionHandler cleanupHandler;

  @Before
  public void setup() throws Exception {
    super.setUp();
    setupServices();
    stubGitHubAppInstallationDeletion();
  }

  private void setupServices() {
    String wireMockBaseUrl = "http://localhost:" + githubMockServer.port();
    GitHubAppDeletionService deletionService = new GitHubAppDeletionService(
        gitHubAppDAO,
        installationStateDAO,
        passwordHandler,
        insightProxy,
        wireMockBaseUrl);
    cleanupHandler = new SourceControlAuthenticationTransitionHandler(deletionService);
  }

  private void stubGitHubAppInstallationDeletion() {
    stubFor(delete(urlPathMatching("/app/installations/.*"))
        .willReturn(aResponse().withStatus(204)));
  }

  @Test
  public void testHandleAuthTransition_SameAuthType_PAT_DeletesGitHubApp() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.PAT);

    cleanupHandler.handleAuthTransition(storedSC, newSC);

    // When transitioning to non-GITHUB_APP auth type, GitHub App should be deleted
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_PatToGitHubApp_ClearsToken() {
    Application app = tempEntity.newApplicationWithParent();
    String originalToken = "pat-token-value";

    SourceControl storedSC = createSourceControlWithToken(
        app.getId(), originalToken, AuthenticationType.PAT);
    SourceControl newSC = createSourceControlWithToken(
        app.getId(), originalToken, AuthenticationType.GITHUB_APP);

    cleanupHandler.handleAuthTransition(storedSC, newSC);
    assertThat(newSC.getToken()).isNull();
    verify(0, deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_GitHubAppToPat_DeletesGitHubApp() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.PAT);

    cleanupHandler.handleAuthTransition(storedSC, newSC);
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_NullToGitHubApp_NoHandle() {
    Application app = tempEntity.newApplicationWithParent();

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, null);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);

    cleanupHandler.handleAuthTransition(storedSC, newSC);
    verify(0, deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_SameAuthType_GitHubApp_NoHandle() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);

    cleanupHandler.handleAuthTransition(storedSC, newSC);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNotNull();
    verify(0, deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_GitHubAppToNull_DeletesGitHubApp() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, null);

    cleanupHandler.handleAuthTransition(storedSC, newSC);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_NullToPat_DeletesGitHubApp() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, null);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.PAT);

    cleanupHandler.handleAuthTransition(storedSC, newSC);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testDeleteGitHubAppInstallation_GitHubProviderWithGitHubAppAuth_CallsDeletion() {
    Application app = tempEntity.newApplicationWithParent();
    createAndInsertGitHubApp(app.getId());

    SourceControl sc = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);

    cleanupHandler.deleteGitHubAppInstallation(sc);
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testDeleteGitHubAppInstallation_WithPendingInstallationStates_DeletesBoth() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("pending-state-1", gitHubApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("pending-state-2", gitHubApp.getId(), "code-verifier-2", expiresAt);

    SourceControl sc = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNotNull();
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-2")).isNotNull();
      tx.commit();
    }

    cleanupHandler.deleteGitHubAppInstallation(sc);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-1")).isNull();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-2")).isNull();
      tx.commit();
    }
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  @Test
  public void testHandleAuthTransition_GitHubAppToPat_WithPendingStates_DeletesAllData() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("transition-state-1", gitHubApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("transition-state-2", gitHubApp.getId(), "code-verifier-2", expiresAt);

    SourceControl storedSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(
        app.getId(), SourceControlProvider.GITHUB, AuthenticationType.PAT);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNotNull();
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "transition-state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "transition-state-2")).isNotNull();
      tx.commit();
    }

    cleanupHandler.handleAuthTransition(storedSC, newSC);

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "transition-state-1")).isNull();
      assertThat(installationStateDAO.findByStateToken(tx, "transition-state-2")).isNull();
      tx.commit();
    }
    verify(deleteRequestedFor(urlPathMatching("/app/installations/.*")));
  }

  private GitHubApp createAndInsertGitHubApp(String ownerId) {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(TEST_GITHUB_APP_ID);
    gitHubApp.setSlug(TEST_GITHUB_APP_SLUG);
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setClientId(TEST_CLIENT_ID);
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(TEST_CLIENT_SECRET));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId(TEST_INSTALLATION_ID);
    return tempEntity.newGitHubApp(gitHubApp);
  }

  private SourceControl createSourceControl(
      final String ownerId,
      final SourceControlProvider provider,
      final AuthenticationType authType)
  {
    return new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setRepositoryUrl("https://github.com/test/repo")
        .setProvider(provider)
        .setAuthenticationType(authType)
        .build();
  }

  private SourceControl createSourceControlWithToken(
      final String ownerId,
      final String token,
      final AuthenticationType authType)
  {
    return new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setRepositoryUrl("https://github.com/test/repo")
        .setToken(token)
        .setProvider(SourceControlProvider.GITHUB)
        .setAuthenticationType(authType)
        .build();
  }

  private String generateTestRsaPrivateKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      byte[] pkcs8EncodedKey = keyPair.getPrivate().getEncoded();
      return Base64.getEncoder().encodeToString(pkcs8EncodedKey);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to generate test RSA key", e);
    }
  }
}
