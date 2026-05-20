/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GitHubAppDeletionService}.
 */
public class GitHubAppDeletionServiceTest
    extends AbstractComponentTest
{
  private static final Long TEST_VALID_INSTALLATION_ID = 67890L;

  private int appIdCounter = 10000;

  @Rule(order = 0)
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private GitHubAppInstallationStateDAO installationStateDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private InsightProxy insightProxy;

  @Mock
  private GitHubAppAuthStrategyCache mockCache;

  @Mock
  private GitHubAppSelectionCache mockSelectionCache;

  @Inject
  private GitHubAppDeletionService deletionService;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    setupGitHubAppDeletionService();
    stubGitHubAppInstallationDeletion();
  }

  private void setupGitHubAppDeletionService() {
    String wireMockBaseUrl = "http://localhost:" + githubMockServer.port();
    deletionService = new GitHubAppDeletionService(
        gitHubAppDAO,
        installationStateDAO,
        passwordHandler,
        insightProxy,
        mockCache,
        mockSelectionCache,
        wireMockBaseUrl);
  }

  private void stubGitHubAppInstallationDeletion() {
    stubFor(delete(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID))
        .willReturn(aResponse()
            .withStatus(204)));
  }

  @Test
  public void testDelete_NoGitHubApp_ReturnsSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testDelete_GitHubAppExists_DeletesSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());
    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_CalledTwice_IsIdempotent() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());
    deletionService.delete(app.getId());
    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testDelete_NullInstallationId_DeletesGitHubAppFromDatabase() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubAppWithoutInstallationId(app.getId());

    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testDelete_GitHubApiDeletionFails_ContinuesWithDatabaseDeletion() {
    stubFor(delete(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID))
        .willReturn(aResponse()
            .withStatus(500)
            .withBody("{\"message\":\"Internal Server Error\"}")));

    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());

    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_GitHubApiReturns404_ContinuesWithDatabaseDeletion() {
    stubFor(delete(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID))
        .willReturn(aResponse()
            .withStatus(404)
            .withBody("{\"message\":\"Not Found\"}")));

    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());
    deletionService.delete(app.getId());

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_WithPendingInstallationStates() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());

    List<GitHubApp> gitHubApps = gitHubAppDAO.getByOwnerId(app.getId());
    assertThat(gitHubApps).isNotEmpty();
    GitHubApp gitHubApp = gitHubApps.get(0);

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);

    tempEntity.newGitHubAppInstallationState("pending-state-1", gitHubApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("pending-state-2", gitHubApp.getId(), "code-verifier-2", expiresAt);

    deletionService.delete(app.getId());

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_InactiveGitHubApp_DeletesSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createGitHubApp(app.getId(), TEST_VALID_INSTALLATION_ID, false);

    deletionService.delete(gitHubApp);

    assertThat(gitHubAppDAO.getById(gitHubApp.getId())).isNull();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  private GitHubApp createGitHubApp(String ownerId) {
    return createGitHubApp(ownerId, TEST_VALID_INSTALLATION_ID, true);
  }

  private GitHubApp createGitHubAppWithoutInstallationId(String ownerId) {
    return createGitHubApp(ownerId, null, true);
  }

  private GitHubApp createGitHubApp(String ownerId, Long installationId, boolean isActive) {
    appIdCounter++;

    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(appIdCounter);
    gitHubApp.setSlug("test-app");
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setClientId("test-client-id");
    gitHubApp.setClientSecret(passwordHandler.encryptPassword("test-client-secret"));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId(installationId);
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setActive(isActive);
    return tempEntity.newGitHubApp(gitHubApp);
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

  @Test
  public void testDeactivateGitHubApps_NoGitHubApp_ReturnsSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testDeactivateGitHubApps_DeactivatesSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = tempEntity.newGitHubApp(app.getId());
    gitHubApp.setActive(true);
    gitHubAppDAO.update(gitHubApp);

    assertThat(gitHubAppDAO.getById(gitHubApp.getId()).isActive()).isTrue();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }

    GitHubApp deactivatedApp = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(deactivatedApp).isNotNull();
    assertThat(deactivatedApp.isActive()).isFalse();
  }

  @Test
  public void testDeactivateGitHubApps_DeactivatesMultipleApps() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp gitHubApp1 = createGitHubApp(app.getId(), TEST_VALID_INSTALLATION_ID, true);
    GitHubApp gitHubApp2 = createGitHubApp(app.getId(), TEST_VALID_INSTALLATION_ID + 1, true);

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }

    assertThat(gitHubAppDAO.getById(gitHubApp1.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId()).isActive()).isFalse();
  }

  @Test
  public void testDeactivateGitHubApps_WithMultipleApps_DeactivatesAll() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp app1 = createGitHubApp(app.getId(), 11111L, true);
    GitHubApp app2 = createGitHubApp(app.getId(), 22222L, false);
    GitHubApp app3 = createGitHubApp(app.getId(), 33333L, true);

    app1.setActive(true);
    gitHubAppDAO.update(app1);
    app3.setActive(true);
    gitHubAppDAO.update(app3);

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app3.getId()).isActive()).isFalse();
  }

  @Test
  public void testReactivateGitHubApps_OnlyReactivatesInstalledApps() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp installedApp = createGitHubApp(app.getId(), 11111L, false);
    GitHubApp uninstalledApp = createGitHubAppWithoutInstallationId(app.getId());

    // Deactivate both
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }
    assertThat(gitHubAppDAO.getById(installedApp.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(uninstalledApp.getId()).isActive()).isFalse();

    // Reactivate — only the installed app should become active
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.reactivateGitHubApps(tx, app.getId());
    }

    assertThat(gitHubAppDAO.getById(installedApp.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(uninstalledApp.getId()).isActive()).isFalse();
  }

  @Test
  public void testReactivateGitHubApps_NoApps_ReturnsSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.reactivateGitHubApps(tx, app.getId());
    }
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isEmpty();
  }

  @Test
  public void testReactivateGitHubApps_MultipleInstalledApps_ReactivatesAll() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp app1 = createGitHubApp(app.getId(), 11111L, false);
    GitHubApp app2 = createGitHubApp(app.getId(), 22222L, false);
    GitHubApp uninstalledApp = createGitHubAppWithoutInstallationId(app.getId());

    // Deactivate the uninstalled app too (it's already created as active by createGitHubAppWithoutInstallationId)
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.deactivateGitHubApps(tx, app.getId());
    }

    // Reactivate
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      deletionService.reactivateGitHubApps(tx, app.getId());
    }

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(uninstalledApp.getId()).isActive()).isFalse();
  }
}
