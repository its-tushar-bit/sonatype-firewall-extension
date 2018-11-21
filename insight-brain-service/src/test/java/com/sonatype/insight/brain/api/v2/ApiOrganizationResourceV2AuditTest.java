/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
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

  private HttpRequest setMembershipMappingRequest(String organizationId,
                                                  ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    return restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH, ApiOrganizationResourceV2.ROLE_MEMBERS_PATH)
        .parameter(organizationId).body(apiRoleMemberMappingListDTO);
  }
}
