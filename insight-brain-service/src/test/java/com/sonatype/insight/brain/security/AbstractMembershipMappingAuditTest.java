/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.ApiMemberMappingAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class AbstractMembershipMappingAuditTest
    extends AbstractAuditTest
{
  private List<MembershipMapping> originalMembershipMapppings;

  @Before
  public void saveOriginalMembershipMapppings() {
    originalMembershipMapppings = new MembershipMappingDAO().getAll().stream()
        .map(mm -> new MembershipMapping(mm.getContextId(), mm.getRoleId(), mm.getMemberName(), mm.getMemberType()))
        .collect(Collectors.toList());
  }

  @After
  public void restoreOriginalMembershipMapppings() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.getAll().forEach(membershipMappingDAO::delete);
    originalMembershipMapppings.forEach(membershipMappingDAO::insert);
  }

  protected void assertRoleMembershipData(AuditDTO auditDTO, String roleId, List<Member> members) {
    Role role = new RoleDAO().getByIdNotNull(roleId);
    assertCustomData(auditDTO, "roleId", role.getId());
    assertCustomData(auditDTO, "roleName", role.getName());
    assertCustomObject(auditDTO, "roleMembers", MemberDTO.transcribe(members));
  }

  protected ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO() {
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

  protected void assertRoleMembershipData(List<AuditDTO> auditDTOs,
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
