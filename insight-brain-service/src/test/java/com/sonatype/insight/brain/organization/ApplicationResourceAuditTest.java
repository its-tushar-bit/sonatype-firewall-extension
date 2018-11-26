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
  private Organization organization;

  private HttpRequest applicationRequest() {
    return restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testAddApplication() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());
    User contact = tempEntity.newUser("aContact");
    application.setContactInternalName(contact.getUsername());
    applicationRequest().body(application).post();

    Application persistedApp = new ApplicationDAO().getByName(application.getName());
    tempEntity.register(persistedApp);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(persistedApp, auditDTO, application.getContactInternalName());
  }

  @Test
  public void testAddApplication_Unauthorized() throws Exception {
    Application application = new Application("appPublicId", "appName", organization.getId());

    applicationRequest().with(unauthorizedUser()).body(application).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO);
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

  private void assertDetailedApplicationData(final Application application,
                                             final AuditDTO auditDTO,
                                             final String contactInternalName)
  {
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", contactInternalName);
    assertParentOrganizationData(auditDTO);
  }

  private void assertParentOrganizationData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "parentOrganizationId", organization.getId());
    assertCustomData(auditDTO, "parentOrganizationName", organization.getName());
  }
}
