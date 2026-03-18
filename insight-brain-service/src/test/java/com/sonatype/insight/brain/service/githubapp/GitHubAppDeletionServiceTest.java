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
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

  @Inject
  private GitHubAppDeletionService deletionService;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
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
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testDelete_GitHubAppExists_DeletesSuccessfully() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());
    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_CalledTwice_IsIdempotent() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());
    deletionService.delete(app.getId());
    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testDelete_NullInstallationId_DeletesGitHubAppFromDatabase() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubAppWithoutInstallationId(app.getId());

    deletionService.delete(app.getId());
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
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
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
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

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  @Test
  public void testDelete_WithPendingInstallationStates() {
    Application app = tempEntity.newApplicationWithParent();
    createGitHubApp(app.getId());

    GitHubApp gitHubApp = gitHubAppDAO.getByOwnerId(app.getId());
    assertThat(gitHubApp).isNotNull();

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);

    tempEntity.newGitHubAppInstallationState("pending-state-1", gitHubApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("pending-state-2", gitHubApp.getId(), "code-verifier-2", expiresAt);

    deletionService.delete(app.getId());

    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
    verify(deleteRequestedFor(urlEqualTo("/app/installations/" + TEST_VALID_INSTALLATION_ID)));
  }

  private void createGitHubApp(String ownerId) {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(12345);
    gitHubApp.setSlug("test-app");
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setClientId("test-client-id");
    gitHubApp.setClientSecret(passwordHandler.encryptPassword("test-client-secret"));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId(TEST_VALID_INSTALLATION_ID);
    gitHubApp.setLastUpdatedAt(new Date());
    tempEntity.newGitHubApp(gitHubApp);
  }

  private void createGitHubAppWithoutInstallationId(String ownerId) {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(12345);
    gitHubApp.setSlug("test-app");
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setClientId("test-client-id");
    gitHubApp.setClientSecret(passwordHandler.encryptPassword("test-client-secret"));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId(null); // Explicitly set to null
    gitHubApp.setLastUpdatedAt(new Date());
    tempEntity.newGitHubApp(gitHubApp);
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
