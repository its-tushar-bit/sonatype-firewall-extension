/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.OwnerType;
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
  public ApiRoleMemberMappingListDTO convert(ApplicableMembershipMappings mappings) {
    return convert(mappings, null);
  }

  public ApiRoleMemberMappingListDTO convert(final ApplicableMembershipMappings mappings, final OwnerType ownerType) {
    final List<ApiRoleMemberMappingDTO> roleMemberMappingDTOs = new ArrayList<>();
    for (final MembersByRole membersByRole : mappings.membersByRole) {
      final ApiRoleMemberMappingDTO roleMemberMappingDTO = new ApiRoleMemberMappingDTO();
      roleMemberMappingDTO.roleId = membersByRole.roleId;
      final List<ApiMemberDTO> memberDTOs = new ArrayList<>();

      for (final MembersByOwner membersByOwner : membersByRole.membersByOwner) {
        if (ownerType == null || ownerType.equals(membersByOwner.ownerType)) {
          for (final Member member : membersByOwner.members) {
            final ApiMemberDTO memberDTO = new ApiMemberDTO(member.getInternalName(), member.getType());
            memberDTOs.add(memberDTO);
          }
        }
      }
      roleMemberMappingDTO.members = memberDTOs;
      roleMemberMappingDTOs.add(roleMemberMappingDTO);
    }
    final ApiRoleMemberMappingListDTO memberMappingDTO = new ApiRoleMemberMappingListDTO();
    memberMappingDTO.memberMappings = roleMemberMappingDTOs;
    return memberMappingDTO;
  }

  public Map<String, List<Member>> convert(final ApiRoleMemberMappingListDTO memberMappingDTOs) {
    Map<String, List<Member>> roleToMembers = new LinkedHashMap<>();
    for (ApiRoleMemberMappingDTO memberMappingDTO : memberMappingDTOs.memberMappings) {
      roleToMembers.put(memberMappingDTO.roleId, convert(memberMappingDTO.members));
    }
    return roleToMembers;
  }

  private List<Member> convert(final List<ApiMemberDTO> memberDTOs) {
    final List<Member> memberList = new ArrayList<>();
    if (memberDTOs != null) {
      for (final ApiMemberDTO memberDTO : memberDTOs) {
        final Member member = new Member();
        member.setType(memberDTO.type);
        member.setInternalName(memberDTO.userOrGroupName);
        memberList.add(member);
      }
    }
    return memberList;
  }
}
