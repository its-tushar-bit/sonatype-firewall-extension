/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sonatype.insight.brain.api.v2.dto.githubapp.ApiGitHubAppManifestDTO;
import com.sonatype.insight.brain.api.v2.dto.githubapp.Manifest;
import com.sonatype.insight.brain.git.GitHubManifestService;
import com.sonatype.insight.brain.service.BaseUrl;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppRegistrationStateDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.ws.rs.InternalServerErrorException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiGitHubAppServiceTest
    extends AbstractComponentTest
{
  private static final String CLIENT_ID = "Iv1.test-client-id";

  private static final String CLIENT_SECRET = "test-client-secret";

  private static final String APP_SLUG = "test-app-slug";

  private static final Integer APP_ID = 123456;

  private static final Long INSTALLATION_ID = 98765L;

  private static final String OAUTH_CODE = "test-oauth-code";

  private static final String ACCESS_TOKEN = "ghu_test_access_token";

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private GitHubAppInstallationStateDAO installationStateDAO;

  @Inject
  private GitHubAppRegistrationStateDAO registrationStateDAO;

  @Inject
  private GitHubManifestService gitHubManifestService;

  @Inject
  GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private InsightProxy insightProxy;

  @Inject
  private GitHubAppAuthStrategyCache authStrategyCache;

  private ApiGitHubAppService service;

  private Organization organization;

  private GitHubApp gitHubApp;

  private BaseUrl baseUrl;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    String mockServerUrl = githubMockServer.baseUrl();
    baseUrl = mock(BaseUrl.class);
    service = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockServerUrl, // githubApiBaseUrl
        mockServerUrl, // githubOAuthTokenUrl
        baseUrl);

    setupGitHubMocks();

    organization = tempEntity.newOrganization("test-org");

    gitHubApp = new GitHubApp();
    gitHubApp.setAppId(APP_ID);
    gitHubApp.setSlug(APP_SLUG);
    gitHubApp.setClientId(CLIENT_ID);
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(CLIENT_SECRET));
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setOwnerId(organization.getId());
    gitHubApp.setGithubOrganizationName("(personal)");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setInstallationId(INSTALLATION_ID);
    gitHubApp.setActive(true);
    gitHubApp = tempEntity.newGitHubApp(gitHubApp);
  }

  private void setupGitHubMocks() {
    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":" +
                    "\"bearer\",\"scope\":\"\"}")));

    // Mock user endpoint
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("token " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"login\":\"testuser\",\"id\":12345}")));

    // Mock user installations API
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .withHeader("Authorization", equalTo("token " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":1,\"installations\":[{\"id\":" + INSTALLATION_ID
                    + ",\"app_id\":" + APP_ID + ",\"account\":{\"login\":\"test-org\",\"id\":12345}}]}")));

    // Mock delete installation API (for GitHubAppDeletionService)
    // Accepts any Authorization header (JWT token from GitHubApp authentication)
    githubMockServer.stubFor(
        delete(urlMatching("/app/installations/.*"))
            .willReturn(aResponse()
                .withStatus(204)));
  }

  @Test
  public void testHandleInstallationSetupCallback_Success_NewSourceControl() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-success", gitHubApp.getId(), futureDate);

    GitHubApp result = service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-success", OAUTH_CODE);

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(organization.getId());
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      GitHubAppInstallationState deletedState = installationStateDAO.findByStateToken(tx, "valid-state-success");
      assertThat(deletedState).isNull();
    }

    GitHubApp updated = gitHubAppDAO.getByOwnerId(organization.getId());
    assertThat(updated.getInstallationId()).isEqualTo(INSTALLATION_ID);
    assertThat(updated.getGithubOrganizationName()).isEqualTo("test-org(personal)");
  }

  @Test
  public void testHandleInstallationSetupCallback_PersonalAccount_StoresUsername() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-personal", gitHubApp.getId(), futureDate);

    // Mock user installations API with personal account
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":1,\"installations\":[{\"id\":" + INSTALLATION_ID
                    + ",\"app_id\":" + APP_ID
                    + ",\"account\":{\"login\":\"personal-user\",\"id\":54321,\"type\":\"User\"}}]}")));

    GitHubApp result = service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-personal", OAUTH_CODE);

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(organization.getId());

    GitHubApp updated = gitHubAppDAO.getByOwnerId(organization.getId());
    assertThat(updated.getInstallationId()).isEqualTo(INSTALLATION_ID);
    assertThat(updated.getGithubOrganizationName()).isEqualTo("personal-user(personal)");
  }

  @Test
  public void testHandleInstallationSetupCallback_InvalidStateToken_NotFound() {
    assertThatThrownBy(() -> service.handleInstallationSetupCallback(INSTALLATION_ID, "invalid-state", OAUTH_CODE))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Invalid or expired state parameter");
  }

  @Test
  public void testHandleInstallationSetupCallback_ExpiredStateToken() {
    Date pastDate = new Date(System.currentTimeMillis() - 60000);
    createInstallationState("expired-state", gitHubApp.getId(), pastDate);

    assertThatThrownBy(() -> service.handleInstallationSetupCallback(INSTALLATION_ID, "expired-state", OAUTH_CODE))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Invalid or expired state parameter");

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      GitHubAppInstallationState deletedState = installationStateDAO.findByStateToken(tx, "expired-state");
      assertThat(deletedState).isNull();
    }
  }

  @Test
  public void testHandleInstallationSetupCallback_EmptyClientSecret() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-empty-secret", gitHubApp.getId(), futureDate);

    GitHubApp app = getGitHubAppByOwnerId(organization.getId());
    app.setClientSecret("");
    updateGitHubApp(app);

    // Configure WireMock to reject OAuth token exchange with empty client_secret (simulating GitHub API behavior)
    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .withRequestBody(containing("\"client_secret\":\"\""))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"bad_verification_code\","
                    + "\"error_description\":\"The code passed is incorrect or expired.\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-empty-secret", OAUTH_CODE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("clientSecret is required");
  }

  @Test
  public void testHandleInstallationSetupCallback_TokenExpiryBoundary() {
    Date almostExpired = new Date(System.currentTimeMillis() + 1000);
    createInstallationState("almost-expired", gitHubApp.getId(), almostExpired);

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      GitHubAppInstallationState retrieved = installationStateDAO.findByStateToken(tx, "almost-expired");
      assertThat(retrieved.isExpired()).isFalse();
    }

    try {
      Thread.sleep(1500);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      GitHubAppInstallationState retrievedAfterSleep = installationStateDAO.findByStateToken(tx, "almost-expired");
      assertThat(retrievedAfterSleep.isExpired()).isTrue();
    }
  }

  @Test
  public void testHandleInstallationSetupCallback_OAuthTokenExchangeFailure_400() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-oauth-400", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"invalid_grant\","
                    + "\"error_description\":\"The provided authorization grant is invalid\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-oauth-400", OAUTH_CODE))
            .isInstanceOf(IOException.class);
  }

  @Test
  public void testHandleInstallationSetupCallback_OAuthTokenExchangeFailure_401() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-oauth-401", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"unauthorized_client\",\"error_description\":" +
                    "\"Client authentication failed\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-oauth-401", OAUTH_CODE))
            .isInstanceOf(IOException.class);
  }

  @Test
  public void testHandleInstallationSetupCallback_OAuthTokenExchangeFailure_500() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-oauth-500", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Internal server error\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-oauth-500", OAUTH_CODE))
            .isInstanceOf(IOException.class);
  }

  @Test
  public void testHandleInstallationSetupCallback_UserInstallationsApiFailure_404() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-installations-404", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Not Found\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-installations-404", OAUTH_CODE))
            .isInstanceOf(IOException.class);
  }

  @Test
  public void testHandleInstallationSetupCallback_UserInstallationsApiFailure_403() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-installations-403", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Forbidden\"}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-installations-403", OAUTH_CODE))
            .isInstanceOf(IOException.class);
  }

  @Test
  public void testHandleInstallationSetupCallback_UserOwnsMultipleInstallations() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-multiple-installs", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":3,\"installations\":["
                    + "{\"id\":11111,\"app_id\":111,"
                    + "\"account\":{\"login\":\"other-org-1\",\"id\":11111}},"
                    + "{\"id\":" + INSTALLATION_ID + ",\"app_id\":" + APP_ID + ","
                    + "\"account\":{\"login\":\"test-org\",\"id\":12345}},"
                    + "{\"id\":33333,\"app_id\":333,"
                    + "\"account\":{\"login\":\"other-org-2\",\"id\":33333}}]}")));

    service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-multiple-installs", OAUTH_CODE);

    GitHubApp updated = gitHubAppDAO.getByOwnerId(organization.getId());
    assertThat(updated.getInstallationId()).isEqualTo(INSTALLATION_ID);
    assertThat(updated.getGithubOrganizationName()).isEqualTo("test-org(personal)");
  }

  @Test
  public void testHandleInstallationSetupCallback_UserDoesNotOwnInstallation_EmptyList() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-empty-installs", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":0,\"installations\":[]}")));

    assertThatThrownBy(
        () -> service.handleInstallationSetupCallback(INSTALLATION_ID, "valid-state-empty-installs", OAUTH_CODE))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("User does not have permission to install this GitHub App");
  }

  private GitHubAppInstallationState createInstallationState(String stateValue, String githubAppId, Date expiresAt) {
    return tempEntity.newGitHubAppInstallationState(stateValue, githubAppId, null, expiresAt);
  }

  @Test
  public void testGenerateManifest_Success() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    // Verify result structure
    assertThat(result).isNotNull();
    assertThat(result.state()).isNotNull().isNotBlank();
    assertThat(result.state()).matches("^[A-Za-z0-9]{32}$");

    Manifest manifest = result.manifest();
    assertThat(manifest).isNotNull();
    assertThat(manifest.name()).startsWith("Sonatype IQ Server ");
    assertThat(manifest.url()).isEqualTo("https://iqserver.example.com");
    assertThat(manifest.redirect_url()).contains("https://iqserver.example.comapi/v2/githubApp/redirect");
    assertThat(manifest.description()).contains("GitHub App for Sonatype IQ Server integration");
    assertThat(manifest.default_permissions()).isNotEmpty();
    assertThat(manifest.request_oauth_on_install()).isTrue();
    assertThat(manifest.setup_on_update()).isTrue();

    try (TransactionContext tx = registrationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppRegistrationState token = registrationStateDAO.findByStateToken(tx, result.state());
      assertThat(token).isNotNull();
      assertThat(token.getStateToken()).isEqualTo(result.state());
      assertThat(token.getOwnerId()).isEqualTo(organization.getId()); // Owner is set during manifest generation
      assertThat(token.getGithubOrganizationName()).isEqualTo("test-org");
      assertThat(token.getExpiresAt()).isAfter(new Date());
      assertThat(token.getCreatedAt()).isBefore(new Date(System.currentTimeMillis() + 1000));
      tx.commit();
    }
    GitHubAppInstallationState deleted = findStateToken("ManifestGenerationTestState");
    assertThat(deleted).isNull();
  }

  @Test
  public void testGenerateManifest_BaseUrlEmpty_ThrowsException() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    assertThatThrownBy(() -> testService.generateManifest(organization.getId(), "test-org"))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("IQ Server base URL must be configured");
  }

  @Test
  public void testGenerateRandomSuffix_AlphanumericOnly() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    for (int i = 0; i < 10; i++) {
      ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
      String suffix = result.manifest().name().replace("Sonatype IQ Server ", "");
      assertThat(suffix).matches("^[A-Za-z0-9]+$");
    }
  }

  @Test
  public void testGenerateManifest_VeryLongBaseUrl() {
    // Test with a very long base URL (2048 characters)
    String longBaseUrl = "https://very-long-subdomain-" + "x".repeat(2000) + ".example.com";
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn(longBaseUrl);

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result).isNotNull();
    assertThat(result.manifest().url()).isEqualTo(longBaseUrl);
    assertThat(result.manifest().redirect_url()).startsWith(longBaseUrl);
  }

  @Test
  public void testGenerateManifest_BaseUrlWithSpecialCharacters() {
    // Test with base URL containing special characters that need encoding
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iq-server.example.com/path with spaces");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result).isNotNull();
    assertThat(result.manifest().url()).contains("path with spaces");
  }

  @Test
  public void testGenerateManifest_BaseUrlWithQueryParameters() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iq-server.example.com:8443?param=value&foo=bar");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result).isNotNull();
    assertThat(result.manifest().url()).contains("?param=value&foo=bar");
  }

  @Test
  public void testGenerateManifest_BaseUrlWithUnicodeCharacters() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iq-server-münchen.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result).isNotNull();
    assertThat(result.manifest().url()).contains("münchen");
  }

  @Test
  public void testGenerateManifest_ConcurrentGeneration_GeneratesUniqueStateTokens() throws Exception {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    // Simulate concurrent manifest generation
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    ConcurrentHashMap<String, Boolean> stateTokens = new ConcurrentHashMap<>();
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
          String stateToken = result.state();

          // Check for duplicate state tokens (race condition)
          Boolean alreadyExists = stateTokens.putIfAbsent(stateToken, true);
          assertThat(alreadyExists).as("State token should be unique: " + stateToken).isNull();
        }
        finally {
          latch.countDown();
        }
      });
    }

    latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
    executor.shutdown();

    // Verify all state tokens are unique
    assertThat(stateTokens).hasSize(threadCount);
  }

  @Test
  public void testGenerateManifest_StateTokenPersistence_VerifyDatabaseState() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    // Verify state token was persisted correctly in database
    try (TransactionContext tx = registrationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppRegistrationState persisted = registrationStateDAO.findByStateToken(tx, result.state());

      assertThat(persisted).isNotNull();
      assertThat(persisted.getStateToken()).isEqualTo(result.state());
      assertThat(persisted.getOwnerId()).isEqualTo(organization.getId());
      assertThat(persisted.getExpiresAt()).isNotNull();
      assertThat(persisted.getCreatedAt()).isNotNull();

      // Verify expiration is approximately 10 minutes from now
      long expectedExpiry = System.currentTimeMillis() + (10 * 60 * 1000);
      long actualExpiry = persisted.getExpiresAt().getTime();
      assertThat(Math.abs(actualExpiry - expectedExpiry)).isLessThan(5000); // Within 5 seconds

      tx.commit();
    }
    GitHubApp updated = getGitHubAppByOwnerId(organization.getId());
    assertThat(updated.getInstallationId()).isEqualTo(INSTALLATION_ID);
  }

  @Test
  public void testGenerateManifest_BaseUrlNull_ThrowsException() {
    when(baseUrl.get()).thenReturn(null);

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    assertThatThrownBy(() -> testService.generateManifest(organization.getId(), "test-org"))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("IQ Server base URL must be configured");
  }

  @Test
  public void testGenerateManifest_BaseUrlBlank_ThrowsException() {
    when(baseUrl.get()).thenReturn(" ");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    assertThatThrownBy(() -> testService.generateManifest(organization.getId(), "test-org"))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("IQ Server base URL must be configured");
  }

  @Test
  public void testGenerateManifest_GeneratesUniqueStateTokens() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result1 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result2 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result3 = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result1.state()).isNotEqualTo(result2.state());
    assertThat(result1.state()).isNotEqualTo(result3.state());
    assertThat(result2.state()).isNotEqualTo(result3.state());

    assertThat(result1.manifest().name()).isNotEqualTo(result2.manifest().name());
    assertThat(result1.manifest().name()).isNotEqualTo(result3.manifest().name());
    assertThat(result2.manifest().name()).isNotEqualTo(result3.manifest().name());
  }

  @Test
  public void testGenerateManifest_StateTokenExpiresInTenMinutes() throws InterruptedException {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    long beforeGenerate = System.currentTimeMillis();
    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
    long afterGenerate = System.currentTimeMillis();

    // generateManifest creates a GitHubAppRegistrationState, not InstallationState
    try (TransactionContext tx = registrationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppRegistrationState token = registrationStateDAO.findByStateToken(tx, result.state());
      assertThat(token).isNotNull();

      long expectedExpiration = beforeGenerate + (10 * 60 * 1000);
      long actualExpiration = token.getExpiresAt().getTime();

      assertThat(actualExpiration).isBetween(
          expectedExpiration - 2000,
          afterGenerate + (10 * 60 * 1000) + 2000);
      tx.commit();
    }
  }

  @Test
  public void testCreateDefaultPermissions_ReturnsExpectedPermissions() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
    Map<String, String> permissions = result.manifest().default_permissions();

    assertThat(permissions).isNotNull();
    assertThat(permissions).hasSize(6);
    assertThat(permissions).containsEntry("contents", "write");
    assertThat(permissions).containsEntry("pull_requests", "write");
  }

  @Test
  public void testGenerateStateToken_GeneratesCorrectLength() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result.state()).hasSize(32);
    assertThat(result.state()).matches("[A-Za-z0-9]+");
  }

  @Test
  public void testGenerateStateToken_GeneratesUniqueTokens() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result1 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result2 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result3 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result4 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result5 = testService.generateManifest(organization.getId(), "test-org");

    assertThat(result1.state()).isNotEqualTo(result2.state());
    assertThat(result1.state()).isNotEqualTo(result3.state());
    assertThat(result1.state()).isNotEqualTo(result4.state());
    assertThat(result1.state()).isNotEqualTo(result5.state());
    assertThat(result2.state()).isNotEqualTo(result3.state());
    assertThat(result2.state()).isNotEqualTo(result4.state());
    assertThat(result3.state()).isNotEqualTo(result4.state());
  }

  @Test
  public void testGenerateStateToken_AlphanumericOnly() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    for (int i = 0; i < 10; i++) {
      ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
      assertThat(result.state()).matches("^[A-Za-z0-9]+$");
    }
  }

  @Test
  public void testGenerateRandomSuffix_GeneratesCorrectLength() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
    String appName = result.manifest().name();

    String suffix = appName.replace("Sonatype IQ Server ", "");
    assertThat(suffix).hasSize(8);
  }

  @Test
  public void testGenerateRandomSuffix_GeneratesUniqueSuffixes() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    ApiGitHubAppManifestDTO result1 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result2 = testService.generateManifest(organization.getId(), "test-org");
    ApiGitHubAppManifestDTO result3 = testService.generateManifest(organization.getId(), "test-org");

    String name1 = result1.manifest().name();
    String name2 = result2.manifest().name();
    String name3 = result3.manifest().name();

    assertThat(name1).isNotEqualTo(name2);
    assertThat(name1).isNotEqualTo(name3);
    assertThat(name2).isNotEqualTo(name3);
  }

  @Test
  public void testGenerateRandomSuffix_UppercaseAlphanumericOnly() {
    when(baseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(

        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        baseUrl);

    for (int i = 0; i < 10; i++) {
      ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "test-org");
      String suffix = result.manifest().name().replace("Sonatype IQ Server ", "");
      assertThat(suffix).matches("^[A-Za-z0-9]+$");
    }
  }

  private String generateCodeVerifier() {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    StringBuilder sb = new StringBuilder(128);
    for (int i = 0; i < 128; i++) {
      sb.append(alphabet.charAt((int) (Math.random() * alphabet.length())));
    }
    return sb.toString();
  }

  @Test
  public void testCreateGitHubAppFromManifest_NullCode_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-null-code",
        organization.getId(), futureDate);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(null, registrationState))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("GitHub manifest conversion code is required");
  }

  @Test
  public void testCreateGitHubAppFromManifest_EmptyCode_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-empty-code",
        organization.getId(), futureDate);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest("", registrationState))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("GitHub manifest conversion code is required");

    assertThatThrownBy(() -> service.createGitHubAppFromManifest("   ", registrationState))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("GitHub manifest conversion code is required");
  }

  @Test
  public void testCreateGitHubAppFromManifest_MissingAppId_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-no-id",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-no-id";
    mockManifestConversionMissingField(manifestCode, null, APP_SLUG, CLIENT_ID, CLIENT_SECRET);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing app ID");
  }

  @Test
  public void testCreateGitHubAppFromManifest_MissingClientId_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-no-client-id",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-no-client-id";
    mockManifestConversionMissingField(manifestCode, APP_ID + 2000, APP_SLUG, null, CLIENT_SECRET);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing client_id");
  }

  @Test
  public void testCreateGitHubAppFromManifest_EmptyClientId_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-empty-client-id",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-empty-client-id";
    mockManifestConversionMissingField(manifestCode, APP_ID + 2001, APP_SLUG, "", CLIENT_SECRET);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing client_id");
  }

  @Test
  public void testCreateGitHubAppFromManifest_MissingClientSecret_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-no-secret",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-no-secret";
    mockManifestConversionMissingClientSecret(manifestCode, APP_ID + 3000, APP_SLUG, CLIENT_ID);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing client_secret");
  }

  @Test
  public void testCreateGitHubAppFromManifest_EmptyClientSecret_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-empty-secret",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-empty-secret";
    mockManifestConversionMissingClientSecret(manifestCode, APP_ID + 3001, APP_SLUG, CLIENT_ID, "");

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing client_secret");
  }

  @Test
  public void testCreateGitHubAppFromManifest_MissingPrivateKey_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-no-pem",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-no-pem";
    mockManifestConversionMissingPem(manifestCode, APP_ID + 4000, APP_SLUG, CLIENT_ID, CLIENT_SECRET);

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing private key");
  }

  @Test
  public void testCreateGitHubAppFromManifest_EmptyPrivateKey_ThrowsException() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-empty-pem",
        organization.getId(), futureDate);

    String manifestCode = "test-manifest-code-empty-pem";
    mockManifestConversionMissingPem(manifestCode, APP_ID + 4001, APP_SLUG, CLIENT_ID, CLIENT_SECRET, "");

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(InternalServerErrorException.class)
        .hasMessageContaining("Invalid response from GitHub: missing private key");
  }

  @Test
  public void testCreateGitHubAppFromManifest_GitHubApiFailure_PropagatesException_Empty_manifest_code() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("manifest-state-api-fail",
        organization.getId(), futureDate);

    String manifestCode = "";

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + manifestCode + "/conversions"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Internal server error\"}")));

    assertThatThrownBy(() -> service.createGitHubAppFromManifest(manifestCode, registrationState))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void testCreateGitHubAppFromManifest_InsertsNewAppAsInactive_KeepsExistingActive() throws Exception {
    deleteExistingGitHubAppForOwner(organization.getId());

    GitHubApp existingApp = createGitHubApp(999999, "old-app-slug", "old-client-id",
        organization.getId(), "old-org", null);

    GitHubApp retrievedBefore = gitHubAppDAO.getByOwnerId(organization.getId());
    assertThat(retrievedBefore).isNotNull();
    assertThat(retrievedBefore.getAppId()).isEqualTo(999999);
    assertThat(retrievedBefore.getSlug()).isEqualTo("old-app-slug");

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("replace-app-state",
        organization.getId(), futureDate);

    String manifestCode = "replace-app-code";
    Integer newAppId = APP_ID + 5000;
    String newSlug = "new-app-slug";
    mockManifestConversion(manifestCode, newAppId, newSlug);

    service.createGitHubAppFromManifest(manifestCode, registrationState);

    List<GitHubApp> allApps = gitHubAppDAO.getAllByOwnerId(organization.getId());
    assertThat(allApps).hasSize(2);

    GitHubApp newApp = allApps.stream()
        .filter(app -> app.getAppId().equals(newAppId))
        .findFirst()
        .orElse(null);
    assertThat(newApp).isNotNull();
    assertThat(newApp.getSlug()).isEqualTo(newSlug);
    assertThat(newApp.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(newApp.getOwnerId()).isEqualTo(organization.getId());
    assertThat(newApp.getId()).isNotEqualTo(existingApp.getId());
    assertThat(newApp.isActive()).isFalse();

    GitHubApp oldApp = gitHubAppDAO.getById(existingApp.getId());
    assertThat(oldApp).isNotNull();
    assertThat(oldApp.isActive()).isTrue();
  }

  @Test
  public void testCreateGitHubAppFromManifest_ReplaceMultipleTimes_EachInsertsNewInactive() throws Exception {
    deleteExistingGitHubAppForOwner(organization.getId());

    Date futureDate1 = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState state1 = createRegistrationState("first-app-state",
        organization.getId(), futureDate1);
    String code1 = "first-app-code";
    Integer appId1 = APP_ID + 7000;
    mockManifestConversion(code1, appId1, "first-slug");
    service.createGitHubAppFromManifest(code1, state1);

    List<GitHubApp> appsAfterFirst = gitHubAppDAO.getAllByOwnerId(organization.getId());
    assertThat(appsAfterFirst).hasSize(1);
    assertThat(appsAfterFirst.get(0).getAppId()).isEqualTo(appId1);
    assertThat(appsAfterFirst.get(0).isActive()).isFalse();

    Date futureDate2 = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState state2 = createRegistrationState("second-app-state",
        organization.getId(), futureDate2);
    String code2 = "second-app-code";
    Integer appId2 = APP_ID + 8000;
    mockManifestConversion(code2, appId2, "second-slug");
    service.createGitHubAppFromManifest(code2, state2);

    List<GitHubApp> appsAfterSecond = gitHubAppDAO.getAllByOwnerId(organization.getId());
    assertThat(appsAfterSecond).hasSize(2);

    Date futureDate3 = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState state3 = createRegistrationState("third-app-state",
        organization.getId(), futureDate3);
    String code3 = "third-app-code";
    Integer appId3 = APP_ID + 9000;
    mockManifestConversion(code3, appId3, "third-slug");
    service.createGitHubAppFromManifest(code3, state3);

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      List<GitHubApp> allApps = gitHubAppDAO.getAll(tx);
      List<GitHubApp> ownerApps = allApps.stream()
          .filter(app -> organization.getId().equals(app.getOwnerId()))
          .toList();
      assertThat(ownerApps).hasSize(3);
      assertThat(ownerApps.get(0).getAppId()).isEqualTo(appId1);
      assertThat(ownerApps.get(0).isActive()).isFalse();
      assertThat(ownerApps.get(1).getAppId()).isEqualTo(appId2);
      assertThat(ownerApps.get(1).isActive()).isFalse();
      assertThat(ownerApps.get(2).getAppId()).isEqualTo(appId3);
      assertThat(ownerApps.get(2).isActive()).isFalse();
      tx.commit();
    }
  }

  @Test
  public void testCreateGitHubAppFromManifest_WithPendingInstallationStates_CreatesNewAppWithoutDeletion() throws Exception {
    deleteExistingGitHubAppForOwner(organization.getId());

    GitHubApp existingApp = createGitHubApp(999999, "old-app-slug", "old-client-id",
        organization.getId(), "old-org", 888888L);

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("pending-state-1", existingApp.getId(), "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("pending-state-2", existingApp.getId(), "code-verifier-2", expiresAt);

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-2")).isNotNull();
      tx.commit();
    }

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("replace-with-states",
        organization.getId(), futureDate);

    String manifestCode = "replace-code-with-states";
    Integer newAppId = APP_ID + 10000;
    String newSlug = "new-app-after-states-cleanup";
    mockManifestConversion(manifestCode, newAppId, newSlug);

    service.createGitHubAppFromManifest(manifestCode, registrationState);

    List<GitHubApp> allApps = gitHubAppDAO.getAllByOwnerId(organization.getId());
    assertThat(allApps).hasSize(2);

    GitHubApp newApp = allApps.stream()
        .filter(app -> app.getAppId().equals(newAppId))
        .findFirst()
        .orElse(null);
    assertThat(newApp).isNotNull();
    assertThat(newApp.getAppId()).isEqualTo(newAppId);
    assertThat(newApp.getSlug()).isEqualTo(newSlug);
    assertThat(newApp.isActive()).isFalse();

    GitHubApp oldApp = gitHubAppDAO.getById(existingApp.getId());
    assertThat(oldApp).isNotNull();
    assertThat(oldApp.isActive()).isTrue();

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "pending-state-2")).isNotNull();
      tx.commit();
    }
  }

  @Test
  public void testCreateGitHubAppFromManifest_MultiplePendingStates_CreatesNewAppWithoutDeletion() throws Exception {
    deleteExistingGitHubAppForOwner(organization.getId());

    GitHubApp existingApp = createGitHubApp(999998, "app-with-many-states", "client-many-states",
        organization.getId(), "org-many-states", 888887L);

    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("state-1", existingApp.getId(), "verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("state-2", existingApp.getId(), "verifier-2", expiresAt);
    tempEntity.newGitHubAppInstallationState("state-3", existingApp.getId(), "verifier-3", expiresAt);
    tempEntity.newGitHubAppInstallationState("state-4", existingApp.getId(), "verifier-4", expiresAt);

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-2")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-3")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-4")).isNotNull();
      tx.commit();
    }

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = createRegistrationState("replace-many-states",
        organization.getId(), futureDate);

    String manifestCode = "replace-code-many-states";
    Integer newAppId = APP_ID + 11000;
    String newSlug = "new-app-after-many-states";
    mockManifestConversion(manifestCode, newAppId, newSlug);

    service.createGitHubAppFromManifest(manifestCode, registrationState);

    List<GitHubApp> allApps = gitHubAppDAO.getAllByOwnerId(organization.getId());
    assertThat(allApps).hasSize(2);

    GitHubApp newApp = allApps.stream()
        .filter(app -> app.getAppId().equals(newAppId))
        .findFirst()
        .orElse(null);
    assertThat(newApp).isNotNull();
    assertThat(newApp.getAppId()).isEqualTo(newAppId);
    assertThat(newApp.isActive()).isFalse();

    GitHubApp oldApp = gitHubAppDAO.getById(existingApp.getId());
    assertThat(oldApp).isNotNull();
    assertThat(oldApp.isActive()).isTrue();

    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      assertThat(installationStateDAO.findByStateToken(tx, "state-1")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-2")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-3")).isNotNull();
      assertThat(installationStateDAO.findByStateToken(tx, "state-4")).isNotNull();
      tx.commit();
    }
  }

  private GitHubApp createGitHubApp(
      Integer appId,
      String slug,
      String clientId,
      String ownerId,
      String githubOrgName,
      Long installationId)
  {
    GitHubApp app = new GitHubApp();
    app.setAppId(appId);
    app.setSlug(slug);
    app.setClientId(clientId);
    try {
      app.setClientSecret(passwordHandler.encryptPassword("test-secret-" + appId));
      app.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to encrypt password or private key", e);
    }
    app.setOwnerId(ownerId);
    app.setGithubOrganizationName(githubOrgName);
    app.setLastUpdatedAt(new Date());
    app.setInstallationId(installationId);
    app.setActive(true);
    return tempEntity.newGitHubApp(app);
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

  private void deleteExistingGitHubAppForOwner(String ownerId) {
    GitHubApp existingApp = gitHubAppDAO.getByOwnerId(ownerId);
    if (existingApp != null) {
      try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
        tx.begin();
        gitHubAppDAO.delete(tx, existingApp);
        tx.commit();
      }
    }
  }

  private void insertStateToken(GitHubAppInstallationState token) {
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      installationStateDAO.insert(tx, token);
      tx.commit();
    }
  }

  private GitHubAppInstallationState findStateToken(String stateValue) {
    try (TransactionContext tx = installationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState result = installationStateDAO.findByStateToken(tx, stateValue);
      tx.commit();
      return result;
    }
  }

  private void insertSourceControl(SourceControl sourceControl) {
    try (TransactionContext tx = sourceControlDAO.createTransactionContext()) {
      tx.begin();
      sourceControlDAO.insert(tx, sourceControl);
      tx.commit();
    }
  }

  private SourceControl getSourceControlByOwnerId(String ownerId) {
    try (TransactionContext tx = sourceControlDAO.createTransactionContext()) {
      tx.begin();
      SourceControl result = sourceControlDAO.getByOwnerId(tx, ownerId);
      tx.commit();
      return result;
    }
  }

  private void deleteSourceControlByOwnerId(String ownerId) {
    try (TransactionContext tx = sourceControlDAO.createTransactionContext()) {
      tx.begin();
      SourceControl existing = sourceControlDAO.getByOwnerId(tx, ownerId);
      if (existing != null) {
        sourceControlDAO.delete(tx, existing);
      }
      tx.commit();
    }
  }

  private void insertGitHubApp(GitHubApp app) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      gitHubAppDAO.insert(tx, app);
      tx.commit();
    }
  }

  private void updateGitHubApp(GitHubApp app) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      gitHubAppDAO.update(tx, app);
      tx.commit();
    }
  }

  private GitHubApp getGitHubAppByOwnerId(String ownerId) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      GitHubApp result = gitHubAppDAO.getByOwnerId(tx, ownerId);
      tx.commit();
      return result;
    }
  }

  private GitHubApp getGitHubAppByAppId(Integer appId) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      GitHubApp result = gitHubAppDAO.getByAppId(tx, appId);
      tx.commit();
      return result;
    }
  }

  private GitHubAppRegistrationState createRegistrationState(String stateToken, String ownerId, Date expiresAt) {
    return tempEntity.newGitHubAppRegistrationState(stateToken, ownerId, expiresAt);
  }

  private void mockManifestConversion(String code, Integer appId, String slug) {
    String privateKeyPem = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEogIBAAKCAQEAr8QX8ucHKiSq36qP82OnlHF+v1XbSDuws2zovOtHa/RW8TgV\n" +
        "151K8lzQ7IJAmzrrbg2zQfwmc3mlVTB0/9zF6f3tAIdG8dulZvCM3qw3PBQW4L8l\n" +
        "FCT/TpiAlbZk7AOAnrj6t8rOnlUQpcnj3SfYYqN2yoLoltGURHL/KxBlWY+mToO7\n" +
        "xCIr7yZjoc2XNEQbNX0MI/NIRzJc4r/eJYGg1fwjBkge7bGY2uuRtSyEJsh+RBMz\n" +
        "cLEgR31gvmM4q0F4aXWB105ItJvEYsIbW6F4K5y0c+FSOYumUq1k7gzSJ+bzBIZT\n" +
        "2HZgAi4KDCHmfsjZJAa5fvtIoY93g+y9DCxY/QIDAQABAoIBAAWPH6zJIjwXWTov\n" +
        "VFPI/pmnrQI3xU3ue6e7Z98IDtZNUm3G0kO+4i67eBGUCw00rRH8Xsywrtpc18vB\n" +
        "Us6SSw9S4yrcd+G+ip55poCpS5iAWU0s3DRJsCEzdbmrCoCIvmVP5SWNIOX3hKV9\n" +
        "JIkv6DfpTA5Rf8RyhuflUKQFioTIzZO+aH6plbPHepnh+wIo73BURgMHSSezxotl\n" +
        "3jpPYMOLk8V0xr8zvChqgF8bQvA+RQqEnaPK0R+sISiDG+QMu1cHN9eSW2iv7dM0\n" +
        "A1hAecOYGeX+juh/VuzRE+172ywFA99h7wcSdgMxpzt7IkczUBTQds5fHlZtLsdu\n" +
        "U+/7zRECgYEA8T84Td8Zxgl7nWnt4NRKVRc2qAjxAsISlx6l/MDopV6Tda6/xrzk\n" +
        "P/Pbokdf8nN+/Yq2GXsW5t23I3O0CIMXvDcQpDcZr0SGHaOn0nXYxAlUGEP21Ut1\n" +
        "Soyti+X5G9kwlV0o81yngP5AYY5QRd22RvFlshgKFSWZ0qaFT7fcyFUCgYEAuoPA\n" +
        "zOfWTpAHccsC84lrLMM5sIthK+lrQbeqEPApw/XWEE9E/cvWu7t2fRE7rJs0UCIh\n" +
        "NVPBDPPdufGqb7LKmMWLG0xtOFkvf2uSQ6RyBwYY0sPEFwN+ZRHpIwDsOWErwxl7\n" +
        "KV1AMcewsyP1dMLbjwziO9CsCe2OMRdQlgVqFgkCgYAEMklUcXENVNTlpBYTNx4j\n" +
        "5Md6nM00cxPHtSzF/MUPO1ntTiDf4CFIS4GijQNKQGARIPyR7OY1Fd49q6GSFFWx\n" +
        "XHPZp2u29MYwdcxRiONAZbkkwunkQ+/CYDgUmud+aITD1F8F/LKdN87+427aCEVH\n" +
        "bqOKOYjTXVgTpfnjrRsWEQKBgB3Ckgvf3iEQ+C8e/myPe6tbxyO1SZ7xEq0cuiUT\n" +
        "vQZIfoyBqXd5g9zWj5RrIINtDE7Q802H/KCtdK6Lse86rvrrYkPL0Q2RpXOGXYMv\n" +
        "hQY74dAXbn1hkFReJD3ykr6hE5OAyFcUSv7mZvpefXbQ9KmBm8OBi0HWRr7sgm49\n" +
        "lOzJAoGAfQb6LdG9StBmEIB+hOApHIkBqD3/U/p+5UiFmImtED0J5/syk9oEjcKG\n" +
        "MwsBdW+0Xxv5kC1WrKqnodqyPi1coLpJJugnNNxtekqnJonGlRaLaPRkoCOmV9sp\n" +
        "4nB+wefAwEWa2cczC0S3fjbw4VJy1P9mXKYdevus3JRJDoAg/hs=\n" +
        "-----END RSA PRIVATE KEY-----";

    String responseJson = String.format(
        "{\"id\":%d,\"slug\":\"%s\",\"client_id\":\"%s\",\"client_secret\":\"%s\",\"pem\":\"%s\",\"owner\":" +
            "{\"login\":\"test-owner\",\"id\":12345}}",
        appId, slug, CLIENT_ID, CLIENT_SECRET, privateKeyPem.replace("\n", "\\n"));

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + code + "/conversions"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)));
  }

  private void mockManifestConversionMissingField(
      String code,
      Integer appId,
      String slug,
      String clientId,
      String clientSecret)
  {
    String privateKeyPem = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEogIBAAKCAQEAr8QX8ucHKiSq36qP82OnlHF+v1XbSDuws2zovOtHa/RW8TgV\n" +
        "151K8lzQ7IJAmzrrbg2zQfwmc3mlVTB0/9zF6f3tAIdG8dulZvCM3qw3PBQW4L8l\n" +
        "FCT/TpiAlbZk7AOAnrj6t8rOnlUQpcnj3SfYYqN2yoLoltGURHL/KxBlWY+mToO7\n" +
        "xCIr7yZjoc2XNEQbNX0MI/NIRzJc4r/eJYGg1fwjBkge7bGY2uuRtSyEJsh+RBMz\n" +
        "cLEgR31gvmM4q0F4aXWB105ItJvEYsIbW6F4K5y0c+FSOYumUq1k7gzSJ+bzBIZT\n" +
        "2HZgAi4KDCHmfsjZJAa5fvtIoY93g+y9DCxY/QIDAQABAoIBAAWPH6zJIjwXWTov\n" +
        "VFPI/pmnrQI3xU3ue6e7Z98IDtZNUm3G0kO+4i67eBGUCw00rRH8Xsywrtpc18vB\n" +
        "Us6SSw9S4yrcd+G+ip55poCpS5iAWU0s3DRJsCEzdbmrCoCIvmVP5SWNIOX3hKV9\n" +
        "JIkv6DfpTA5Rf8RyhuflUKQFioTIzZO+aH6plbPHepnh+wIo73BURgMHSSezxotl\n" +
        "3jpPYMOLk8V0xr8zvChqgF8bQvA+RQqEnaPK0R+sISiDG+QMu1cHN9eSW2iv7dM0\n" +
        "A1hAecOYGeX+juh/VuzRE+172ywFA99h7wcSdgMxpzt7IkczUBTQds5fHlZtLsdu\n" +
        "U+/7zRECgYEA8T84Td8Zxgl7nWnt4NRKVRc2qAjxAsISlx6l/MDopV6Tda6/xrzk\n" +
        "P/Pbokdf8nN+/Yq2GXsW5t23I3O0CIMXvDcQpDcZr0SGHaOn0nXYxAlUGEP21Ut1\n" +
        "Soyti+X5G9kwlV0o81yngP5AYY5QRd22RvFlshgKFSWZ0qaFT7fcyFUCgYEAuoPA\n" +
        "zOfWTpAHccsC84lrLMM5sIthK+lrQbeqEPApw/XWEE9E/cvWu7t2fRE7rJs0UCIh\n" +
        "NVPBDPPdufGqb7LKmMWLG0xtOFkvf2uSQ6RyBwYY0sPEFwN+ZRHpIwDsOWErwxl7\n" +
        "KV1AMcewsyP1dMLbjwziO9CsCe2OMRdQlgVqFgkCgYAEMklUcXENVNTlpBYTNx4j\n" +
        "5Md6nM00cxPHtSzF/MUPO1ntTiDf4CFIS4GijQNKQGARIPyR7OY1Fd49q6GSFFWx\n" +
        "XHPZp2u29MYwdcxRiONAZbkkwunkQ+/CYDgUmud+aITD1F8F/LKdN87+427aCEVH\n" +
        "bqOKOYjTXVgTpfnjrRsWEQKBgB3Ckgvf3iEQ+C8e/myPe6tbxyO1SZ7xEq0cuiUT\n" +
        "vQZIfoyBqXd5g9zWj5RrIINtDE7Q802H/KCtdK6Lse86rvrrYkPL0Q2RpXOGXYMv\n" +
        "hQY74dAXbn1hkFReJD3ykr6hE5OAyFcUSv7mZvpefXbQ9KmBm8OBi0HWRr7sgm49\n" +
        "lOzJAoGAfQb6LdG9StBmEIB+hOApHIkBqD3/U/p+5UiFmImtED0J5/syk9oEjcKG\n" +
        "MwsBdW+0Xxv5kC1WrKqnodqyPi1coLpJJugnNNxtekqnJonGlRaLaPRkoCOmV9sp\n" +
        "4nB+wefAwEWa2cczC0S3fjbw4VJy1P9mXKYdevus3JRJDoAg/hs=\n" +
        "-----END RSA PRIVATE KEY-----";

    StringBuilder jsonBuilder = new StringBuilder("{");
    if (appId != null) {
      jsonBuilder.append("\"id\":").append(appId).append(",");
    }
    if (slug != null) {
      jsonBuilder.append("\"slug\":\"").append(slug).append("\",");
    }
    if (clientId != null) {
      jsonBuilder.append("\"client_id\":\"").append(clientId).append("\",");
    }
    if (clientSecret != null) {
      jsonBuilder.append("\"client_secret\":\"").append(clientSecret).append("\",");
    }
    jsonBuilder.append("\"pem\":\"").append(privateKeyPem.replace("\n", "\\n")).append("\",");
    jsonBuilder.append("\"owner\":{\"login\":\"test-owner\",\"id\":12345}");
    jsonBuilder.append("}");

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + code + "/conversions"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBuilder.toString())));
  }

  private void mockManifestConversionMissingClientSecret(String code, Integer appId, String slug, String clientId) {
    mockManifestConversionMissingClientSecret(code, appId, slug, clientId, null);
  }

  private void mockManifestConversionMissingClientSecret(
      String code,
      Integer appId,
      String slug,
      String clientId,
      String clientSecret)
  {
    String privateKeyPem = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEogIBAAKCAQEAr8QX8ucHKiSq36qP82OnlHF+v1XbSDuws2zovOtHa/RW8TgV\n" +
        "151K8lzQ7IJAmzrrbg2zQfwmc3mlVTB0/9zF6f3tAIdG8dulZvCM3qw3PBQW4L8l\n" +
        "FCT/TpiAlbZk7AOAnrj6t8rOnlUQpcnj3SfYYqN2yoLoltGURHL/KxBlWY+mToO7\n" +
        "xCIr7yZjoc2XNEQbNX0MI/NIRzJc4r/eJYGg1fwjBkge7bGY2uuRtSyEJsh+RBMz\n" +
        "cLEgR31gvmM4q0F4aXWB105ItJvEYsIbW6F4K5y0c+FSOYumUq1k7gzSJ+bzBIZT\n" +
        "2HZgAi4KDCHmfsjZJAa5fvtIoY93g+y9DCxY/QIDAQABAoIBAAWPH6zJIjwXWTov\n" +
        "VFPI/pmnrQI3xU3ue6e7Z98IDtZNUm3G0kO+4i67eBGUCw00rRH8Xsywrtpc18vB\n" +
        "Us6SSw9S4yrcd+G+ip55poCpS5iAWU0s3DRJsCEzdbmrCoCIvmVP5SWNIOX3hKV9\n" +
        "JIkv6DfpTA5Rf8RyhuflUKQFioTIzZO+aH6plbPHepnh+wIo73BURgMHSSezxotl\n" +
        "3jpPYMOLk8V0xr8zvChqgF8bQvA+RQqEnaPK0R+sISiDG+QMu1cHN9eSW2iv7dM0\n" +
        "A1hAecOYGeX+juh/VuzRE+172ywFA99h7wcSdgMxpzt7IkczUBTQds5fHlZtLsdu\n" +
        "U+/7zRECgYEA8T84Td8Zxgl7nWnt4NRKVRc2qAjxAsISlx6l/MDopV6Tda6/xrzk\n" +
        "P/Pbokdf8nN+/Yq2GXsW5t23I3O0CIMXvDcQpDcZr0SGHaOn0nXYxAlUGEP21Ut1\n" +
        "Soyti+X5G9kwlV0o81yngP5AYY5QRd22RvFlshgKFSWZ0qaFT7fcyFUCgYEAuoPA\n" +
        "zOfWTpAHccsC84lrLMM5sIthK+lrQbeqEPApw/XWEE9E/cvWu7t2fRE7rJs0UCIh\n" +
        "NVPBDPPdufGqb7LKmMWLG0xtOFkvf2uSQ6RyBwYY0sPEFwN+ZRHpIwDsOWErwxl7\n" +
        "KV1AMcewsyP1dMLbjwziO9CsCe2OMRdQlgVqFgkCgYAEMklUcXENVNTlpBYTNx4j\n" +
        "5Md6nM00cxPHtSzF/MUPO1ntTiDf4CFIS4GijQNKQGARIPyR7OY1Fd49q6GSFFWx\n" +
        "XHPZp2u29MYwdcxRiONAZbkkwunkQ+/CYDgUmud+aITD1F8F/LKdN87+427aCEVH\n" +
        "bqOKOYjTXVgTpfnjrRsWEQKBgB3Ckgvf3iEQ+C8e/myPe6tbxyO1SZ7xEq0cuiUT\n" +
        "vQZIfoyBqXd5g9zWj5RrIINtDE7Q802H/KCtdK6Lse86rvrrYkPL0Q2RpXOGXYMv\n" +
        "hQY74dAXbn1hkFReJD3ykr6hE5OAyFcUSv7mZvpefXbQ9KmBm8OBi0HWRr7sgm49\n" +
        "lOzJAoGAfQb6LdG9StBmEIB+hOApHIkBqD3/U/p+5UiFmImtED0J5/syk9oEjcKG\n" +
        "MwsBdW+0Xxv5kC1WrKqnodqyPi1coLpJJugnNNxtekqnJonGlRaLaPRkoCOmV9sp\n" +
        "4nB+wefAwEWa2cczC0S3fjbw4VJy1P9mXKYdevus3JRJDoAg/hs=\n" +
        "-----END RSA PRIVATE KEY-----";

    StringBuilder jsonBuilder = new StringBuilder("{");
    jsonBuilder.append("\"id\":").append(appId).append(",");
    jsonBuilder.append("\"slug\":\"").append(slug).append("\",");
    jsonBuilder.append("\"client_id\":\"").append(clientId).append("\",");
    if (clientSecret != null) {
      jsonBuilder.append("\"client_secret\":\"").append(clientSecret).append("\",");
    }
    jsonBuilder.append("\"pem\":\"").append(privateKeyPem.replace("\n", "\\n")).append("\",");
    jsonBuilder.append("\"owner\":{\"login\":\"test-owner\",\"id\":12345}");
    jsonBuilder.append("}");

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + code + "/conversions"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBuilder.toString())));
  }

  private void mockManifestConversionMissingPem(
      String code,
      Integer appId,
      String slug,
      String clientId,
      String clientSecret)
  {
    mockManifestConversionMissingPem(code, appId, slug, clientId, clientSecret, null);
  }

  private void mockManifestConversionMissingPem(
      String code,
      Integer appId,
      String slug,
      String clientId,
      String clientSecret,
      String pem)
  {
    StringBuilder jsonBuilder = new StringBuilder("{");
    jsonBuilder.append("\"id\":").append(appId).append(",");
    jsonBuilder.append("\"slug\":\"").append(slug).append("\",");
    jsonBuilder.append("\"client_id\":\"").append(clientId).append("\",");
    jsonBuilder.append("\"client_secret\":\"").append(clientSecret).append("\",");
    if (pem != null) {
      jsonBuilder.append("\"pem\":\"").append(pem.replace("\n", "\\n")).append("\",");
    }
    jsonBuilder.append("\"owner\":{\"login\":\"test-owner\",\"id\":12345}");
    jsonBuilder.append("}");

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + code + "/conversions"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBuilder.toString())));
  }

  @Test
  public void testGenerateManifest_WithOrganizationName_StoresCorrectly() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), "my-github-org");

    try (TransactionContext tx = registrationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppRegistrationState token = registrationStateDAO.findByStateToken(tx, result.state());
      assertThat(token).isNotNull();
      assertThat(token.getGithubOrganizationName()).isEqualTo("my-github-org");
      assertThat(token.getOwnerId()).isEqualTo(organization.getId());
      tx.commit();
    }
  }

  @Test
  public void testGenerateManifest_WithNullOrganizationName_StoresNull() {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");

    ApiGitHubAppService testService = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        authStrategyCache,
        gitHubAppDeletionService,
        mockBaseUrl);

    ApiGitHubAppManifestDTO result = testService.generateManifest(organization.getId(), null);

    try (TransactionContext tx = registrationStateDAO.createTransactionContext()) {
      tx.begin();
      GitHubAppRegistrationState token = registrationStateDAO.findByStateToken(tx, result.state());
      assertThat(token).isNotNull();
      assertThat(token.getGithubOrganizationName()).isEqualTo("(personal)");
      assertThat(token.getOwnerId()).isEqualTo(organization.getId());
      tx.commit();
    }
  }
}
