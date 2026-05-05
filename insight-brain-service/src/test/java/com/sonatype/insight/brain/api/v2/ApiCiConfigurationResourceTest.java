/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

/**
 * Tests for ApiCiConfigurationResource REST endpoints.
 * Focuses on HTTP-level concerns: status codes, request validation, and serialization.
 * Business logic tests are in CiConfigurationServiceTest.
 *
 * @since 1.201
 */
@Category(SlowTest.class)
public class ApiCiConfigurationResourceTest
    extends AbstractResourceTest
{
  private CiIntegrationsConfigDao ciConfigDao;

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    ciConfigDao = lookup(CiIntegrationsConfigDao.class);
    objectMapper = lookup(ObjectMapper.class);
  }

  @Test
  public void testGetConfiguration_success() throws Exception {
    // Given: Organization with configuration
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = createConfig();
    saveConfig(org.getId(), config);

    // When: Requesting configuration via REST
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .query("direct", true)
        .get();

    // Then: Returns 200 with configuration
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("CI");

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  public void testGetConfiguration_notFound() throws Exception {
    // Given: Organization with no configuration
    Organization org = tempEntity.newOrganization();

    // When: Requesting configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .query("direct", true)
        .get();

    // Then: Returns 404 Not Found
    assertResponseStatus(404, response);
  }

  @Test
  public void testSetConfiguration_success() throws Exception {
    // Given: Valid CI configuration
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    config.setFailBuildOnPolicyWarnings(true);

    // When: Setting configuration via REST
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 200 with configuration
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("CI");

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  public void testSetConfiguration_validation_nullConfig() throws Exception {
    // Given: Organization without configuration
    Organization org = tempEntity.newOrganization();

    // When: Attempting to set null configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(null)
        .put();

    // Then: Returns 400 Bad Request
    assertResponseStatus(400, response);
  }

  @Test
  public void testSetConfiguration_validation_emptyString() throws Exception {
    // Given: Configuration with whitespace-only parameterPriority
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("   ");

    // When: Attempting to set an invalid configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("parameterPriority cannot be empty");
  }

  @Test
  public void testSetConfiguration_validation_emptyResultFile() throws Exception {
    // Given: Configuration with whitespace-only resultFile
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setResultFile("   ");

    // When: Attempting to set an invalid configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("resultFile cannot be empty");
  }

  @Test
  public void testSetConfiguration_validation_listWithEmptyString() throws Exception {
    // Given: Configuration with scanPatterns containing empty string
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setScanPatterns(Arrays.asList("**/*.jar", "", "**/*.war"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("scanPatterns[1] cannot be null or empty");
  }

  @Test
  public void testSetConfiguration_validation_listWithNullValue() throws Exception {
    // Given: Configuration with moduleExcludes containing null value
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setModuleExcludes(Arrays.asList("test-module", null, "another-module"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("moduleExcludes[1] cannot be null or empty");
  }

  @Test
  public void testSetConfiguration_validation_listWithWhitespaceString() throws Exception {
    // Given: Configuration with advancedProperties containing whitespace-only string
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setAdvancedProperties(Arrays.asList("prop1=value1", "   ", "prop2=value2"));

    // When: Attempting to set an invalid configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .body(config)
        .put();

    // Then: Returns 400 with a validation error
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("advancedProperties[1] cannot be null or empty");
  }

  @Test
  public void testDeleteConfiguration_success() throws Exception {
    // Given: Organization with existing configuration
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = createConfig();
    saveConfig(org.getId(), config);

    // When: Deleting configuration via REST
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .delete();

    // Then: Returns 204 No Content
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteConfiguration_notFound() throws Exception {
    // Given: Organization without configuration
    Organization org = tempEntity.newOrganization();

    // When: Attempting to delete non-existent configuration
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getPublicId())
        .delete();

    // Then: Returns 404 Not Found
    assertResponseStatus(404, response);
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
