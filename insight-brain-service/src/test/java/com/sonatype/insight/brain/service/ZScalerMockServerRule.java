/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.rules.ExternalResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ZScalerMockServerRule
    extends ExternalResource
{
  private WireMockServer zScalerMockServer;

  public static final String AUTHENTICATED_SESSION_PATH = "/api/v1/authenticatedSession";

  public static final String ADMIN_USERS_ME = "/api/v1/adminUsers/me";

  public static final String ADMIN_ROLES = "/api/v1/adminRoles";

  public static final String URL_CATEGORIES_PATH = "/api/v1/urlCategories";

  public static final String ACTIVATE_PATH = "/api/v1/status/activate";

  private final int port;

  public ZScalerMockServerRule() {
    this.port = 0; // Use dynamic port by default
  }

  @Override
  public void before() {
    zScalerMockServer = (port > 0)
        ? new WireMockServer(wireMockConfig().port(port))
        : new WireMockServer(wireMockConfig().dynamicPort());
    zScalerMockServer.start();
  }

  @Override
  public void after() {
    zScalerMockServer.stop();
  }

  public WireMockServer getWireMockServer() {
    return zScalerMockServer;
  }

  public String getBaseUrl() {
    return zScalerMockServer.baseUrl();
  }

  public int getPort() {
    return zScalerMockServer.port();
  }

  public void mockAuthentication(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(
        post(urlPathMatching(AUTHENTICATED_SESSION_PATH))
            .withHeader("Content-Type", equalTo("application/json")) // Match Content-Type header
            .withRequestBody(matchingJsonPath("$.username", matching(".*"))) // Match any username
            .withRequestBody(matchingJsonPath("$.password", matching(".*"))) // Match any password
            .withRequestBody(matchingJsonPath("$.apiKey", matching(".*"))) // Match any apiKey
            .withRequestBody(matchingJsonPath("$.timestamp", matching(".*"))) // Match any timestamp
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")
                .withHeader("Set-Cookie", "JSESSIONID=mock-session-id")
                .withBody(responseBody)));

    if (statusCode == 200) {
      mockGetCurrentAdminUser(200, "{\"role\":{\"id\":\"mock-role-id\"}}");
      mockGetAdminRoleDetails(200, "{\"featurePermissions\":" +
          "{\"OVERRIDE_EXISTING_CAT\":\"READ_WRITE\",\"CUSTOM_URL_CAT\":\"READ_WRITE\"}}");
    }
  }

  public void mockAuthenticationWithRolesAndPermissions(
      int statusCode, String responseBody,
      int adminUsersStatusCode, String adminUsersResponseBody,
      int adminRolesStatusCode, String adminRolesResponseBody)
  {
    zScalerMockServer.stubFor(
        post(urlPathMatching(AUTHENTICATED_SESSION_PATH))
            .withHeader("Content-Type", equalTo("application/json")) // Match Content-Type header
            .withRequestBody(matchingJsonPath("$.username", matching(".*"))) // Match any username
            .withRequestBody(matchingJsonPath("$.password", matching(".*"))) // Match any password
            .withRequestBody(matchingJsonPath("$.apiKey", matching(".*"))) // Match any apiKey
            .withRequestBody(matchingJsonPath("$.timestamp", matching(".*"))) // Match any timestamp
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")
                .withHeader("Set-Cookie", "JSESSIONID=mock-session-id")
                .withBody(responseBody)));

    if (statusCode == 200) {
      mockGetCurrentAdminUser(adminUsersStatusCode, adminUsersResponseBody);
      mockGetAdminRoleDetails(adminRolesStatusCode, adminRolesResponseBody);
    }
  }

  public void mockGetCurrentAdminUser(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(
        get(urlPathMatching(ADMIN_USERS_ME))
            .withHeader("Cookie", containing("JSESSIONID=mock-session-id"))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody))
    );
  }

  public void mockGetAdminRoleDetails(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(
        get(urlPathMatching(ADMIN_ROLES + "/mock-role-id"))
            .withHeader("Cookie", containing("JSESSIONID=mock-session-id"))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody))
    );
  }

  public void mockGetQuota(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(get(urlPathMatching(URL_CATEGORIES_PATH + "/urlQuota"))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)));
  }

  public void mockGetCustomUrlCategories(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(get(urlPathMatching(URL_CATEGORIES_PATH))
        .withQueryParam("customOnly", equalTo("true")) // Match the query parameter
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)));
  }

  public void mockCreateCustomUrlCategory(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(post(urlPathMatching(URL_CATEGORIES_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)));
  }

  public void mockUpdateCustomUrlCategories(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(put(urlPathMatching(URL_CATEGORIES_PATH + "/.*"))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)));
  }

  public void mockActivateChanges(int statusCode, String responseBody) {
    zScalerMockServer.stubFor(post(urlPathMatching(ACTIVATE_PATH))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)));
  }
}
