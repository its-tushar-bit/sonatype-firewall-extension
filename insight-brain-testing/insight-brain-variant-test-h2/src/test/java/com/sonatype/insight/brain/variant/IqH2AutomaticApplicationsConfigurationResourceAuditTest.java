/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfiguration;
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfigurationResource;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2AutomaticApplicationsConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization organization;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    organization = ctx.tempEntity().newOrganization();
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

  private HttpRequest automaticApplicationsConfigurationRequest() {
    return ctx.restRequest().path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH);
  }

  @Test
  void testUpdate_Enabled() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, null);
    assertCustomData(auditDTO, "automaticApplicationCreation", "enabled");
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdate_Disabled() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(false, null)).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, null);
    assertCustomData(auditDTO, "automaticApplicationCreation", "disabled");
  }

  @Test
  void testUpdate_Unauthorized() throws Exception {
    automaticApplicationsConfigurationRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .with(unauthorizedUser())
        .put();

    assertAuditLog(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS, "unauthorized");
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
