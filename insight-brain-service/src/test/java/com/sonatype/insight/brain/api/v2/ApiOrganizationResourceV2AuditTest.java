/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiOrganizationResourceV2AuditTest
    extends AbstractAuditTest
{
  private OrganizationDAO organizationDAO;

  private Organization parentOrg;

  private Organization childOrg;

  private Organization targetOrg;

  @Before
  public void before() {
    organizationDAO = lookup(OrganizationDAO.class);

    parentOrg = tempEntity.newOrganization();
    childOrg = tempEntity.newOrganization(parentOrg);
    targetOrg = tempEntity.newOrganization();
  }

  @Test
  public void testAddOrganization() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().body(organizationDto).post();
    Organization organization = organizationDAO.getByName(organizationDto.name);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().with(unauthorizedUser()).body(organizationDto).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testMoveOrganization() throws Exception {
    moveOrganizationApiRequest().parameter(childOrg.getId(), targetOrg.getId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationAndParentData(auditDTO, childOrg, targetOrg);
  }

  @Test
  public void testMoveOrganization_Unauthorized() throws Exception {
    moveOrganizationApiRequest().with(unauthorizedUser()).parameter(childOrg.getId(), targetOrg.getId()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    Organization organization = tempEntity.newOrganization();

    organizationApiRequest().path(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteOrganization_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();

    organizationApiRequest().path(organization.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_ORGANIZATION, "unauthorized");
  }

  private HttpRequest organizationApiRequest() {
    return restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }

  private HttpRequest moveOrganizationApiRequest() {
    return organizationApiRequest().path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH);
  }
}
