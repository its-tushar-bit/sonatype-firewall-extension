/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ci;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationResponseDto;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for CiConfigurationService.
 *
 * @since 1.201
 */
public class CiConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private CiConfigurationService service;

  @Inject
  private CiIntegrationsConfigDao ciConfigDao;

  @Inject
  private ObjectMapper objectMapper;

  @Test
  public void testGetConfiguration_Authorized() throws Exception {
    // Given: Organization with configuration and READ permission
    Organization org = tempEntity.newOrganization();
    grantReadPermission(org.getId());
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    // When: Getting configuration with proper permission
    ApiCiConfigurationResponseDto result = service.getConfiguration(ORGANIZATION, org.getId(), true);

    // Then: Returns configuration successfully
    assertThat(result).isNotNull();
    assertThat(result.getData().getParameterPriority()).isEqualTo("CI");

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() throws Exception {
    // Given: Organization with configuration but no authentication
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    try {
      // When: Attempting to get configuration without authentication
      // Then: Throws UnauthenticatedException
      service.getConfiguration(ORGANIZATION, org.getId(), true);
    }
    finally {
      // Cleanup even if an exception is thrown
      ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
    }
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() throws Exception {
    // Given: Organization with configuration but no READ permission
    Organization org = tempEntity.newOrganization();
    Organization otherOrg = tempEntity.newOrganization();
    grantReadPermission(otherOrg.getId()); // Wrong permission
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    try {
      // When: Attempting to get configuration without permission
      // Then: Throws UnauthorizedException
      service.getConfiguration(ORGANIZATION, org.getId(), true);
    }
    finally {
      // Cleanup even if an exception is thrown
      ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
    }
  }

  @Test
  public void testGetConfiguration_ApplicationAuthorized() throws Exception {
    // Given: Application with configuration and READ permission
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("myApp", org.getId());
    grantReadPermission(app.getId());
    saveConfig(app.getId(), APPLICATION, createConfig("API"));

    // When: Getting application configuration with proper permission
    ApiCiConfigurationResponseDto result = service.getConfiguration(APPLICATION, app.getId(), true);

    // Then: Returns configuration successfully
    assertThat(result).isNotNull();
    assertThat(result.getData().getParameterPriority()).isEqualTo("API");

    // Cleanup
    ciConfigDao.delete(APPLICATION.toString(), app.getId());
  }

  @Test
  public void testSetConfiguration_Authorized() {
    // Given: Organization with WRITE permission
    Organization org = tempEntity.newOrganization();
    grantWritePermission(org.getId());
    ApiCiConfigurationDto config = createConfig("CI");

    // When: Setting configuration with proper permission
    ApiCiConfigurationDto result = service.setConfiguration(ORGANIZATION, org.getId(), config);

    // Then: Configuration is saved successfully
    assertThat(result).isNotNull();
    assertThat(result.getParameterPriority()).isEqualTo("CI");

    // Cleanup
    service.deleteConfiguration(ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    // Given: Organization but no authentication
    Organization org = tempEntity.newOrganization();
    ApiCiConfigurationDto config = createConfig("CI");

    // When: Attempting to set configuration without authentication
    // Then: Throws UnauthenticatedException
    service.setConfiguration(ORGANIZATION, org.getId(), config);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    // Given: Organization but only READ permission
    Organization org = tempEntity.newOrganization();
    grantReadPermission(org.getId());
    ApiCiConfigurationDto config = createConfig("CI");

    // When: Attempting to set configuration without WRITE permission
    // Then: Throws UnauthorizedException
    service.setConfiguration(ORGANIZATION, org.getId(), config);
  }

  @Test
  public void testSetConfiguration_ApplicationAuthorized() {
    // Given: Application with WRITE permission
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("myApp", org.getId());
    grantWritePermission(app.getId());
    ApiCiConfigurationDto config = createConfig("API");

    // When: Setting application configuration with proper permission
    ApiCiConfigurationDto result = service.setConfiguration(APPLICATION, app.getId(), config);

    // Then: Configuration is saved successfully
    assertThat(result).isNotNull();
    assertThat(result.getParameterPriority()).isEqualTo("API");

    // Cleanup
    service.deleteConfiguration(APPLICATION, app.getId());
  }

  @Test
  public void testDeleteConfiguration_Authorized() throws Exception {
    // Given: Organization with configuration and WRITE permission
    Organization org = tempEntity.newOrganization();
    grantWritePermission(org.getId());
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    // When: Deleting configuration with proper permission
    service.deleteConfiguration(ORGANIZATION, org.getId());

    // Then: Configuration is deleted successfully
    assertThat(ciConfigDao.findByOwner(ORGANIZATION.toString(), org.getId())).isEmpty();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() throws Exception {
    // Given: Organization with configuration but no authentication
    Organization org = tempEntity.newOrganization();
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    try {
      // When: Attempting to delete configuration without authentication
      // Then: Throws UnauthenticatedException
      service.deleteConfiguration(ORGANIZATION, org.getId());
    }
    finally {
      // Cleanup even if an exception is thrown
      ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
    }
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() throws Exception {
    // Given: Organization with configuration but only READ permission
    Organization org = tempEntity.newOrganization();
    grantReadPermission(org.getId());
    saveConfig(org.getId(), ORGANIZATION, createConfig("CI"));

    try {
      // When: Attempting to delete configuration without WRITE permission
      // Then: Throws UnauthorizedException
      service.deleteConfiguration(ORGANIZATION, org.getId());
    }
    finally {
      // Cleanup even if an exception is thrown
      ciConfigDao.delete(ORGANIZATION.toString(), org.getId());
    }
  }

  private void saveConfig(String ownerId, OwnerType ownerType, ApiCiConfigurationDto config) throws Exception {
    ciConfigDao.save(new CiIntegrationsConfig(
        ownerId, ownerType.toString(), objectMapper.writeValueAsString(config)));
  }

  private ApiCiConfigurationDto createConfig(String parameterPriority) {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority(parameterPriority);
    return config;
  }
}
