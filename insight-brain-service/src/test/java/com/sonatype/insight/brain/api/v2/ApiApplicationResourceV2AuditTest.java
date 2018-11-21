/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.AbstractMembershipMappingAuditTest;
import com.sonatype.insight.brain.security.Member;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class ApiApplicationResourceV2AuditTest
    extends AbstractMembershipMappingAuditTest
{
  @Test
  public void testSetMembershipMappingForRole() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = apiRoleMemberMappingListDTO();

    setMembershipMappingRequest(application.getId(), apiRoleMemberMappingListDTO).put();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP,
        apiRoleMemberMappingListDTO.memberMappings.size(), null);
    auditDTOs.forEach(auditDTO -> assertApplicationData(auditDTO, application));
    assertRoleMembershipData(auditDTOs, apiRoleMemberMappingListDTO);
  }

  @Test
  public void testSetMembershipMappingForRole_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    setMembershipMappingRequest(application.getId(), apiRoleMemberMappingListDTO()).with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO() {
    ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = new ApiRoleMemberMappingListDTO();
    apiRoleMemberMappingListDTO.memberMappings = new ArrayList<>();
    for (Role role : new RoleDAO().getApplicationRoles()) {
      ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = new ApiRoleMemberMappingDTO();
      apiRoleMemberMappingDTO.roleId = role.getId();
      apiRoleMemberMappingDTO.members = new ArrayList<>();
      for (MemberType memberType : MemberType.values()) {
        ApiMemberDTO apiMemberDTO = new ApiMemberDTO();
        apiMemberDTO.type = memberType;
        apiMemberDTO.userOrGroupName = tempEntity.uuid();
        apiRoleMemberMappingDTO.members.add(apiMemberDTO);
      }
      apiRoleMemberMappingListDTO.memberMappings.add(apiRoleMemberMappingDTO);
    }
    return apiRoleMemberMappingListDTO;
  }

  private HttpRequest setMembershipMappingRequest(String applicationId,
                                                  ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    return restRequest().path(PublicApiPaths.APP_RESOURCE_PATH, ApiApplicationResourceV2.ROLE_MEMBERS_PATH)
        .parameter(applicationId).body(apiRoleMemberMappingListDTO);
  }

  private void assertRoleMembershipData(List<AuditDTO> auditDTOs,
                                        ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    Map<String, List<Member>> roleToMembers = new ApiMemberMappingAdapter().convert(apiRoleMemberMappingListDTO);
    for (String roleId : roleToMembers.keySet()) {
      AuditDTO auditDTO = auditDTOs.stream().filter(a -> a.data.get("roleId").equals(roleId)).findFirst().orElse(null);
      assertThat("Failed to find audit log entry for role id " + roleId, auditDTO, notNullValue());
      assertRoleMembershipData(auditDTO, roleId, roleToMembers.get(roleId));
    }
  }
}
