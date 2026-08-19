/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;

/**
 * Audit tests for ApiCiConfigurationResource.
 * Tests verify that UPDATE_CI_CONFIGURATION and DELETE_CI_CONFIGURATION audit events
 * are properly logged for all CI configuration operations.
 *
 * @since 1.201
 */
@IqH2Test
class IqH2ApiCiConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private CiIntegrationsConfigDao ciConfigDao;

  private ObjectMapper objectMapper;

  private Organization testOrg;

  private Application testApp;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  public void setUp() {
    logOutput.before();
    logOutput.clear();
    ciConfigDao = ctx.lookup(CiIntegrationsConfigDao.class);
    objectMapper = ctx.lookup(ObjectMapper.class);
    testOrg = ctx.tempEntity().newOrganization();
    testApp = ctx.tempEntity().newApplication(testOrg.getPublicId());
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  @Test
  public void testSetConfiguration_Organization() throws Exception {
    ApiCiConfigurationDto dto = new ApiCiConfigurationDto();
    dto.setParameterPriority("CI");
    dto.setFailBuildOnPolicyWarnings(true);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(dto)
        .put();

    ctx.assertResponseStatus(200, response);
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

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .body(dto)
        .put();

    ctx.assertResponseStatus(200, response);
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

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(newConfig)
        .put();

    ctx.assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, null);
    assertCustomObject(auditDTO, "ciConfiguration", newConfig);

    // Cleanup
    ciConfigDao.delete(ORGANIZATION.toString(), testOrg.getId());
  }

  @Test
  public void testSetConfiguration_Error_NullConfig() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .body(null)
        .put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.UPDATE_CI_CONFIGURATION, "bad-request");
  }

  @Test
  public void testDeleteConfiguration_Organization() throws Exception {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    ciConfigDao.save(new CiIntegrationsConfig(
        testOrg.getId(), ORGANIZATION.toString(), objectMapper.writeValueAsString(config)));

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, null);
  }

  @Test
  public void testDeleteConfiguration_Application() throws Exception {
    ApiCiConfigurationDto config = new ApiCiConfigurationDto();
    config.setParameterPriority("CI");
    ciConfigDao.save(new CiIntegrationsConfig(
        testApp.getId(), APPLICATION.toString(), objectMapper.writeValueAsString(config)));

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(APPLICATION, testApp.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, null);
  }

  @Test
  public void testDeleteConfiguration_Error_NotFound() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.CI_CONFIG_RESOURCE_PATH_V2)
        .parameter(ORGANIZATION, testOrg.getId())
        .delete();

    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_CI_CONFIGURATION, "not-found");
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
