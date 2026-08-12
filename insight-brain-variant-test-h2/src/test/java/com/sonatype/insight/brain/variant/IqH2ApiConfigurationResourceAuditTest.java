/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private SystemConfigurationPropertyDAO dao;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    dao = ctx.lookup(SystemConfigurationPropertyDAO.class);
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

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testSetConfiguration() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    HttpResponse response = restRequest().body(properties).put();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPERTIES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    assertCustomData(auditDTO, SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_PROPERTIES, "bad-request");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    HttpResponse response = restRequest().query("property", SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL).delete();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_PROPERTIES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    assertCustomData(auditDTO, SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.DELETE_PROPERTIES, "bad-request");
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
