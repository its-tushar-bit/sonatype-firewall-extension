/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApplicationMoveResourceAuditTest
    extends AbstractAuditTest
{
  private Application application;

  private Organization organization;

  @Before
  public void before() {
    application = tempEntity.newApplication("appName", "appPublicId", tempEntity.newOrganization().getId(),
        tempEntity.newUser("appContactName").getUsername());
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testMoveApplication() throws Exception {
    moveRequest(application.getId(), organization.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  public void testMoveApplication_ToSameOrganization() throws Exception {
    moveRequest(application.getId(), application.getParentOwnerId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
    assertParentOrganizationData(auditDTO, new OrganizationDAO().getByIdNotNull(application.getParentOwnerId()));
  }

  @Test
  public void testMoveApplication_UnauthorizedWrite() throws Exception {
    moveRequest(application.getId(), organization.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testMoveApplication_UnauthorizedAddApplication() throws Exception {
    tempEntity.newMembershipMapping(application.getId(), Role.OWNER_ROLE_ID, unauthorizedUser.getUsername());

    moveRequest(application.getId(), organization.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
    assertParentOrganizationData(auditDTO, organization);
  }

  private HttpRequest moveRequest(String applicationId, String organizationId) {
    return restRequest().path(ApplicationMoveResource.RESOURCE_PATH, ApplicationMoveResource.DESTINATION_PATH)
        .parameter(applicationId, organizationId);
  }
}
