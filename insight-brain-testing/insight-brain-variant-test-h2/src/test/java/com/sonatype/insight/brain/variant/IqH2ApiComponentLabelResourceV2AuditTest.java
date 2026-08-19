/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H2 port of {@code ApiComponentLabelResourceV2AuditTest}.
 */
@IqH2Test
class IqH2ApiComponentLabelResourceV2AuditTest
    implements AuditTestSupport
{
  private static final String COMPONENT_HASH = "componentHash";

  private IqTestContext ctx;

  private Organization organization;

  private Application application;

  private Label label;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    organization = ctx.tempEntity().newOrganization();
    application = ctx.tempEntity().newApplication(organization.getId());
    label = ctx.tempEntity().newLabel(organization.getId());
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
    return ctx.restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2);
  }

  @Test
  void testSetComponentLabel_Application() throws Exception {
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), OwnerType.APPLICATION, application.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  void testSetComponentLabel_Organization() throws Exception {
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), OwnerType.ORGANIZATION, organization.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  void testSetComponentLabel_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser())
        .parameter(COMPONENT_HASH, label.getLabel(), OwnerType.APPLICATION, application.getId())
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testDeleteComponentLabel_Application() throws Exception {
    ctx.tempEntity().newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), OwnerType.APPLICATION, application.getId()).delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  void testDeleteComponentLabel_Organization() throws Exception {
    ctx.tempEntity().newComponentLabel(organization.getId(), label.getId(), COMPONENT_HASH);
    restRequest().parameter(COMPONENT_HASH, label.getLabel(), OwnerType.ORGANIZATION, organization.getId()).delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO, label);
  }

  @Test
  void testDeleteComponentLabel_Unauthorized() throws Exception {
    ctx.tempEntity().newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest().with(unauthorizedUser())
        .parameter(COMPONENT_HASH, label.getLabel(), OwnerType.APPLICATION, application.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void assertComponentLabelData(final AuditDTO auditDTO, final Label label) {
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
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
