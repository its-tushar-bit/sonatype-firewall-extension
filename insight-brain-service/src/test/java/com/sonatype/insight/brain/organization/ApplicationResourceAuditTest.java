/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApplicationResourceAuditTest
    extends AbstractAuditTest
{
  private ApplicationDAO applicationDAO;

  private Organization organization;

  private Application application;

  private HttpRequest applicationRequest() {
    return restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  @Before
  public void before() {
    applicationDAO = lookup(ApplicationDAO.class);
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
  }

  @Test
  public void testAddApplication() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());
    User contact = tempEntity.newUser("aContact");
    application.setContactInternalName(contact.getUsername());
    applicationRequest().body(application).post();

    Application persistedApp = applicationDAO.getByName(application.getName());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(persistedApp, auditDTO, application.getContactInternalName());
  }

  @Test
  public void testAddApplication_Unauthorized() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());

    applicationRequest().with(unauthorizedUser()).body(application).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateApplication() throws Exception {
    Application application = tempEntity
        .newApplication("existing-app", "existing-app-public-id", organization.getId(), "aContact");
    application.setName("new-name");

    applicationRequest().body(application).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, application.getContactInternalName());
  }

  @Test
  public void testUpdateApplication_Unauthorized() throws Exception {
    Application application = tempEntity.newApplication(organization.getId());
    applicationRequest().with(unauthorizedUser()).body(application).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteApplication() throws Exception {
    Application application = tempEntity
        .newApplication("existing-app", "existing-app-public-id", organization.getId(), "aContact");
    applicationRequest().path(application.getPublicId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, application.getContactInternalName());
  }

  @Test
  public void testDeleteApplication_Unauthorized() throws Exception {
    Application application = tempEntity.newApplication(organization.getId());
    applicationRequest().path(application.getPublicId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testSetIcon_Robot() throws Exception {
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
  public void testSetIcon_File() throws Exception {
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
  public void testSetIcon_Default() throws Exception {
    applicationRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH)
        .parameter(application.getId())
        .part("hasRobotSource", "false")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_ICON, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "iconType", "default");
  }

  @Test
  public void testSetIcon_Unauthorized() throws Exception {
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
}
