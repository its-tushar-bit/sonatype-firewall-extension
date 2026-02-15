/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.Date;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.githubapp.ApiGitHubAppResource;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApiGitHubAppResource}.
 *
 * Tests the REST endpoint for GitHub App installation setup callback.
 * Uses WireMock to mock GitHub OAuth and API endpoints.
 */
public class ApiGitHubAppResourceTest
    extends AbstractResourceTest
{
  private static final String CLIENT_ID = "Iv1.test-client-id";

  private static final String CLIENT_SECRET = "test-client-secret";

  private static final String APP_SLUG = "test-app-slug";

  private static final Integer APP_ID = 123456;

  private static final Long INSTALLATION_ID = 98765L;

  private static final String OAUTH_CODE = "test-oauth-code";

  private static final String ACCESS_TOKEN = "ghu_test_access_token";

  private static final String MANIFEST_CODE = "test-manifest-code";

  private static final String PRIVATE_KEY_PEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
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

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  private PasswordHandler passwordHandler;

  private Organization organization;

  private GitHubApp gitHubApp;

  @Before
  public void setUp() throws Exception {
    passwordHandler = lookup(PasswordHandler.class);
    setupGitHubMocks();

    organization = tempEntity.newOrganization("test-org");

    gitHubApp = new GitHubApp();
    gitHubApp.setAppId(APP_ID);
    gitHubApp.setSlug(APP_SLUG);
    gitHubApp.setClientId(CLIENT_ID);
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(CLIENT_SECRET));
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setOwnerId(organization.getId());
    gitHubApp.setInstallationId(INSTALLATION_ID);
    gitHubApp.setGithubOrganizationName("test-org");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp = tempEntity.newGitHubApp(gitHubApp);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.GITHUB_APP_RESOURCE_PATH);
  }

  private void setupGitHubMocks() {
    // Mock successful OAuth token exchange
    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"bearer\",\"scope\":\"\"}")
            )
    );

    // Mock user endpoint
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"login\":\"testuser\",\"id\":12345,\"type\":\"User\"}")
            )
    );

    // Mock user installations API
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":1,\"installations\":[{\"id\":" + INSTALLATION_ID
                    + ",\"app_id\":" + APP_ID + "}]}")
            )
    );
  }

  @Test
  public void testHandleInstallationSetup_Success() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-success", gitHubApp.getId(), futureDate);

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-success")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_InvalidStateToken() throws Exception {
    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "invalid-state-token")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid or expired state parameter");
  }

  @Test
  public void testHandleInstallationSetup_ExpiredStateToken() throws Exception {
    Date pastDate = new Date(System.currentTimeMillis() - 60000);
    createInstallationState("expired-state", gitHubApp.getId(), pastDate);

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "expired-state")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid or expired state parameter");
  }

  @Test
  public void testHandleInstallationSetup_UserDoesNotOwnInstallation() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-wrong-install", gitHubApp.getId(), futureDate);

    Long differentInstallationId = 99999L;

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", differentInstallationId.toString())
        .query("state", "valid-state-wrong-install")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_GitHubOAuthFailure_BadRequest() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-github-400", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"bad_verification_code\",\"error_description\":\"The code is invalid\"}")
            )
    );

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-github-400")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_GitHubOAuthFailure_Unauthorized() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-github-401", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/login/oauth/access_token"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"unauthorized_client\",\"error_description\":\"Invalid client credentials\"}")
            )
    );

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-github-401")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_GitHubInstallationsFailure_ServerError() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-github-500", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Internal server error\"}")
            )
    );

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-github-500")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_MultipleInstallations_VerifyCorrectOne() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-multi-verify", gitHubApp.getId(), futureDate);

    Long otherInstallId = 66666L;
    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":2,\"installations\":["
                    + "{\"id\":" + otherInstallId + ",\"app_id\":999},"
                    + "{\"id\":" + INSTALLATION_ID + ",\"app_id\":" + APP_ID + "}]}")
            )
    );

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-multi-verify")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleInstallationSetup_EmptyInstallationsList_ShouldFail() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    createInstallationState("valid-state-empty-list", gitHubApp.getId(), futureDate);

    githubMockServer.stubFor(
        get(urlPathEqualTo("/user/installations"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"total_count\":0,\"installations\":[]}")
            )
    );

    HttpResponse response = restRequest()
        .path("setupInstallation")
        .query("installation_id", INSTALLATION_ID.toString())
        .query("state", "valid-state-empty-list")
        .query("code", OAUTH_CODE)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleRedirect_Success() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    String stateToken = "valid-redirect-state";
    createRegistrationState(stateToken, organization.getId(), futureDate);

    mockManifestConversionSuccess();

    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", stateToken)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleRedirect_InvalidStateToken() throws Exception {
    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", "invalid-state-token")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid or expired state parameter");
  }

  @Test
  public void testHandleRedirect_ExpiredStateToken() throws Exception {
    Date pastDate = new Date(System.currentTimeMillis() - 60000);
    String expiredState = "expired-redirect-state";
    createRegistrationState(expiredState, organization.getId(), pastDate);

    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", expiredState)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid or expired state parameter");
  }

  @Test
  public void testHandleRedirect_MissingCode() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    String stateToken = "valid-state-no-code";
    createRegistrationState(stateToken, organization.getId(), futureDate);

    HttpResponse response = restRequest()
        .path("redirect")
        .query("state", stateToken)
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testHandleRedirect_MissingState() throws Exception {
    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testHandleRedirect_GitHubManifestConversionFailure_BadRequest() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    String stateToken = "valid-state-github-manifest-400";
    createRegistrationState(stateToken, organization.getId(), futureDate);

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + MANIFEST_CODE + "/conversions"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Not Found\"}")
            )
    );

    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", stateToken)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleRedirect_GitHubManifestConversionFailure_ServerError() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    String stateToken = "valid-state-github-manifest-500";
    createRegistrationState(stateToken, organization.getId(), futureDate);

    // Mock GitHub manifest conversion failure with 500
    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + MANIFEST_CODE + "/conversions"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Internal Server Error\"}")
            )
    );

    HttpResponse response = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", stateToken)
        .get();

    assertResponseStatus(500, response);
  }

  @Test
  public void testHandleRedirect_StateTokenConsumedOnce() throws Exception {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    String stateToken = "valid-state-single-use";
    createRegistrationState(stateToken, organization.getId(), futureDate);

    mockManifestConversionSuccess();

    HttpResponse response1 = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", stateToken)
        .get();

    assertResponseStatus(500, response1);

    HttpResponse response2 = restRequest()
        .path("redirect")
        .query("code", MANIFEST_CODE)
        .query("state", stateToken)
        .get();

    assertResponseStatus(400, response2);
    assertThat(response2.getBodyText()).contains("Invalid or expired state parameter");
  }

  private GitHubAppInstallationState createInstallationState(String stateValue, String githubAppId, Date expiresAt) {
    return tempEntity.newGitHubAppInstallationState(stateValue, githubAppId, generateCodeVerifier(), expiresAt);
  }

  private GitHubAppRegistrationState createRegistrationState(String stateToken, String ownerId, Date expiresAt) {
    return tempEntity.newGitHubAppRegistrationState(stateToken, ownerId, expiresAt);
  }

  private void mockManifestConversionSuccess() {
    String responseJson = String.format(
        "{\"id\":%d,\"slug\":\"%s\",\"client_id\":\"%s\",\"client_secret\":\"%s\",\"pem\":\"%s\",\"owner\":" +
                "{\"login\":\"test-owner\",\"id\":12345}}",
        APP_ID, APP_SLUG, CLIENT_ID, CLIENT_SECRET, PRIVATE_KEY_PEM.replace("\n", "\\n")
    );

    githubMockServer.stubFor(
        post(urlPathEqualTo("/app-manifests/" + MANIFEST_CODE + "/conversions"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)
            )
    );
  }

  private String generateCodeVerifier() {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    StringBuilder sb = new StringBuilder(128);
    for (int i = 0; i < 128; i++) {
      sb.append(alphabet.charAt((int) (Math.random() * alphabet.length())));
    }
    return sb.toString();
  }
}
