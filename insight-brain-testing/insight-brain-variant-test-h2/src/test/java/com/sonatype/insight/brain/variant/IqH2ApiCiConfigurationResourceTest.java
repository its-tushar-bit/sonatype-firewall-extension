/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ApiCiConfigurationResource REST endpoints.
 * Focuses on HTTP-level concerns: status codes, request validation, and serialization.
 * Business logic tests are in CiConfigurationServiceTest.
 *
 * @since 1.201
 */
@IqH2Test
class IqH2ApiCiConfigurationResourceTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private CiIntegrationsConfigDao ciConfigDao;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    ciConfigDao = ctx.lookup(CiIntegrationsConfigDao.class);
    objectMapper = ctx.lookup(ObjectMapper.class);
  }

  @Test
  void testGetConfiguration_success() throws Exception {
    // Given: Organization with configuration
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = createConfig();
    saveConfig(org.getId(), config);

    // When: Requesting configuration via REST
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .query("direct", true)
        .get();

    // Then: Returns 200 with configuration
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("CI");

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  void testGetConfiguration_notFound() throws Exception {
    // Given: Organization with no configuration
    Organization org = ctx.tempEntity().newOrganization();

    // When: Requesting configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .query("direct", true)
        .get();

    // Then: Returns 404 Not Found
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testSetConfiguration_success() throws Exception {
    // Given: Valid CI configuration
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    config.setFailBuildOnPolicyWarnings(true);

    // When: Setting configuration via REST
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 200 with configuration
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("CI");

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  void testSetConfiguration_validation_nullConfig() throws Exception {
    // Given: Organization without configuration
    Organization org = ctx.tempEntity().newOrganization();

    // When: Attempting to set null configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(null)
        .put();

    // Then: Returns 400 Bad Request
    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testSetConfiguration_validation_emptyString() throws Exception {
    // Given: Configuration with whitespace-only parameterPriority
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("   ");

    // When: Attempting to set an invalid configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("parameterPriority cannot be empty");
  }

  @Test
  void testSetConfiguration_validation_emptyResultFile() throws Exception {
    // Given: Configuration with whitespace-only resultFile
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setResultFile("   ");

    // When: Attempting to set an invalid configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("resultFile cannot be empty");
  }

  @Test
  void testSetConfiguration_validation_listWithEmptyString() throws Exception {
    // Given: Configuration with scanPatterns containing empty string
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setScanPatterns(Arrays.asList("**/*.jar", "", "**/*.war"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("scanPatterns[1] cannot be null or empty");
  }

  @Test
  void testSetConfiguration_validation_listWithNullValue() throws Exception {
    // Given: Configuration with moduleExcludes containing null value
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setModuleExcludes(Arrays.asList("test-module", null, "another-module"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("moduleExcludes[1] cannot be null or empty");
  }

  @Test
  void testSetConfiguration_validation_listWithWhitespaceString() throws Exception {
    // Given: Configuration with advancedProperties containing whitespace-only string
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setAdvancedProperties(Arrays.asList("prop1=value1", "   ", "prop2=value2"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("advancedProperties[1] cannot be null or empty");
  }

  @Test
  void testDeleteConfiguration_success() throws Exception {
    // Given: Organization with existing configuration
    Organization org = ctx.tempEntity().newOrganization();
    ApiCiConfigurationDto config = createConfig();
    saveConfig(org.getId(), config);

    // When: Deleting configuration via REST
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .delete();

    // Then: Returns 204 No Content
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteConfiguration_notFound() throws Exception {
    // Given: Organization without configuration
    Organization org = ctx.tempEntity().newOrganization();

    // When: Attempting to delete non-existent configuration
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .delete();

    // Then: Returns 404 Not Found
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("not found");
  }

  private void saveConfig(String ownerId, ApiCiConfigurationDto config) throws Exception {
    ciConfigDao.save(new CiIntegrationsConfig(
        ownerId, OwnerType.ORGANIZATION.toString(), objectMapper.writeValueAsString(config)));
  }

  private ApiCiConfigurationDto createConfig() {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    return config;
  }
}
