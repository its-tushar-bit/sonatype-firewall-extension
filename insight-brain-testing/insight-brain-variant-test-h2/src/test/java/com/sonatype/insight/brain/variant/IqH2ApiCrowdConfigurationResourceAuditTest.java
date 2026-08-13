/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCrowdConfigurationService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiCrowdConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
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
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testInsertOrUpdateCrowdConfiguration() throws Exception {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = "serverUrl";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CROWD, null);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_SERVER_URL_AUDIT_KEY, dto.serverUrl);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_APPLICATION_NAME_AUDIT_KEY, dto.applicationName);
  }

  @Test
  void testInsertOrUpdateCrowdConfiguration_BadRequest() throws Exception {
    HttpResponse response = restRequest().body(null).put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_CROWD, "bad-request");
  }

  @Test
  void testDeleteCrowdConfiguration() throws Exception {
    CrowdConfiguration crowdConfiguration = ctx.tempEntity().newCrowdConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_CROWD, null);
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_SERVER_URL_AUDIT_KEY,
        crowdConfiguration.getServerUrl());
    assertCustomData(auditDTO, ApiCrowdConfigurationService.CROWD_APPLICATION_NAME_AUDIT_KEY,
        crowdConfiguration.getApplicationName());
  }

  @Test
  void testDeleteCrowdConfiguration_Unauthorized() throws Exception {
    HttpResponse response = restRequest().with(unauthorizedUser()).delete();

    ctx.assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.DELETE_CROWD, "unauthorized");
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
