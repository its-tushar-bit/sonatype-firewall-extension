/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.AbstractMembershipMappingAuditTest;

import org.junit.Test;

public class ApiOrganizationResourceV2AuditTest
    extends AbstractMembershipMappingAuditTest
{
  @Test
  public void testSetMembershipMappingForRole() throws Exception {
    Organization organization = tempEntity.newOrganization();
    ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = apiRoleMemberMappingListDTO();

    setMembershipMappingRequest(organization.getId(), apiRoleMemberMappingListDTO).put();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP,
        apiRoleMemberMappingListDTO.memberMappings.size(), null);
    auditDTOs.forEach(auditDTO -> assertOrganizationData(auditDTO, organization));
    assertRoleMembershipData(auditDTOs, apiRoleMemberMappingListDTO);
  }

  @Test
  public void testSetMembershipMappingForRole_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();

    setMembershipMappingRequest(organization.getId(), apiRoleMemberMappingListDTO()).with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().body(organizationDto).post();
    Organization organization = new OrganizationDAO().getByName(organizationDto.name);
    tempEntity.register(organization);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().with(unauthorizedUser()).body(organizationDto).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  private HttpRequest organizationApiRequest() {
    return restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }

  private HttpRequest setMembershipMappingRequest(String organizationId,
                                                  ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    return organizationApiRequest().path(DefaultApiOrganizationResourceV2.ROLE_MEMBERS_PATH)
        .parameter(organizationId).body(apiRoleMemberMappingListDTO);
  }
}
