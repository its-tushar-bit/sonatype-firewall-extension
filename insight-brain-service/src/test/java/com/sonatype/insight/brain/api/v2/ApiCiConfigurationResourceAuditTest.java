/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

/**
 * Audit tests for ApiCiConfigurationResource.
 * Tests verify that UPDATE_CI_CONFIGURATION and DELETE_CI_CONFIGURATION audit events
 * are properly logged for all CI configuration operations.
 *
 * @since 1.201
 */
@Category(SlowTest.class)
public class ApiCiConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private CiIntegrationsConfigDao ciConfigDao;

  private ObjectMapper objectMapper;

  private Organization testOrg;

  private Application testApp;

  @Before
  public void setUp() {
    ciConfigDao = lookup(CiIntegrationsConfigDao.class);
    objectMapper = lookup(ObjectMapper.class);
    testOrg = tempEntity.newOrganization();
    testApp = tempEntity.newApplication(testOrg.getPublicId());
  }

  @Test
  public void testSetConfiguration_Organization() throws Exception {
    ApiCiConfigurationDto dto = new ApiCiConfigurationDto();
    dto.setParameterPriority("CI");
    dto.setFailBuildOnPolicyWarnings(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(dto)
        .put();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, null);
    assertCustomObject(auditDTO, "ciConfiguration", dto);

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), testOrg.getId());
  }

  @Test
  public void testSetConfiguration_Application() throws Exception {
    ApiCiConfigurationDto dto = new ApiCiConfigurationDto();
    dto.setParameterPriority("CI");
    dto.setFailBuildOnPolicyWarnings(false);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .body(dto)
        .put();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, null);
    assertCustomObject(auditDTO, "ciConfiguration", dto);

    // Cleanup
    ciConfigDao.delete(APPLICATION.toString(), testApp.getId());
  }

  @Test
  public void testSetConfiguration_Update() throws Exception {
    ApiCiConfigurationDto initialConfig = new ApiCiConfigurationDto();
    initialConfig.setParameterPriority("CI");
    ciConfigDao.save(new CiIntegrationsConfig(
        testOrg.getId(), ORGANIZATION.toString(), objectMapper.writeValueAsString(initialConfig)));

    ApiCiConfigurationDto newConfig = new ApiCiConfigurationDto();
    newConfig.setParameterPriority("CI");
    newConfig.setFailBuildOnPolicyWarnings(true);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(newConfig)
        .put();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, null);
    assertCustomObject(auditDTO, "ciConfiguration", newConfig);

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), testOrg.getId());
  }

  @Test
  public void testSetConfiguration_Error_NullConfig() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(null)
        .put();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, "bad-request");
  }

  @Test
  public void testDeleteConfiguration_Organization() throws Exception {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    ciConfigDao.save(new CiIntegrationsConfig(
        testOrg.getId(), ORGANIZATION.toString(), objectMapper.writeValueAsString(config)));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, null);
  }

  @Test
  public void testDeleteConfiguration_Application() throws Exception {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    ciConfigDao.save(new CiIntegrationsConfig(
        testApp.getId(), APPLICATION.toString(), objectMapper.writeValueAsString(config)));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .delete();

    assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, null);
  }

  @Test
  public void testDeleteConfiguration_Error_NotFound() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, "not-found");
  }
}
