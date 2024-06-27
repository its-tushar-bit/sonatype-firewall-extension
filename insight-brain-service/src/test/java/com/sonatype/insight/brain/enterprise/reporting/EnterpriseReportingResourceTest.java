/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokens;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokensResponse;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.jaxrs.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnterpriseReportingResourceTest
    extends AbstractResourceTest
{
  @Before
  @After
  public void clearLookerConfigCache() {
    getCLMServer().getInstance(EnterpriseReportingService.class)
        .clearEnterpriseReportingConfigDTOBaseUrlSupplierForTests();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EnterpriseReportingResource.RESOURCE_PATH);
  }

  @Test
  public void testGetDashboardMetadata_Success() throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataJsonList()).atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(new byte[0]).atUri("rest/enterpriseReporting/icons/rolling-recap.svg");
    HttpResponse response = restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    assertResponseStatus(200, response);
    DashboardMetadataListDTO responseList =
        response.getBody(DashboardMetadataListDTO.class);
    assertThat(responseList).isNotNull();
    assertThat(responseList.dashboardMetadata.size()).isEqualTo(1);
    DashboardMetadataDTO dashboardMetadataDTO = responseList.dashboardMetadata.get(0);
    assertThat(dashboardMetadataDTO.dashboardId).isEqualTo("sbom-scorecard");
    assertThat(dashboardMetadataDTO.title).isEqualTo("Sbom Report Overview");
    assertThat(dashboardMetadataDTO.description).isEqualTo("A comprehensive view of monthly sboms");
    assertThat(dashboardMetadataDTO.features.size()).isEqualTo(2);
    assertThat(dashboardMetadataDTO.accessButtonText).isEqualTo("Open Dashboard");
    assertThat(dashboardMetadataDTO.previewImage).isEqualTo("rolling-recap.svg");
    assertThat(dashboardMetadataDTO.priority).isEqualTo(1);
    assertThat(dashboardMetadataDTO.spotlight).isTrue();
  }

  @Test
  public void testGetIcon_Success() throws Exception {
    assertTestGetIcon(200, "rolling-recap.svg");
  }

  @Test
  public void testGetIcon_404_NotFound() throws Exception {
    assertTestGetIcon(404, "fake-icon-name.svg");
  }

  @Test
  public void testGetIcon_400_BadRequest() throws Exception {
    assertTestGetIcon(400, "..\\fake-icon-name.svg");
  }

  @Test
  public void testAcquireEmbedSession_Success() throws Exception {
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);
    hdsMockServer.respondWith(expectedResponse).atUri("rest/enterpriseReporting/acquireEmbedSession");

    String encodedEmbedDomain = "http%3A%2F%2Flocalhost%3A8070";
    String embedDomain = "http://localhost:8070";
    HttpResponse response =
        restRequest().path(EnterpriseReportingResource.ACQUIRE_EMBED_SESSION).query("dashboardId", "dashboardIdParam")
            .query("embedDomain", encodedEmbedDomain)
            .get();
    assertResponseStatus(200, response);
    EmbedCookielessSessionAcquire embedSessionResponse =
        response.getBody(EmbedCookielessSessionAcquire.class);
    assertThat(embedSessionResponse).isNotNull();

    assertThat(embedSessionResponse.getAuthenticationToken()).isEqualTo("authTokenResponse");
    assertThat(embedSessionResponse.getAuthenticationTokenTtl()).isEqualTo(300);
    assertThat(embedSessionResponse.getNavigationToken()).isEqualTo("navTokenResponse");
    assertThat(embedSessionResponse.getNavigationTokenTtl()).isEqualTo(400);
    assertThat(embedSessionResponse.getApiToken()).isEqualTo("apiTokenResponse");
    assertThat(embedSessionResponse.getApiTokenTtl()).isEqualTo(500);
    assertThat(embedSessionResponse.getSessionReferenceToken()).isEqualTo("sessionTokenResponse");
    assertThat(embedSessionResponse.getSessionReferenceTokenTtl()).isEqualTo(600);

    // Verify that the domain sent to HDS was truncated to exclude the final separator
    String requestBody = hdsMockServer.getCapturedRequestBody("rest/enterpriseReporting/acquireEmbedSession");
    SSOEmbedUrlRequest requestSentToHds = JsonUtils.parse(requestBody, new TypeReference<SSOEmbedUrlRequest>() { });
    assertThat(requestSentToHds.embedDomain).isEqualTo(embedDomain);
  }

  @Test
  public void testGetBaseUrl_Success() throws Exception {
    EnterpriseReportingConfigDTO config = new EnterpriseReportingConfigDTO("https://looker.example.com");
    hdsMockServer.respondWith(config).atUri("rest/enterpriseReporting/config");

    HttpResponse response =
        restRequest().path(EnterpriseReportingResource.GET_BASE_URL)
            .get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isNotNull();
    assertThat(response.getBodyText()).isEqualTo(config.baseUrl);
  }

  @Test
  public void testAcquireEmbedSession_BadRequest_missingParameters() throws Exception {
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);
    hdsMockServer.respondWith(expectedResponse).atUri("rest/enterpriseReporting/acquireEmbedSession");

    HttpResponse response =
        restRequest().path(EnterpriseReportingResource.ACQUIRE_EMBED_SESSION).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Dashboard is null or empty");
  }

  @Test
  public void testGenerateEmbedTokens_Success() throws Exception {
    EmbedCookielessSessionGenerateTokensResponse expectedResponse =
        new EmbedCookielessSessionGenerateTokensResponse("navToken", 200, "apiToken", 300, "sessionRefTokenResponse",
            400);
    hdsMockServer.respondWith(expectedResponse).atUri("rest/enterpriseReporting/generateEmbedTokens");

    EmbedCookielessSessionGenerateTokens tokenRequestDto =
        new EmbedCookielessSessionGenerateTokens("navToken", "apiToken", "oldSessionToken");
    HttpResponse response =
        restRequest().path(EnterpriseReportingResource.GENERATE_EMBED_TOKENS).body(tokenRequestDto).put();
    assertResponseStatus(200, response);
    EmbedCookielessSessionGenerateTokensResponse embedSessionResponse =
        response.getBody(EmbedCookielessSessionGenerateTokensResponse.class);
    assertThat(embedSessionResponse).isNotNull();

    assertThat(embedSessionResponse.getNavigationToken()).isEqualTo("navToken");
    assertThat(embedSessionResponse.getNavigationTokenTtl()).isEqualTo(200);
    assertThat(embedSessionResponse.getApiToken()).isEqualTo("apiToken");
    assertThat(embedSessionResponse.getApiTokenTtl()).isEqualTo(300);
    assertThat(embedSessionResponse.getSessionReferenceToken()).isEqualTo("sessionRefTokenResponse");
    assertThat(embedSessionResponse.getSessionReferenceTokenTtl()).isEqualTo(400);
  }

  @Test
  public void testGenerateEmbedTokens_BadRequest_MissingParameters() throws Exception {
    EmbedCookielessSessionGenerateTokens tokenRequestDto =
        new EmbedCookielessSessionGenerateTokens(null, "apiToken", "oldSessionToken");
    HttpResponse response =
        restRequest().path(EnterpriseReportingResource.GENERATE_EMBED_TOKENS).body(tokenRequestDto).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Navigation token is null or empty");

    tokenRequestDto = new EmbedCookielessSessionGenerateTokens("navToken", null, "oldSessionToken");
    response = restRequest().path(EnterpriseReportingResource.GENERATE_EMBED_TOKENS).body(tokenRequestDto).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Api token is null or empty");

    tokenRequestDto = new EmbedCookielessSessionGenerateTokens("navToken", "apiToken", null);
    response = restRequest().path(EnterpriseReportingResource.GENERATE_EMBED_TOKENS).body(tokenRequestDto).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Session reference token is null or empty");
  }

  private void assertTestGetIcon(int expectedStatus, String iconName) throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataJsonList()).atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(getBytesFromIconsZip()).atUri("rest/enterpriseReporting/icons");
    restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.GET_IER_ICON_PATH)
        .parameter(iconName)
        .get();
    assertResponseStatus(expectedStatus, response);
  }

  private String createDashboardMetadataJsonList() {
    return "{\n" +
        "  \"dashboardMetadata\": [\n" +
        "    {\n" +
        "      \"dashboardId\": \"sbom-scorecard\",\n" +
        "      \"title\": \"Sbom Report Overview\",\n" +
        "      \"description\": \"A comprehensive view of monthly sboms\",\n" +
        "      \"features\": [\n" +
        "        \"Graphs\",\n" +
        "        \"Tables\"\n" +
        "      ],\n" +
        "      \"accessButtonText\": \"Open Dashboard\",\n" +
        "      \"previewImage\": \"rolling-recap.svg\",\n" +
        "      \"priority\": 1,\n" +
        "      \"spotlight\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}";
  }

  private byte[] getBytesFromIconsZip() throws IOException, URISyntaxException {
    return Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons_svg.zip").toURI()));
  }

  private String createDashboardVersionJson() {
    return "{\"version\":1}";
  }
}
