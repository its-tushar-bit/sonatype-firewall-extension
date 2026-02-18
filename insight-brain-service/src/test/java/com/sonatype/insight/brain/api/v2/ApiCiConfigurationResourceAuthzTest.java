/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for ApiCiConfigurationResource.
 * Tests verify that proper permissions (READ for GET, WRITE for PUT/DELETE) are required
 * for all CI configuration operations.
 *
 * @since 1.201
 */
public class ApiCiConfigurationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private CiIntegrationsConfigDao ciConfigDao;

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    ciConfigDao = lookup(CiIntegrationsConfigDao.class);
    objectMapper = lookup(ObjectMapper.class);
  }

  @Test
  public void testGetConfiguration_Organization_AuthorizedUser() throws Exception {
    // Given: CI configuration exists for an organization
    Organization org = tempEntity.newOrganization();
    createCiConfig(org.getId(), ORGANIZATION.toString());

    // When: Authenticated admin user (has READ permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  public void testGetConfiguration_Application_AuthorizedUser() throws Exception {
    // Given: CI configuration exists for an application
    Application app = tempEntity.newApplication(org.getPublicId());
    createCiConfig(app.getId(), APPLICATION.toString());

    // When: Authenticated admin user (has READ permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);

    // Cleanup
    ciConfigDao.delete(APPLICATION.toString(), app.getId());
  }

  @Test
  public void testGetConfiguration_Organization_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetConfiguration_Application_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfiguration_Organization_AuthorizedUser() throws Exception {
    // Given: Valid CI configuration and an organization
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = createValidConfiguration();

    // When: Authenticated admin user (has WRITE permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .body(config)
        .put();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test
  public void testSetConfiguration_Application_AuthorizedUser() throws Exception {
    // Given: Valid CI configuration and an application
    Application app = tempEntity.newApplication(org.getPublicId());
    ApiCiConfigurationDto config = createValidConfiguration();

    // When: Authenticated admin user (has WRITE permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .body(config)
        .put();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);

    // Cleanup
    ciConfigDao.delete(APPLICATION.toString(), app.getId());
  }

  @Test
  public void testSetConfiguration_Organization_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .body(new ApiCiConfigurationDto())
        .put();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfiguration_Application_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .body(new ApiCiConfigurationDto())
        .put();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testDeleteConfiguration_Organization_AuthorizedUser() throws Exception {
    // Given: CI configuration exists for an organization
    Organization org = tempEntity.newOrganization();
    createCiConfig(org.getId(), ORGANIZATION.toString());

    // When: Authenticated admin user (has WRITE permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, org.getId())
        .auth()
        .delete();

    // Then: Should return 204 No Content
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteConfiguration_Application_AuthorizedUser() throws Exception {
    // Given: CI configuration exists for an application
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getPublicId());
    createCiConfig(app.getId(), APPLICATION.toString());

    // When: Authenticated admin user (has WRITE permission)
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, app.getId())
        .auth()
        .delete();

    // Then: Should return 204 No Content
    assertThat(response.getStatusCode()).isEqualTo(204);
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, "anyId")
        .anon()
        .delete();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testDeleteConfiguration_Application_Unauthenticated() throws Exception {
    // When: Unauthenticated request
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, "anyId")
        .anon()
        .delete();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  private void createCiConfig(String ownerId, String ownerType) throws Exception {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    config.setFailBuildOnPolicyWarnings(true);

    CiIntegrationsConfig entity = new CiIntegrationsConfig(
        ownerId, ownerType, objectMapper.writeValueAsString(config));
    ciConfigDao.save(entity);
  }

  private ApiCiConfigurationDto createValidConfiguration() {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    config.setFailBuildOnPolicyWarnings(true);

    return config;
  }
}
