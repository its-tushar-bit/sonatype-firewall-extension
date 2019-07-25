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

public class AbstractMembershipMappingAuditTest
    extends AbstractAuditTest
{
  private List<MembershipMapping> originalMembershipMappings;

  @Before
  public void saveOriginalMembershipMappings() {
    originalMembershipMappings = new MembershipMappingDAO().getAll().stream()
        .map(mm -> new MembershipMapping(mm.getContextId(), mm.getRoleId(), mm.getMemberName(), mm.getMemberType()))
        .collect(Collectors.toList());
  }

  @After
  public void restoreOriginalMembershipMappings() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.getAll().forEach(membershipMappingDAO::delete);
    originalMembershipMappings.forEach(membershipMappingDAO::insert);
  }

  protected void assertRoleMembershipData(AuditDTO auditDTO, String roleId, List<Member> members) {
    assertRoleData(auditDTO, roleId);
    assertCustomObject(auditDTO, "roleMembers", MemberDTO.transcribe(members));
  }

  protected void assertRoleMembershipData(AuditDTO auditDTO, String roleId, Member member) {
    assertRoleData(auditDTO, roleId);
    assertCustomObject(auditDTO, "roleMember", MemberDTO.transcribe(member));
  }

  private void assertRoleData(final AuditDTO auditDTO, final String roleId) {
    Role role = new RoleDAO().getByIdNotNull(roleId);
    assertCustomData(auditDTO, "roleId", role.getId());
    assertCustomData(auditDTO, "roleName", role.getName());
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
      assertRoleMembershipData(findFirstByDataKeyValue(auditDTOs, "roleId", roleId), roleId, roleToMembers.get(roleId));
    }
  }
}
