/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import com.sonatype.insight.brain.api.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembersByOwner;
import com.sonatype.insight.brain.security.MembersByRole;

/**
 * @since 1.11.0
 */
@Named
public class ApiMemberMappingAdapter
{
  public ApiRoleMemberMappingListDTO convert(final ApplicableMembershipMappings mappings, final String ownerType) {
    final List<ApiRoleMemberMappingDTO> roleMemberMappingDTOs = new ArrayList<>();
    for (final MembersByRole membersByRole : mappings.membersByRole) {
      final ApiRoleMemberMappingDTO roleMemberMappingDTO = new ApiRoleMemberMappingDTO();
      roleMemberMappingDTO.setRoleId(membersByRole.roleId);
      roleMemberMappingDTO.setRoleName(membersByRole.roleName);
      roleMemberMappingDTO.setRoleDescription(membersByRole.roleDescription);
      final List<ApiMemberDTO> memberDTOs = new ArrayList<>();

      for (final MembersByOwner membersByOwner : membersByRole.membersByOwner) {
        if (ownerType.equals(membersByOwner.ownerType)) {
          for (final Member member : membersByOwner.members) {
            final ApiMemberDTO memberDTO = new ApiMemberDTO(member.getInternalName(), member.getType());
            memberDTOs.add(memberDTO);
          }
        }
      }
      roleMemberMappingDTO.setMembers(memberDTOs);
      roleMemberMappingDTOs.add(roleMemberMappingDTO);
    }
    final ApiRoleMemberMappingListDTO memberMappingDTO = new ApiRoleMemberMappingListDTO();
    memberMappingDTO.setMemberMappings(roleMemberMappingDTOs);
    return memberMappingDTO;
  }

  public List<Member> convert(final List<ApiMemberDTO> memberDTOs) {
    final List<Member> memberList = new ArrayList<>();
    if (memberDTOs != null) {
      for (final ApiMemberDTO memberDTO : memberDTOs) {
        final Member member = new Member();
        member.setType(memberDTO.getType());
        member.setInternalName(memberDTO.getUserOrGroupName());
        memberList.add(member);
      }
    }
    return memberList;
  }
}
