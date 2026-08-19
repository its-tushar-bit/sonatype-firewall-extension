/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfiguration;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfigurationResource;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H2 port of {@code AutomaticSourceControlConfigurationResourceAuditTest}.
 */
@IqH2Test
class IqH2AutomaticSourceControlConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
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

  private HttpRequest automaticSourceControlConfigurationRequest() {
    return ctx.restRequest().path(AutomaticSourceControlConfigurationResource.RESOURCE_PATH);
  }

  @Test
  void testUpdateAutomaticSourceControl_Enabled() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(true)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "automaticSourceControlConfiguration", "enabled");
  }

  @Test
  void testUpdateAutomaticSourceControl_Disabled() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(false)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "automaticSourceControlConfiguration", "disabled");
  }

  @Test
  void testUpdateAutomaticSourceControl_Unauthorized() throws Exception {
    automaticSourceControlConfigurationRequest().body(
        new AutomaticSourceControlConfiguration(true)).with(httpRequest -> httpRequest.auth(unauthorizedUser)).put();

    assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL, "unauthorized");
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
