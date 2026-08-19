/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.PolicyMonitoringResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractAuditTest}
 * scaffolding (log capture + unauthorized-user helper) that the legacy {@code PolicyMonitoringResourceAuditTest}
 * inherited from its base class.
 */
@IqH2Test
class IqH2PolicyMonitoringResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private Organization org;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent();
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

  private HttpRequest restRequest(Owner owner) {
    return restRequest(owner.getType(), owner.getPublicId());
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return ctx.restRequest().path(PolicyMonitoringResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  void testSet_Application() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), Stage.ID_RELEASE);
    restRequest(app).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  void testSet_Organization() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  void testSet_Unauthorized() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).with(unauthorizedUser()).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDelete_Application() throws Exception {
    PolicyMonitoring policyMonitoring = ctx.tempEntity().newPolicyMonitoring(app.getId(), Stage.ID_RELEASE);
    restRequest(app).query("stageTypeId", policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", "inherited");
  }

  @Test
  void testDelete_Organization() throws Exception {
    PolicyMonitoring policyMonitoring = ctx.tempEntity().newPolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).query("stageTypeId", policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "stageId", "inherited");
  }

  @Test
  void testDelete_RootOrganization() throws Exception {
    PolicyMonitoring policyMonitoring =
        ctx.tempEntity().newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, Stage.ID_RELEASE);
    restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).query("stageTypeId",
        policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org.getParentOrganizationId(), "Root Organization");
    assertCustomData(auditDTO, "stageId", "none");
  }

  @Test
  void testDelete_Unauthorized() throws Exception {
    restRequest(org).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, "unauthorized");
    assertOrganizationData(auditDTO, org);
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
