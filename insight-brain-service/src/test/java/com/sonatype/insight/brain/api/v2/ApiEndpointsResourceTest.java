/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.api.v2.service.ApiEndpointsService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiEndpointsResourceTest
    extends AbstractResourceTest
{
  @Before
  public void before() {
    ApiEndpointsService.clearCaches();
  }

  @After
  public void after() {
    ApiEndpointsService.clearCaches();
  }

  @Test
  public void testGetOpenAPI_Public() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.ENDPOINTS_RESOURCE_PATH + "/" + ApiEndpointsResource.ENDPOINT_TYPE_RESOURCE_PATH)
        .parameter(ApiType.PUBLIC)
        .get();

    assertResponseStatus(200, response);
    OpenAPI openAPI = Json.mapper().readValue(response.getBodyText(), OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertEndpoint(openAPI, "/api/v2/applications", "Applications", HttpMethod.GET, HttpMethod.POST);
  }

  @Test
  public void testGetOpenAPI_Experimental() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.ENDPOINTS_RESOURCE_PATH + "/" + ApiEndpointsResource.ENDPOINT_TYPE_RESOURCE_PATH)
        .parameter(ApiType.EXPERIMENTAL)
        .get();

    assertResponseStatus(200, response);
    OpenAPI openAPI = Json.mapper().readValue(response.getBodyText(), OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertEndpoint(openAPI,
        "/api/experimental/signatures/vulnerability/applications/{applicationId}/reports/{reportId}",
        "Signatures", HttpMethod.POST);
  }

  @Test
  public void testGetOpenAPI_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(false);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.ENDPOINTS_RESOURCE_PATH + "/" + ApiEndpointsResource.ENDPOINT_TYPE_RESOURCE_PATH)
        .parameter(ApiType.PUBLIC)
        .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Feature not supported.");
  }

  private void assertEndpoint(
      OpenAPI openAPI,
      String expectedEndpoint,
      String expectedTag,
      HttpMethod... expectedOperations)
  {
    PathItem pathItem = openAPI.getPaths().get(expectedEndpoint);
    assertThat(pathItem).isNotNull();
    Map<HttpMethod, Operation> operationsMap = pathItem.readOperationsMap();
    assertThat(operationsMap).containsOnlyKeys(expectedOperations);
    assertThat(operationsMap.values()).allSatisfy(
        operation -> assertThat(operation.getTags()).containsExactly(expectedTag));
  }
}
