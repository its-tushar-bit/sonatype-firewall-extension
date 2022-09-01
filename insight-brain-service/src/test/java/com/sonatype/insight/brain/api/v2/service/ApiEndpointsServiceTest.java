/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApiEndpointsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiEndpointsService apiEndpointsService;

  @Mock
  private Application mockApplication;

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE.clear();
  }

  @After
  public void after() {
    ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE.clear();
  }

  @Test
  public void testGetOpenAPI_PublicClass() throws Exception {
    setupApplicationClasses();
    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    assertEndpoints(result, "/api/v2/ApiEndpointsServiceTestPublicResource",
        "Api Endpoints Service Test Public Resource");
    assertThat(ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE).containsOnlyKeys(ApiType.PUBLIC);
  }

  @Test
  public void testGetOpenAPI_ExperimentalClass() throws Exception {
    setupApplicationClasses();
    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.EXPERIMENTAL);

    assertEndpoints(result, "/api/experimental/ApiEndpointsServiceTestExperimentalResource",
        "Api Endpoints Service Test Experimental Resource");
    assertThat(ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE).containsOnlyKeys(ApiType.EXPERIMENTAL);
  }

  @Test
  public void testGetOpenAPI_CustomTag() throws Exception {
    when(mockApplication.getClasses()).thenReturn(new HashSet<>(
        Collections.singletonList(ApiEndpointsServiceTestResourceWithCustomTag.class)));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getTags()).hasSize(1);
    assertThat(openAPI.getPaths()).hasSize(1);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceWithCustomTag", "Custom Tag", HttpMethod.GET,
        HttpMethod.DELETE);
    assertThat(ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE).containsOnlyKeys(ApiType.PUBLIC);
  }

  private void assertEndpoints(String result, String expectedEndpoint, String expectedTag) throws Exception {
    assertThat(result).isNotBlank();
    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getTags()).hasSize(1);
    assertThat(openAPI.getPaths()).hasSize(2);
    assertEndpoint(openAPI, expectedEndpoint, expectedTag, HttpMethod.GET,
        HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);
    assertEndpoint(openAPI, expectedEndpoint + "/nested", expectedTag, HttpMethod.GET);
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

  private void setupApplicationClasses() {
    when(mockApplication.getClasses()).thenReturn(new HashSet<>(Arrays.asList(
        ApiEndpointsServiceTestInterface.class,
        ApiEndpointsServiceTestAbstractClass.class,
        ApiEndpointsServiceTestClass.class,
        ApiEndpointsServiceTestPublicResource.class,
        ApiEndpointsServiceTestExperimentalResource.class,
        ApiEndpointsServiceTestPrivateResource.class,
        ApiEndpointsServiceTestApiResource.class,
        ApiEndpointsServiceTestOtherResource.class
    )));
  }

  private interface ApiEndpointsServiceTestInterface
  {
  }

  private abstract class ApiEndpointsServiceTestAbstractClass
  {
  }

  private class ApiEndpointsServiceTestClass
  {
  }

  @Path("api/v2/ApiEndpointsServiceTestPublicResource")
  private static class ApiEndpointsServiceTestPublicResource
  {
    @GET
    public String get() {
      return null;
    }

    @GET
    @Path("nested")
    public String getNested() {
      return null;
    }

    @POST
    public void post() {
      // noop
    }

    @PUT
    public void put() {
      // noop
    }

    @DELETE
    public void delete() {
      // noop
    }
  }

  @Path("api/experimental/ApiEndpointsServiceTestExperimentalResource")
  private static class ApiEndpointsServiceTestExperimentalResource
  {
    @GET
    public String get() {
      return null;
    }

    @GET
    @Path("nested")
    public String getNested() {
      return null;
    }

    @POST
    public void post() {
      // noop
    }

    @PUT
    public void put() {
      // noop
    }

    @DELETE
    public void delete() {
      // noop
    }
  }

  @Path("rest/ApiEndpointsServiceTestPrivateResource")
  private static class ApiEndpointsServiceTestPrivateResource
  {
    @GET
    public String get() {
      return null;
    }

    @GET
    @Path("nested")
    public String getNested() {
      return null;
    }

    @POST
    public void post() {
      // noop
    }

    @PUT
    public void put() {
      // noop
    }

    @DELETE
    public void delete() {
      // noop
    }
  }

  @Path("api/ApiEndpointsServiceTestApiResource")
  private static class ApiEndpointsServiceTestApiResource
  {
    @GET
    public String get() {
      return null;
    }
  }

  @Path("other/ApiEndpointsServiceTestOtherResource")
  private static class ApiEndpointsServiceTestOtherResource
  {
    @GET
    public String get() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceWithCustomTag")
  @Tag(name = "Custom Tag")
  private static class ApiEndpointsServiceTestResourceWithCustomTag
  {
    @GET
    public String get() {
      return null;
    }

    @DELETE
    public void delete() {
      // noop
    }
  }
}
