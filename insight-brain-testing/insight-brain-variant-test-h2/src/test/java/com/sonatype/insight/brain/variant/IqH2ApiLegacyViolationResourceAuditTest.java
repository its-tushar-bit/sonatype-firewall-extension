/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegacyViolationResource;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit tests for {@code ApiLegacyViolationResource}.
 * Verifies that grant/revoke and list operations write the expected audit events.
 */
@IqH2Test
class IqH2ApiLegacyViolationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  private Application application;

  private PolicyEvaluation policyEvaluation;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    applicationDAO = ctx.lookup(ApplicationDAO.class);
    policyDAO = ctx.lookup(PolicyDAO.class);

    application = ctx.tempEntity().newApplicationWithParent();
    policyEvaluation = ctx.tempEntity().newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
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
    return policyDAO;
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2);
  }

  @Test
  void testGrant_AuditsApplyLegacyViolationStatus() throws Exception {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    Policy legacyAllowed = ctx.tempEntity().newPolicy();
    legacyAllowed.setLegacyViolationAllowed(true);
    policyDAO.update(legacyAllowed);
    ctx.tempEntity().newPolicyViolation(policyEvaluation, legacyAllowed);

    restRequest().path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(application.getPublicId())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertThat(auditDTO.data).containsEntry("changedPolicyViolationCount", 1);
  }

  @Test
  void testRevoke_AuditsRevokeLegacyViolationStatus() throws Exception {
    ctx.tempEntity().newLegacyPolicyViolation(policyEvaluation, ctx.tempEntity().newPolicy());

    restRequest().path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(application.getPublicId())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertThat(auditDTO.data).containsEntry("changedPolicyViolationCount", 1);
  }

  @Test
  void testList_AuditsExportPolicyViolations() throws Exception {
    ctx.tempEntity().newLegacyPolicyViolation(policyEvaluation, ctx.tempEntity().newPolicy());

    restRequest().path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(application.getPublicId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
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
