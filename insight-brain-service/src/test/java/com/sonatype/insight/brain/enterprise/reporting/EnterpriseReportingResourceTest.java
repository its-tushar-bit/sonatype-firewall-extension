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
import java.util.List;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokens;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokensResponse;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.enterprisereporting.IerDashboardGroupMetadataDTO;
import com.sonatype.insight.enterprisereporting.IerDashboardMetadataDTO;
import com.sonatype.insight.enterprisereporting.IerDashboardMetadataListDTO;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingFilterDAO;
import com.sonatype.insight.brain.dataaccess.enterprisereporting.EnterpriseReportingDefaultFilterDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingFilter;
import com.sonatype.insight.brain.model.enterprisereporting.EnterpriseReportingDefaultFilter;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnterpriseReportingResourceTest
    extends AbstractResourceTest
{
  private EnterpriseReportingFilterDAO enterpriseReportingFilterDAO;

  private EnterpriseReportingDefaultFilterDAO enterpriseReportingDefaultFilterDAO;

  private UserDAO userDAO;

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

  @Before
  public void setup() {
    enterpriseReportingFilterDAO = lookup(EnterpriseReportingFilterDAO.class);
    enterpriseReportingDefaultFilterDAO = lookup(EnterpriseReportingDefaultFilterDAO.class);
    userDAO = lookup(UserDAO.class);
  }

  @After
  public void cleanupFilters() {
    String userId = userDAO.getByUsernameNotNull(getUsername()).getId();
    for (EnterpriseReportingFilter f : enterpriseReportingFilterDAO.getFiltersByUserId(userId)) {
      enterpriseReportingFilterDAO.delete(f);
    }
    assertThat(enterpriseReportingFilterDAO.getFiltersByUserId(userId)).isEmpty();
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testGetDashboardMetadata_Success() throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataJsonList()).atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(new byte[0]).atUri("rest/enterpriseReporting/icons/rolling-recap.svg");
    HttpResponse response = restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    assertResponseStatus(200, response);
    IerDashboardMetadataListDTO responseList =
        response.getBody(IerDashboardMetadataListDTO.class);
    assertThat(responseList).isNotNull();
    assertThat(responseList.dashboardMetadata().size()).isEqualTo(1);
    assertThat(responseList.dashboardGroupMetadata().size()).isEqualTo(1);
    assertThat(responseList.version()).isNotNull();

    IerDashboardMetadataDTO dashboardMetadataDTO = responseList.dashboardMetadata().get(0);
    assertThat(dashboardMetadataDTO.dashboardId()).isEqualTo("sbom-scorecard");
    assertThat(dashboardMetadataDTO.title()).isEqualTo("Sbom Report Overview");
    assertThat(dashboardMetadataDTO.category()).isEqualTo("enterprise");
    assertThat(dashboardMetadataDTO.description()).isEqualTo("A comprehensive view of monthly sboms");
    assertThat(dashboardMetadataDTO.features().size()).isEqualTo(2);
    assertThat(dashboardMetadataDTO.accessButtonText()).isEqualTo("Open Dashboard");
    assertThat(dashboardMetadataDTO.previewImage()).isEqualTo("rolling-recap.svg");
    assertThat(dashboardMetadataDTO.previewImageIcon()).isEqualTo("faCalendar");
    assertThat(dashboardMetadataDTO.priority()).isEqualTo(1);
    assertThat(dashboardMetadataDTO.spotlight()).isTrue();

    IerDashboardGroupMetadataDTO dashboardGroupMetadataDTO = responseList.dashboardGroupMetadata().get(0);
    assertThat(dashboardGroupMetadataDTO.groupId()).isEqualTo("security");
    assertThat(dashboardGroupMetadataDTO.description()).isEqualTo("A group of security dashboards");
    assertThat(dashboardGroupMetadataDTO.features().size()).isEqualTo(2);
    assertThat(dashboardGroupMetadataDTO.previewImageIcon()).isEqualTo("faShield");
    assertThat(dashboardGroupMetadataDTO.spotlight()).isFalse();
    assertThat(dashboardGroupMetadataDTO.title()).isEqualTo("Security Risk");
  }

  @Test
  public void testGetDashboardMetadataAdditionalAttribute_Success() throws Exception {
    hdsMockServer.respondWith(createDashboardVersionJson()).atUri("rest/enterpriseReporting/currentVersion");
    hdsMockServer.respondWith(createDashboardMetadataAdditionalAttrJsonList())
        .atUri("rest/enterpriseReporting/dashboards");
    hdsMockServer.respondWith(new byte[0]).atUri("rest/enterpriseReporting/icons/rolling-recap.svg");
    HttpResponse response = restRequest().path(EnterpriseReportingResource.DASHBOARDS_METADATA_PATH).get();
    assertResponseStatus(200, response);
    IerDashboardMetadataListDTO responseList =
        response.getBody(IerDashboardMetadataListDTO.class);
    assertThat(responseList).isNotNull();
    assertThat(responseList.dashboardMetadata().size()).isEqualTo(1);
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
        restRequest().path(EnterpriseReportingResource.ACQUIRE_EMBED_SESSION)
            .query("dashboardId", "dashboardIdParam")
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
    SSOEmbedUrlRequest requestSentToHds = JsonUtils.parse(requestBody, new TypeReference<SSOEmbedUrlRequest>()
    {
    });
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
    return """
        {
          "version": {"version": 100},
          "dashboardMetadata": [
            {
              "dashboardId": "sbom-scorecard",
              "groupId": null,
              "title": "Sbom Report Overview",
              "description": "A comprehensive view of monthly sboms",
              "features": ["Graphs", "Tables"],
              "accessButtonText": "Open Dashboard",
              "previewImage": "rolling-recap.svg",
              "priority": 1,
              "spotlight": true,
              "dashboardPath": null,
              "spotlightColor": null,
              "sinceIQVersion": null,
              "spotlightText": null,
              "category": "enterprise",
              "previewImageIcon": "faCalendar"
            }
          ],
          "dashboardGroupMetadata": [
            {
              "groupId": "security",
              "description": "A group of security dashboards",
              "features": ["Trends", "Breakdowns"],
              "previewImageIcon": "faShield",
              "sinceIQVersion": null,
              "spotlight": false,
              "spotlightColor": null,
              "spotlightText": null,
              "title": "Security Risk"
            }
          ]
        }
        """;
  }

  private String createDashboardMetadataAdditionalAttrJsonList() {
    return """
        {
          "version": {"version": 101},
          "dashboardMetadata": [
            {
              "dashboardId": "sbom-scorecard",
              "groupId": null,
              "title": "Sbom Report Overview",
              "description": "A comprehensive view of monthly sboms",
              "features": ["Graphs", "Tables"],
              "accessButtonText": "Open Dashboard",
              "previewImage": "rolling-recap.svg",
              "priority": 1,
              "spotlight": true,
              "dashboardPath": null,
              "spotlightColor": null,
              "sinceIQVersion": null,
              "spotlightText": null,
              "category": "enterprise",
              "previewImageIcon": "faBrain",
              "unexpectedAttr": "what is this"
            }
          ],
          "dashboardGroupMetadata": []
        }
        """;
  }

  private byte[] getBytesFromIconsZip() throws IOException, URISyntaxException {
    return Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons_svg.zip")
        .toURI()));
  }

  private String createDashboardVersionJson() {
    return "{\"version\":1}";
  }

  @Test
  public void testGetFiltersForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter1 = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    var filter2 = createFilterAndInsert(userId, "Filter 2", "{\"k\":2}");

    HttpResponse response = restRequest().path(EnterpriseReportingResource.SAVED_FILTERS_PATH).get();
    assertResponseStatus(200, response);

    var filterList = response.getBody(EnterpriseReportingDashboardFilterDTO[].class);
    assertThat(filterList).isNotNull();
    assertThat(filterList.length).isEqualTo(2);
    assertFilterEquality(filterList[0], filter1, userId);
    assertFilterEquality(filterList[1], filter2, userId);
  }

  @Test
  public void testCreateFilterForCurrentUser() throws Exception {
    var dto = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", false);

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(dto)
        .post();

    assertResponseStatus(200, response);
    var result = response.getBody(EnterpriseReportingDashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.id).isNotNull();
    assertThat(result.name).isEqualTo(dto.name);
    assertThat(result.filter).isEqualTo(dto.filter);

    String userId = getUserId();
    List<EnterpriseReportingFilter> filterList = enterpriseReportingFilterDAO.getFiltersByUserId(userId);
    assertThat(filterList.size()).isEqualTo(1);
    EnterpriseReportingFilter stored = filterList.get(0);
    assertFilterEquality(result, stored, userId);
  }

  @Test
  public void testCreateFilterForCurrentUser__LargePayload() throws Exception {
    // Build a large JSON payload to validate serialization/storage handling
    String largeValue = "a".repeat(500 * 1024);
    String largeJson = "{\"data\":\"" + largeValue + "\"}";
    var dto = new EnterpriseReportingDashboardFilterDTO(null, "Large Filter", largeJson, false);

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(dto)
        .post();

    assertResponseStatus(200, response);
    var result = response.getBody(EnterpriseReportingDashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.id).isNotNull();
    assertThat(result.name).isEqualTo("Large Filter");
    assertThat(result.filter).isEqualTo(largeJson);

    String userId = getUserId();
    var persistedFilter = enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, result.id);
    assertFilterEquality(result, persistedFilter, userId);
  }

  @Test
  public void testCreateFilterForCurrentUser__SetDefault() throws Exception {
    var dto = new EnterpriseReportingDashboardFilterDTO(null, "Filter 1", "{\"k\":1}", true);

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(dto)
        .post();

    assertResponseStatus(200, response);
    var result = response.getBody(EnterpriseReportingDashboardFilterDTO.class);
    assertThat(result).isNotNull();

    String userId = getUserId();
    assertDefaultFilterForUser(userId, result.id);
  }

  @Test
  public void testCreateFilterForCurrentUser__NoName() throws Exception {
    var dto = new EnterpriseReportingDashboardFilterDTO(null, "", "{\"k\":1}", false);
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(dto)
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Filter name is required.");
  }

  @Test
  public void testUpdateFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");

    var updatedDTO = new EnterpriseReportingDashboardFilterDTO(filter.getId(), "Filter 1", "{\"k\":2}", false);
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(updatedDTO)
        .put();

    assertResponseStatus(200, response);
    var responseBody = response.getBody(EnterpriseReportingDashboardFilterDTO.class);
    assertThat(responseBody.id).isEqualTo(filter.getId());
    assertThat(responseBody.name).isEqualTo(filter.getFilterName());
    assertThat(responseBody.filter).isEqualTo(updatedDTO.filter);

    var persistedFilter = enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filter.getId());
    assertFilterEquality(responseBody, persistedFilter, userId);
  }

  @Test
  public void testUpdateFilterForCurrentUser__UpdatesDefault() throws Exception {
    String userId = getUserId();
    var filter1 = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    var filter2 = createFilterAndInsert(userId, "Filter 2", "{\"k\":2}");
    createDefaultFilterAndInsert(userId, filter1.getId());

    assertDefaultFilterForUser(userId, filter1.getId());

    var updatedDTO = new EnterpriseReportingDashboardFilterDTO(filter2.getId(), "Filter 2", "{\"k\":3}", true);
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(updatedDTO)
        .put();
    assertResponseStatus(200, response);

    assertDefaultFilterForUser(userId, filter2.getId());
  }

  @Test
  public void testUpdateFilterForCurrentUser__NoName() throws Exception {
    var dto = new EnterpriseReportingDashboardFilterDTO("1234", "", "{\"k\":1}", false);
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.SAVED_FILTERS_PATH)
        .body(dto)
        .put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Filter name is required.");
  }

  @Test
  public void testDeleteFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DELETE_FILTERS_PATH)
        .parameter(filter.getId())
        .delete();
    assertResponseStatus(204, response);
    assertThat(enterpriseReportingFilterDAO.getFilterByUserAndFilterId(userId, filter.getId())).isNull();
  }

  @Test
  public void testDeleteFilterForCurrentUser_InvalidId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DELETE_FILTERS_PATH)
        .parameter("no-matching-id")
        .delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find filter to delete. It may have already been removed or does not exist.");
  }

  // If there is no filterId value when the request is sent, "undefined" is appended to the URL in its place
  @Test
  public void testDeleteFilterForCurrentUser_UndefinedId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DELETE_FILTERS_PATH)
        .parameter("undefined")
        .delete();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Filter ID cannot be null.");
  }

  @Test
  public void testDeleteFilterForCurrentUser_NullId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DELETE_FILTERS_PATH)
        .parameter("null")
        .delete();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Filter ID cannot be null.");
  }

  @Test
  public void testGetDefaultFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    createDefaultFilterAndInsert(userId, filter.getId());

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DEFAULT_FILTER_PATH)
        .get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo(filter.getId());
  }

  @Test
  public void testGetDefaultFilterForCurrentUser__NoFilter() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DEFAULT_FILTER_PATH)
        .get();
    assertResponseStatus(204, response);
    assertThat(response.getBodyText()).isEqualTo("");
  }

  @Test
  public void testInsertDefaultFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");

    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.UPDATE_DEFAULT_FILTERS_PATH)
        .parameter(filter.getId())
        .put();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo(filter.getId());

    EnterpriseReportingDefaultFilter defaultFilter =
        enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);
    assertThat(defaultFilter).isNotNull();
    assertThat(defaultFilter.getFilterId()).isEqualTo(filter.getId());
  }

  @Test
  public void testInsertDefaultFilterForCurrentUser_InvalidId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.UPDATE_DEFAULT_FILTERS_PATH)
        .parameter("no-matching-id")
        .put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Filter does not already exist to mark as default.");
  }

  @Test
  public void testInsertDefaultFilterForCurrentUser__UndefinedId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.UPDATE_DEFAULT_FILTERS_PATH)
        .parameter("undefined")
        .put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Filter does not already exist to mark as default.");
  }

  @Test
  public void testInsertDefaultFilterForCurrentUser__NullId() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.UPDATE_DEFAULT_FILTERS_PATH)
        .parameter("null")
        .put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Filter does not already exist to mark as default.");
  }

  @Test
  public void testUpdateDefaultFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter1 = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    var filter2 = createFilterAndInsert(userId, "Filter 2", "{\"k\":2}");
    createDefaultFilterAndInsert(userId, filter1.getId());

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.UPDATE_DEFAULT_FILTERS_PATH)
        .parameter(filter2.getId())
        .put();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo(filter2.getId());

    assertDefaultFilterForUser(userId, filter2.getId());
  }

  @Test
  public void testDeleteFilterForCurrentUser__DeletesDefault() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    var defaultFilter = createDefaultFilterAndInsert(userId, filter.getId());

    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId).getFilterId())
        .isEqualTo(defaultFilter.getFilterId());

    // Delete the filter marked as default
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DELETE_FILTERS_PATH)
        .parameter(filter.getId())
        .delete();
    assertResponseStatus(204, response);
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testDeleteDefaultFilterForCurrentUser() throws Exception {
    String userId = getUserId();
    var filter = createFilterAndInsert(userId, "Filter 1", "{\"k\":1}");
    var defaultFilter = createDefaultFilterAndInsert(userId, filter.getId());

    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId).getFilterId())
        .isEqualTo(defaultFilter.getFilterId());

    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DEFAULT_FILTER_PATH)
        .delete();
    assertResponseStatus(204, response);
    assertThat(enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId)).isNull();
  }

  @Test
  public void testDeleteDefaultFilterForCurrentUser__NonExistentFilter() throws Exception {
    HttpResponse response = restRequest()
        .path(EnterpriseReportingResource.DEFAULT_FILTER_PATH)
        .delete();
    assertResponseStatus(204, response);
  }

  private EnterpriseReportingFilter createFilterAndInsert(String userId, String filterName, String filterJson) {
    var filter = new EnterpriseReportingFilter();
    filter.setId(IdUtil.newUUID());
    filter.setUserId(userId);
    filter.setFilterName(filterName);
    filter.setFilter(filterJson);
    enterpriseReportingFilterDAO.insert(filter);
    return filter;
  }

  private EnterpriseReportingDefaultFilter createDefaultFilterAndInsert(String userId, String filterId) {
    var filter = new EnterpriseReportingDefaultFilter();
    filter.setId(userId);
    filter.setFilterId(filterId);
    enterpriseReportingDefaultFilterDAO.insert(filter);
    return filter;
  }

  private String getUserId() {
    return userDAO.getByUsernameNotNull(getUsername()).getId();
  }

  private void assertFilterEquality(
      EnterpriseReportingDashboardFilterDTO response,
      EnterpriseReportingFilter persistedFilter,
      String userId)
  {
    assertThat(persistedFilter.getFilterName()).isEqualTo(response.name);
    assertThat(persistedFilter.getId()).isEqualTo(response.id);
    assertThat(persistedFilter.getFilter()).isEqualTo(response.filter);
    assertThat(persistedFilter.getUserId()).isEqualTo(userId);
  }

  private void assertDefaultFilterForUser(String userId, String expectedFilterId) {
    var defaultFilter = enterpriseReportingDefaultFilterDAO.getDefaultFilterByUserId(userId);
    assertThat(defaultFilter).isNotNull();
    assertThat(defaultFilter.getFilterId()).isEqualTo(expectedFilterId);
  }
}
