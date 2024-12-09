/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
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
  private static final String MOCK_VERSION = "1.165.0-01";

  @Inject
  private ApiEndpointsService apiEndpointsService;

  @Mock
  private Application mockApplication;

  @Mock
  private VersionService mockVersionService;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(VersionService.class).toInstance(mockVersionService);
    super.configure(binder);
  }

  @Before
  public void before() {
    ApiEndpointsService.clearCaches();
    when(mockVersionService.getVersion()).thenReturn(MOCK_VERSION);
  }

  @After
  public void after() {
    ApiEndpointsService.clearCaches();
  }

  @Test
  public void testGetOpenAPI_PublicClass() throws Exception {
    setupApplicationClasses();
    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    assertEndpoints(result, ApiType.PUBLIC, "/api/v2/ApiEndpointsServiceTestPublicResource",
        "Api Endpoints Service Test Public Resource");
    assertThat(ApiEndpointsService.getOpenApiJsonCacheCopy()).containsOnlyKeys(ApiType.PUBLIC);
  }

  @Test
  public void testGetOpenAPI_ExperimentalClass() throws Exception {
    setupApplicationClasses();
    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.EXPERIMENTAL);

    assertEndpoints(result, ApiType.EXPERIMENTAL, "/api/experimental/ApiEndpointsServiceTestExperimentalResource",
        "Api Endpoints Service Test Experimental Resource");
    assertThat(ApiEndpointsService.getOpenApiJsonCacheCopy()).containsOnlyKeys(ApiType.EXPERIMENTAL);
  }

  @Test
  public void testGetOpenAPI_CustomTag() throws Exception {
    when(mockApplication.getClasses()).thenReturn(Set.of(ApiEndpointsServiceTestResourceWithCustomTag.class));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getTags()).hasSize(1);
    assertThat(openAPI.getPaths()).hasSize(1);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceWithCustomTag", "Custom Tag", HttpMethod.GET,
        HttpMethod.DELETE);
    assertThat(ApiEndpointsService.getOpenApiJsonCacheCopy()).containsOnlyKeys(ApiType.PUBLIC);
  }

  @Test
  public void testGetOpenAPI_TagOrdering() throws Exception {
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceA.class,
        ApiEndpointsServiceTestResourceB.class,
        ApiEndpointsServiceTestResourceC.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getTags()).extracting(io.swagger.v3.oas.models.tags.Tag::getName)
        .containsExactly("A tag", "Api Endpoints Service Test Resource A", "Api Endpoints Service Test Resource C");
  }

  @Test
  public void testGetOpenAPI_DoesNotAddDuplicateTags() throws Exception {
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceA.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getTags()).extracting(io.swagger.v3.oas.models.tags.Tag::getName)
        .containsExactly("Api Endpoints Service Test Resource A");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetOpenAPI_EnumValuesAreRestricted() throws Exception {
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceEnumParameter.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(2);
    PathItem pathItemA = openAPI.getPaths().get("/api/v2/ApiEndpointsServiceTestResourceEnumParameter/A/{ownerType}");
    assertThat(pathItemA.getGet().getParameters().get(0).getSchema().getEnum()).containsExactlyInAnyOrderElementsOf(
        Arrays.stream(OwnerType.values()).map(OwnerType::toString).toList());
    PathItem pathItemB = openAPI.getPaths().get("/api/v2/ApiEndpointsServiceTestResourceEnumParameter/B/{ownerType}");
    assertThat(pathItemB.getGet().getParameters().get(0).getSchema().getEnum()).containsExactlyInAnyOrder("application",
        "organization");
  }

  @Test
  public void testGetOpenAPI_HasNoProductLicenseFeatures() throws Exception {
    testProductLicense.setFeatures();
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceLicensedFeatureA.class,
        ApiEndpointsServiceTestResourceLicensedFeatureB.class,
        ApiEndpointsServiceTestResourceLicensedFeatureC.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(5);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/A",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/B",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/A",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/B",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/A",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasClassProductLicenseFeature() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceLicensedFeatureA.class,
        ApiEndpointsServiceTestResourceLicensedFeatureB.class,
        ApiEndpointsServiceTestResourceLicensedFeatureC.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(6);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/A",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/B",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/A",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/B",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/A",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/B",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasMethodProductLicenseFeature() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.SBOM_REPORTS);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceLicensedFeatureA.class,
        ApiEndpointsServiceTestResourceLicensedFeatureB.class,
        ApiEndpointsServiceTestResourceLicensedFeatureC.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(8);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/A",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/B",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/C",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/A",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/B",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/C",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/A",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/C",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasClassAndMethodProductLicenseFeatures() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SBOM_REPORTS);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceLicensedFeatureA.class,
        ApiEndpointsServiceTestResourceLicensedFeatureB.class,
        ApiEndpointsServiceTestResourceLicensedFeatureC.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(9);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/A",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/B",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA/C",
        "Api Endpoints Service Test Resource Licensed Feature A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/A",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/B",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB/C",
        "Api Endpoints Service Test Resource Licensed Feature B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/A",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/B",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC/C",
        "Api Endpoints Service Test Resource Licensed Feature C", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasNoFeatureFlags() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(false);
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceFeatureFlagA.class,
        ApiEndpointsServiceTestResourceFeatureFlagB.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(1);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/A",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasClassFeatureFlag() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(true);
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceFeatureFlagA.class,
        ApiEndpointsServiceTestResourceFeatureFlagB.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(2);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/A",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagB/A",
        "Api Endpoints Service Test Resource Feature Flag B", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasMethodFeatureFlag() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(false);
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceFeatureFlagA.class,
        ApiEndpointsServiceTestResourceFeatureFlagB.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(3);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/A",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/B",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagB/B",
        "Api Endpoints Service Test Resource Feature Flag B", HttpMethod.GET);
  }

  @Test
  public void testGetOpenAPI_HasClassAndMethodFeatureFlags() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_MANAGER.setEnabled(true);
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestResourceFeatureFlagA.class,
        ApiEndpointsServiceTestResourceFeatureFlagB.class
    ));

    String result = apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);

    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getPaths()).hasSize(4);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/A",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagA/B",
        "Api Endpoints Service Test Resource Feature Flag A", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagB/A",
        "Api Endpoints Service Test Resource Feature Flag B", HttpMethod.GET);
    assertEndpoint(openAPI, "/api/v2/ApiEndpointsServiceTestResourceFeatureFlagB/B",
        "Api Endpoints Service Test Resource Feature Flag B", HttpMethod.GET);
  }

  private void assertEndpoints(String result, ApiType expectedApiType, String expectedEndpoint, String expectedTag)
      throws Exception
  {
    assertThat(result).isNotBlank();
    OpenAPI openAPI = Json.mapper().readValue(result, OpenAPI.class);
    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getInfo()).isNotNull();
    String expectedTitle = null;
    switch (expectedApiType) {
      case PUBLIC: {
        expectedTitle = "Sonatype Lifecycle Public REST API";
        break;
      }
      case EXPERIMENTAL: {
        expectedTitle = "Sonatype Lifecycle Experimental REST API";
        break;
      }
      default: {
        break;
      }
    }
    assertThat(openAPI.getInfo().getTitle()).isEqualTo(expectedTitle);
    assertThat(openAPI.getInfo().getVersion()).isEqualTo(MOCK_VERSION);
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
    when(mockApplication.getClasses()).thenReturn(Set.of(
        ApiEndpointsServiceTestInterface.class,
        ApiEndpointsServiceTestAbstractClass.class,
        ApiEndpointsServiceTestClass.class,
        ApiEndpointsServiceTestPublicResource.class,
        ApiEndpointsServiceTestExperimentalResource.class,
        ApiEndpointsServiceTestPrivateResource.class,
        ApiEndpointsServiceTestApiResource.class,
        ApiEndpointsServiceTestOtherResource.class
    ));
  }

  private interface ApiEndpointsServiceTestInterface
  {
  }

  private abstract static class ApiEndpointsServiceTestAbstractClass
  {
  }

  private static class ApiEndpointsServiceTestClass
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

  @Path("api/v2/ApiEndpointsServiceTestResourceA")
  @Tag(name = "Api Endpoints Service Test Resource A", description = "A description")
  private static class ApiEndpointsServiceTestResourceA
  {
    @GET
    public String get() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceB")
  @Tag(name = "A tag")
  private static class ApiEndpointsServiceTestResourceB
  {
    @GET
    public String get() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceC")
  private static class ApiEndpointsServiceTestResourceC
  {
    @GET
    public String get() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceEnumParameter")
  private static class ApiEndpointsServiceTestResourceEnumParameter
  {
    @GET
    @Path("A/{ownerType}")
    public OwnerType getA(@PathParam("ownerType") OwnerType ownerType) {
      return null;
    }

    @GET
    @Path("B/{ownerType:application|organization}")
    public String getB(@PathParam("ownerType") OwnerType ownerType) {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceLicensedFeatureA")
  @UnlicensedPath
  private static class ApiEndpointsServiceTestResourceLicensedFeatureA
  {
    @GET
    @Path("A")
    @UnlicensedPath
    public String getA() {
      return null;
    }

    @GET
    @Path("B")
    public String getB() {
      return null;
    }

    @GET
    @Path("C")
    @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
    public String getC() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceLicensedFeatureB")
  private static class ApiEndpointsServiceTestResourceLicensedFeatureB
  {
    @GET
    @Path("A")
    @UnlicensedPath
    public String getA() {
      return null;
    }

    @GET
    @Path("B")
    public String getB() {
      return null;
    }

    @GET
    @Path("C")
    @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
    public String getC() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceLicensedFeatureC")
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  private static class ApiEndpointsServiceTestResourceLicensedFeatureC
  {
    @GET
    @Path("A")
    @UnlicensedPath
    public String getA() {
      return null;
    }

    @GET
    @Path("B")
    public String getB() {
      return null;
    }

    @GET
    @Path("C")
    @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
    public String getC() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceFeatureFlagA")
  private static class ApiEndpointsServiceTestResourceFeatureFlagA
  {
    @GET
    @Path("A")
    public String getA() {
      return null;
    }

    @GET
    @Path("B")
    @HasFeature(SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING)
    public String getB() {
      return null;
    }
  }

  @Path("api/v2/ApiEndpointsServiceTestResourceFeatureFlagB")
  @HasFeature(SystemConfigurationPropertyFeature.SBOM_MANAGER)
  private static class ApiEndpointsServiceTestResourceFeatureFlagB
  {
    @GET
    @Path("A")
    public String getA() {
      return null;
    }

    @GET
    @Path("B")
    @HasFeature(SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING)
    public String getB() {
      return null;
    }
  }
}
