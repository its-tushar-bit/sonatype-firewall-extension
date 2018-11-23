/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class OrganizationResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testAddOrganization() throws Exception {
    Organization organization = new Organization("orgName");

    organization = organizationRequest().body(organization).post().getBody(Organization.class);
    tempEntity.register(organization);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    organizationRequest().body(new Organization()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testUpdateOrganization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setName("updatedName");

    organizationRequest().body(organization).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateOrganization_Unauthorized() throws Exception {
    organizationRequest().body(tempEntity.newOrganization()).with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  private HttpRequest organizationRequest() {
    return restRequest().path(OrganizationResource.RESOURCE_PATH);
  }
}
