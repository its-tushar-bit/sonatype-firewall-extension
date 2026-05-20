/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.Date;

import jakarta.inject.Inject;

import com.google.inject.Binder;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppRegistrationStateDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.git.GitHubManifestService;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightProxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class ApiGitHubAppServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final Long INSTALLATION_ID = 98765L;

  private static final String OAUTH_CODE = "test-oauth-code";

  private static final String CLIENT_ID = "Iv1.test-client-id";

  private static final String CLIENT_SECRET = "test-client-secret";

  private static final String APP_SLUG = "test-app-slug";

  private static final Integer APP_ID = 123456;

  private static final String ACCESS_TOKEN = "ghu_test_access_token";

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private ApiGitHubAppService service;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private GitHubAppInstallationStateDAO installationStateDAO;

  @Inject
  private GitHubAppRegistrationStateDAO registrationStateDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private GitHubManifestService gitHubManifestService;

  @Inject
  private InsightProxy insightProxy;

  private ApiGitHubAppService serviceWithMocks;

  @Override
  public void configure(Binder binder) {
    BaseUrl mockBaseUrl = mock(BaseUrl.class);
    lenient().when(mockBaseUrl.get()).thenReturn("https://iqserver.example.com");
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }

  @Before
  public void setupGitHubMocks() {
    String mockServerUrl = githubMockServer.baseUrl();

    // Create service instance with WireMock URLs for tests that need HTTP interactions
    serviceWithMocks = new ApiGitHubAppService(
        gitHubAppDAO,
        installationStateDAO,
        registrationStateDAO,
        sourceControlDAO,
        ownerDAO,
        passwordHandler,
        insightProxy,
        gitHubManifestService,
        mock(GitHubAppAuthStrategyCache.class),
        mock(GitHubAppSelectionCache.class),
        mock(GitHubAppDeletionService.class),
        mockServerUrl, // githubApiBaseUrl
        mockServerUrl, // githubOAuthTokenUrl
        mock(BaseUrl.class));

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"bearer\",\"scope\":\"\"}")));

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("token " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"login\":\"testuser\",\"id\":12345}")));

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .withHeader("Authorization", equalTo("token " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":1,\"installations\":[{\"id\":" + INSTALLATION_ID
                    + ",\"app_id\":" + APP_ID + ",\"account\":{\"login\":\"test-org\",\"id\":12345}}]}")));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGenerateManifest_Unauthenticated() {
    service.generateManifest(org.getId(), "test-org");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGenerateManifest_Unauthorized() {
    login();
    service.generateManifest(org.getId(), "test-org");
  }

  @Test
  public void testGenerateManifest_Authorized() {
    // User with minimal WRITE permission on the organization can call generateManifest
    grantWritePermission(org.getId());

    // Should not throw UnauthorizedException with WRITE permission
    service.generateManifest(org.getId(), "test-org");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testHandleInstallationSetupCallback_Unauthenticated() throws Exception {
    GitHubApp gitHubApp = createGitHubApp();

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("state-unauth", gitHubApp.getId(), null, futureDate);

    service.handleInstallationSetupCallback(INSTALLATION_ID, "state-unauth", OAUTH_CODE);
  }

  @Test(expected = UnauthorizedException.class)
  public void testHandleInstallationSetupCallback_Unauthorized() throws Exception {
    GitHubApp gitHubApp = createGitHubApp();
    login();

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("state-no-perm", gitHubApp.getId(), null, futureDate);

    service.handleInstallationSetupCallback(INSTALLATION_ID, "state-no-perm", OAUTH_CODE);
  }

  @Test
  public void testHandleInstallationSetupCallback_Authorized() throws Exception {
    grantWritePermission(org.getId());
    GitHubApp gitHubApp = createGitHubApp();
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("state-authorized", gitHubApp.getId(), null, futureDate);

    // User with minimal WRITE permission can handle installation setup callback
    // Using serviceWithMocks which is configured with WireMock URLs for OAuth and GitHub API
    serviceWithMocks.handleInstallationSetupCallback(INSTALLATION_ID, "state-authorized", OAUTH_CODE);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testHandleManifestConversionAndRegistration_Unauthenticated() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = tempEntity.newGitHubAppRegistrationState(
        "manifest-state-unauth", org.getId(), futureDate);

    service.handleManifestConversionAndRegistration(
        "test-code", registrationState.getStateToken());
  }

  @Test(expected = UnauthorizedException.class)
  public void testHandleManifestConversionAndRegistration_Unauthorized() throws Exception {
    login();

    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppRegistrationState registrationState = tempEntity.newGitHubAppRegistrationState(
        "manifest-state-no-perm", org.getId(), futureDate);

    service.handleManifestConversionAndRegistration(
        "test-code", registrationState.getStateToken());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testListGitHubApps_Unauthenticated() {
    service.listGitHubApps(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testListGitHubApps_Unauthorized() {
    login();
    service.listGitHubApps(org.getId());
  }

  @Test
  public void testListGitHubApps_Authorized() {
    grantWritePermission(org.getId());
    service.listGitHubApps(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteGitHubApp_Unauthenticated() {
    GitHubApp gitHubApp = createGitHubApp();
    service.deleteGitHubApp(gitHubApp.getId(), org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteGitHubApp_Unauthorized() {
    GitHubApp gitHubApp = createGitHubApp();
    login();
    service.deleteGitHubApp(gitHubApp.getId(), org.getId());
  }

  @Test
  public void testDeleteGitHubApp_Authorized() {
    GitHubApp gitHubApp = createGitHubApp();
    grantWritePermission(org.getId());
    service.deleteGitHubApp(gitHubApp.getId(), org.getId());
  }

  private GitHubApp createGitHubApp() {
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setAppId(APP_ID);
    gitHubApp.setSlug(APP_SLUG);
    gitHubApp.setClientId(CLIENT_ID);
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(CLIENT_SECRET));
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setOwnerId(org.getId());
    gitHubApp.setGithubOrganizationName("test-org");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setInstallationId(INSTALLATION_ID);
    return tempEntity.newGitHubApp(gitHubApp);
  }
}
