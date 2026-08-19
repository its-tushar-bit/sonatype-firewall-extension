/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.integration} package because the test calls
 * {@link ApplicationSummaryResource#VERIFY_OR_CREATE_APPLICATION_PATH}, which is package-private. Implements
 * {@link AuditTestSupport} to reuse its audit-log capture/assertion helpers, as the legacy
 * {@code ApplicationSummaryResourceAuditTest} did via {@code AbstractAuditTest}.
 */
@IqH2Test
class IqH2ApplicationSummaryResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private ApplicationDAO applicationDAO;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    applicationDAO = ctx.lookup(ApplicationDAO.class);
  }

  @AfterEach
  void tearDown() {
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
  void testVerifyOrCreateApplication() throws Exception {
    Organization organization = ctx.tempEntity().newOrganizationAutomaticApplicationsConfiguration();
    String nonExistentAppPublicId = TemporaryEntity.uuid();

    verifyOrCreateApplicationRequest().parameter(nonExistentAppPublicId).post();
    Application persistedApp = applicationDAO.getByPublicIdNotNull(nonExistentAppPublicId);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.AUTO_CREATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, persistedApp, organization);
  }

  private void assertDetailedApplicationData(
      final AuditDTO auditDTO,
      final Application application,
      final Organization organization)
  {
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
    assertParentOrganizationData(auditDTO, organization);
  }

  private HttpRequest verifyOrCreateApplicationRequest() {
    return ctx.restRequest()
        .path(ApplicationSummaryResource.RESOURCE_PATH)
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APPLICATION_PATH)
        .query("goal", Goal.EVALUATE_APPLICATION);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... names) {
      super(names);
    }

    void tearDown() {
      after();
    }
  }
}
