/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.organization.IconUtils;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.variant} package; reproduces the {@code AbstractAuditTest}
 * scaffolding (log capture + unauthorized-user helper) that the legacy {@code ApplicationResourceAuditTest}
 * inherited from its base class.
 */
@IqH2Test
class IqH2ApplicationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private ApplicationDAO applicationDAO;

  private Organization organization;

  private Application application;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private HttpRequest applicationRequest() {
    return ctx.restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    applicationDAO = ctx.lookup(ApplicationDAO.class);
    organization = ctx.tempEntity().newOrganization();
    application = ctx.tempEntity().newApplication(organization.getId());
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

  @Test
  void testAddApplication() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());
    User contact = ctx.tempEntity().newUser("aContact");
    application.setContactInternalName(contact.getUsername());
    applicationRequest().body(application).post();

    Application persistedApp = applicationDAO.getByName(application.getName());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(persistedApp, auditDTO, application.getContactInternalName());
  }

  @Test
  void testAddApplication_Unauthorized() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());

    applicationRequest().with(unauthorizedUser()).body(application).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdateApplication() throws Exception {
    Application application = ctx.tempEntity()
        .newApplication("existing-app", "existing-app-public-id", organization.getId(), "aContact");
    application.setName("new-name");

    applicationRequest().body(application).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, application.getContactInternalName());
  }

  @Test
  void testUpdateApplication_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplication(organization.getId());
    applicationRequest().with(unauthorizedUser()).body(application).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testDeleteApplication() throws Exception {
    Application application = ctx.tempEntity()
        .newApplication("existing-app", "existing-app-public-id", organization.getId(), "aContact");
    applicationRequest().path(application.getPublicId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, application.getContactInternalName());
  }

  @Test
  void testDeleteApplication_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplication(organization.getId());
    applicationRequest().path(application.getPublicId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testSetIcon_Robot() throws Exception {
    applicationRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "true")
        .part("hashcode", "")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_ICON, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "iconType", "robot");
  }

  @Test
  void testSetIcon_File() throws Exception {
    String iconFilename = "defaulticon_application.png";

    applicationRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .part("file", iconFilename, IconUtils.loadIconFromProductAssets("defaulticon_application.png"))
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_ICON, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "iconType", "file");
    assertCustomData(auditDTO, "iconFilename", iconFilename);
  }

  @Test
  void testSetIcon_Default() throws Exception {
    applicationRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_ICON, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "iconType", "default");
  }

  @Test
  void testSetIcon_Unauthorized() throws Exception {
    applicationRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_ICON, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void assertDetailedApplicationData(
      final Application application,
      final AuditDTO auditDTO,
      final String contactInternalName)
  {
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", contactInternalName);
    assertParentOrganizationData(auditDTO, organization);
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
