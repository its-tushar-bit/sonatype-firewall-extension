/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.ZScalerMockServerRule;
import com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationDTO;
import com.sonatype.insight.brain.zscaler.ZScalerCategory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.matching.NegativeRegexPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.sonatype.insight.brain.api.PublicApiPaths.ZSCALER_CONFIG_RESOURCE_PATH_V2;
import static com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.EULA_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ApiZScalerConfigurationResourceTest
    extends AbstractResourceTest
{
  @Rule
  public ZScalerMockServerRule zScalerMockServer = new ZScalerMockServerRule();

  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  private ZscalerFormatDAO zscalerFormatDAO;

  private PasswordHandler passwordHandler;

  @Before
  public void setup() throws Exception {
    zScalerConfigurationDAO = lookup(ZScalerConfigurationDAO.class);
    zscalerFormatDAO = lookup(ZscalerFormatDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
    restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH).path(SystemConfigurationProperty.ZSCALER).post();
  }

  @After
  public void tearDown() throws Exception {
    restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH).path(SystemConfigurationProperty.ZSCALER).delete();
  }

  @Test
  public void testGetZScalerConfiguration() throws Exception {
    ZScalerConfiguration config = tempEntity.newZScalerConfiguration("user", "password",
        "https://api.zscaler.net", "apikey123456", true, true, true, true);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).get();

    assertResponseStatus(200, response);
    ApiZScalerConfigurationDTO configDTO = response.getBody(ApiZScalerConfigurationDTO.class);
    assertThat(configDTO).isNotNull();
    assertThat(configDTO.getUsername()).isEqualTo(config.getUsername());
    assertThat(configDTO.getPassword()).isNull();
    assertThat(configDTO.getHostname()).isEqualTo(config.getHostname());
    assertThat(configDTO.getApiKey()).isEqualTo(config.getApikey());
    assertThat(configDTO.isMavenFormatEnabled()).isTrue();
    assertThat(configDTO.isNpmFormatEnabled()).isTrue();
    assertThat(configDTO.isPypiFormatEnabled()).isTrue();
    assertThat(configDTO.isNugetFormatEnabled()).isTrue();
    assertThat(configDTO.isEulaAgreed()).isEqualTo(true);
  }

  @Test
  public void testGetZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Zscaler not configured.");
  }

  @Test
  public void testSetZScalerConfiguration() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("testusername");
    request.setPassword("testpassword");
    request.setHostname("https://api.zscaler.net");
    request.setApiKey("testapikey12");
    request.setMavenFormatEnabled(true);
    request.setEulaAgreed(true);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(request).put();

    assertResponseStatus(200, response);

    assertEquals(String.format("You have acknowledged and agreed that %s", EULA_MESSAGE), response.getBodyText());

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNotNull();
    assertThat(config.getUsername()).isEqualTo(request.getUsername());
    assertThat(passwordHandler.decryptPassword(config.getPassword())).isEqualTo(request.getPassword());
    assertThat(config.getHostname()).isEqualTo(request.getHostname());
    assertThat(config.getApikey()).isEqualTo(request.getApiKey());

    List<ZscalerFormat> zscalerFormats = zscalerFormatDAO.getAll();
    assertThat(zscalerFormats).hasSize(4);
    for (ZscalerFormat format : zscalerFormats) {
      switch (format.getFormat()) {
        case "maven" -> assertThat(format.isEnabled()).isTrue();
        case "npm", "nuget", "pypi" -> assertThat(format.isEnabled()).isFalse();
        default -> throw new AssertionError("Unexpected format: " + format.getFormat());
      }
    }
  }

  @Test
  public void testSetZScalerConfiguration_nullConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(null).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Configuration is required.");
  }

  @Test
  public void testSetZScalerConfiguration_eulaNotAgreed() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("testusername");
    request.setPassword("testpassword");
    request.setHostname("https://api.zscaler.net");
    request.setApiKey("testapikey12");
    request.setMavenFormatEnabled(true);
    request.setEulaAgreed(false);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(request).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo(String.format("You must acknowledge and agree that %s", EULA_MESSAGE));
  }

  @Test
  public void testSetZScalerConfiguration_formatNotEnabled() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("testusername");
    request.setPassword("testpassword");
    request.setHostname("https://api.zscaler.net");
    request.setApiKey("testapikey12");
    request.setEulaAgreed(true);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(request).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("At least one format must be enabled.");
  }

  @Test
  public void testTestZScalerConfiguration() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("username");
    request.setPassword(passwordHandler.encryptPassword("password"));
    request.setHostname(zScalerMockServer.getBaseUrl());
    request.setApiKey("cajgffdcgkej");

    // Mock authentication and functional permission test operations
    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    // Mock create test category (for permission test)
    zScalerMockServer.mockCreateCustomUrlCategory(200,
        "{\"id\":\"test-category-id\",\"configuredName\":\"sonatype-permission-test-123\"," +
            "\"urls\":[\"permission-test-1-123.sonatype-validation.invalid\"]}");
    // Mock update test category (for OVERRIDE_EXISTING_CAT permission test)
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    // Mock delete test category (cleanup)
    zScalerMockServer.mockDeleteCustomUrlCategory(204, "");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/testConfig").body(request).post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testTestZScalerConfiguration_NoPermissions() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("username");
    request.setPassword(passwordHandler.encryptPassword("password"));
    request.setHostname(zScalerMockServer.getBaseUrl());
    request.setApiKey("cajgffdcgkej");

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    // Override the create category mock to return 403 (permission denied)
    zScalerMockServer.mockCreateCustomUrlCategory(403, "{\"error\":\"Forbidden - insufficient permissions\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/testConfig").body(request).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .contains("Insufficient ZScaler permissions")
        .contains("CUSTOM_URL_CAT");
  }

  @Test
  public void testTestZScalerConfiguration_InvalidApiKeyLength() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("username");
    request.setPassword("password");
    request.setHostname("https://api.zscaler.net");
    request.setApiKey("short");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/testConfig").body(request).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The apiKey must be exactly 12 characters.");
  }

  @Test
  public void testTestZScalerConfiguration_ApiKeyTooLong() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("username");
    request.setPassword("password");
    request.setHostname("https://api.zscaler.net");
    request.setApiKey("toolongapikey123");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/testConfig").body(request).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The apiKey must be exactly 12 characters.");
  }

  @Test
  public void testDeleteZScalerConfiguration() throws Exception {
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "apikey123456", true, true, true,
        true);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).delete();

    assertResponseStatus(204, response);

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNull();

    List<ZscalerFormat> zscalerFormats = zscalerFormatDAO.getAll();
    assertThat(zscalerFormats).isEmpty();
  }

  @Test
  public void testDeleteZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Zscaler not configured.");
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration() throws Exception {
    String username = "username";
    String password = passwordHandler.encryptPassword("password");
    String apiKey = "cajgffdcgkej";

    ZScalerCategory mavenCategory = new ZScalerCategory();
    mavenCategory.setId("maven-category");
    mavenCategory.setConfiguredName("sonatype-maven-shadow-download-defense");
    mavenCategory.setCustomCategory(true);
    mavenCategory.setUrls(List.of("https://repo1.maven.org/maven2/"));

    tempEntity.newZScalerConfiguration(username, password, zScalerMockServer.getBaseUrl(), apiKey, true,
        false, false, false);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetQuota(200, "{\"uniqueUrlsProvisioned\":\"1000\", \"remainingUrlsQuota\":\"10\"}");
    // Functional permission test no longer runs during update operations
    zScalerMockServer.mockGetCustomUrlCategories(200,
        new ObjectMapper().writeValueAsString(List.of(mavenCategory)));
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    zScalerMockServer.mockActivateChanges(200, "{\"status\":\"success\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(204, response);

    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/authenticatedSession")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/urlQuota")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withQueryParam("customOnly", equalTo("true")));
    zScalerMockServer.getWireMockServer().verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/.*")));
    zScalerMockServer.getWireMockServer().verify(postRequestedFor(urlPathMatching("/api/v1/status/activate")));
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_noConfiguration() throws Exception {
    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No zScaler configuration found");
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_authenticationException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"),
        zScalerMockServer.getBaseUrl(), "cajgffdcgkej", true,
        false, false, false);

    zScalerMockServer.mockAuthentication(401, "{\"error\":\"Invalid credentials\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Authentication failed: {\"error\":\"Invalid credentials\"}");
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_getCustomUrlCategoriesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"),
        zScalerMockServer.getBaseUrl(), "cajgffdcgkej", true,
        false, false, false);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(204, response);
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_updateCustomUrlCategoriesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"),
        zScalerMockServer.getBaseUrl(), "cajgffdcgkej", true,
        false, false, false);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(204, response);
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_createCustomUrlCategoryException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"),
        zScalerMockServer.getBaseUrl(), "cajgffdcgkej", true,
        false, false, false);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(204, response);
  }

  @Test
  public void testUpdateUrlsZScalerConfiguration_activateChangesException() throws Exception {
    tempEntity.newZScalerConfiguration(
        "username", passwordHandler.encryptPassword("password"),
        zScalerMockServer.getBaseUrl(), "cajgffdcgkej", true,
        false, false, false);

    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    // Functional permission test no longer runs during update operations
    zScalerMockServer.mockGetCustomUrlCategories(200, new ObjectMapper().writeValueAsString(List.of()));
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    zScalerMockServer.mockCreateCustomUrlCategory(200,
        "{\"id\":\"created-category-id\",\"configuredName\":\"sonatype-maven-shadow-download-defense\",\"urls\":[]}");
    zScalerMockServer.mockActivateChanges(500, "{\"error\":\"Activation failed\"}");

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").post();

    assertResponseStatus(204, response);
  }

  @Test
  public void testUpdateAll() throws Exception {
    setupExpectedCalls();

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update").post();

    assertResponseStatus(204, response);

    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/authenticatedSession")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/urlQuota")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withQueryParam("customOnly", equalTo("true")));

    // Account for:
    // - 3 DELETE POSTs with real URLs (MAVEN, NPM, PYPI)
    // - 4 UPDATE POSTs with real URLs from malicious URL fetcher (MAVEN, NPM, PYPI, NUGET)
    // - Total: 7 POSTs without "placeholder"
    // Note: Functional permission test no longer runs during update operations
    zScalerMockServer.getWireMockServer()
        .verify(7, postRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withRequestBody(new NegativeRegexPattern(".*placeholder.*")));
    // Account for delete of NUGET format only (uses placeholder)
    zScalerMockServer.getWireMockServer()
        .verify(1, postRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withRequestBody(new RegexPattern(".*placeholder.*")));
    zScalerMockServer.getWireMockServer().verify(postRequestedFor(urlPathMatching("/api/v1/status/activate")));
  }

  @Test
  public void testDelete() throws Exception {
    setupExpectedCalls();

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update/MAVEN").delete();

    assertResponseStatus(204, response);
    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/authenticatedSession")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/urlQuota")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withQueryParam("customOnly", equalTo("true")));

    // Account for delete of format
    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withRequestBody(new NegativeRegexPattern(".*placeholder.*")));

    zScalerMockServer.getWireMockServer().verify(postRequestedFor(urlPathMatching("/api/v1/status/activate")));
  }

  @Test
  public void testDeleteAll() throws Exception {
    setupExpectedCalls();

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2 + "/update").delete();

    assertResponseStatus(204, response);
    zScalerMockServer.getWireMockServer()
        .verify(postRequestedFor(urlPathMatching("/api/v1/authenticatedSession")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories/urlQuota")));
    zScalerMockServer.getWireMockServer()
        .verify(getRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withQueryParam("customOnly", equalTo("true")));

    // Account for:
    // - 3 POSTs from delete operation (delete of MAVEN/NPM/PYPI formats with real URLs)
    // Note: Functional permission test no longer runs during delete operations
    zScalerMockServer.getWireMockServer()
        .verify(3, postRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withRequestBody(new NegativeRegexPattern(".*placeholder.*")));
    // Account for delete of NUGET format (uses placeholder)
    zScalerMockServer.getWireMockServer()
        .verify(1, postRequestedFor(urlPathMatching("/api/v1/urlCategories"))
            .withRequestBody(new RegexPattern(".*placeholder.*")));

    zScalerMockServer.getWireMockServer().verify(postRequestedFor(urlPathMatching("/api/v1/status/activate")));
  }

  private void setupExpectedCalls() {
    String username = "username";
    String password = passwordHandler.encryptPassword("password");
    String apiKey = "cajgffdcgkej";
    tempEntity.newZScalerConfiguration(username, password, zScalerMockServer.getBaseUrl(), apiKey, true, true, true,
        true);
    zScalerMockServer.mockAuthentication(200, "{\"token\":\"mock-token\"}");
    zScalerMockServer.mockGetQuota(200, "{\"uniqueUrlsProvisioned\":\"1000\", \"remainingUrlsQuota\":\"10\"}");
    // Don't override getCustomUrlCategories - let the functional test mock handle it
    zScalerMockServer.mockUpdateCustomUrlCategories(200, "{\"status\":\"success\"}");
    zScalerMockServer.mockCreateCustomUrlCategory(200,
        "{\"id\":\"created-category-id\",\"configuredName\":\"sonatype-maven-shadow-download-defense\",\"urls\":[]}");
    zScalerMockServer.mockActivateChanges(200, "{\"status\":\"success\"}");
  }

  @Test
  public void testSetZScalerConfiguration_invalidProtocol_returnsHttpBadRequest() throws Exception {
    ApiZScalerConfigurationDTO request = new ApiZScalerConfigurationDTO();
    request.setUsername("testusername");
    request.setPassword("testpassword");
    request.setHostname("ftp://invalid-protocol.com");
    request.setApiKey("testapikey12");
    request.setMavenFormatEnabled(true);
    request.setEulaAgreed(true);

    HttpResponse response = restRequest().path(ZSCALER_CONFIG_RESOURCE_PATH_V2).body(request).put();

    // Test that validation exception becomes HTTP 400 Bad Request
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).isEqualTo("Protocol must be http or https");
  }
}
