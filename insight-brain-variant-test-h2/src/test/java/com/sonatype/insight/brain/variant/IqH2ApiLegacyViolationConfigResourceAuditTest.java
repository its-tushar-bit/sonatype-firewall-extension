/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Audit tests for {@code ApiLegacyViolationConfigResource}. Verifies that setConfig (PUT) writes the
 * CONFIGURE_LEGACY_VIOLATION_STATUS audit event for both APPLICATION and ORGANIZATION owner types.
 */
@IqH2Test
class IqH2ApiLegacyViolationConfigResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization organization;

  private Application application;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    organization = ctx.tempEntity().newOrganization();
    application = ctx.tempEntity().newApplicationWithParent();
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
    return ctx.restRequest().path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2);
  }

  @Test
  void testSetConfig_Application_AuditsConfigureLegacyViolationStatus() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    restRequest().path("application/{ownerId}")
        .parameter(application.getPublicId())
        .body(request)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testSetConfig_Organization_AuditsConfigureLegacyViolationStatus() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;
    request.allowOverride = true;

    restRequest().path("organization/{ownerId}")
        .parameter(organization.getId())
        .body(request)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertOrganizationData(auditDTO, organization);
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
