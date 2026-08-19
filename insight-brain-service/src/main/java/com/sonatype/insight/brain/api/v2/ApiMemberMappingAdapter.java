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
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
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
  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiMemberMappingAdapter(final ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  public ApiRoleMemberMappingListDTO convert(ApplicableMembershipMappings mappings) {
    return convert(mappings, null);
  }

  ApiRoleMemberMappingListDTO convert(
      final ApplicableMembershipMappings mappings,
      final OwnerType ownerType)
  {
    final List<ApiRoleMemberMappingDTO> roleMemberMappingDTOs = new ArrayList<>();
    for (final MembersByRole membersByRole : mappings.membersByRole) {
      final ApiRoleMemberMappingDTO roleMemberMappingDTO = new ApiRoleMemberMappingDTO();
      roleMemberMappingDTO.roleId = membersByRole.roleId;
      final List<ApiMemberDTO> memberDTOs = new ArrayList<>();

      for (final MembersByOwner membersByOwner : membersByRole.membersByOwner) {
        if (ownerType == null || ownerType.equals(membersByOwner.ownerType)) {
          for (final Member member : membersByOwner.members) {
            // If the owner is an application, then MembersByOwner stores the app public id in ownerId.
            // We need to convert it to internal app id when converting to ApiMemberDTO.
            String internalOwnerId;
            if (OwnerType.APPLICATION.equals(membersByOwner.ownerType)) {
              internalOwnerId = applicationDAO.getByPublicId(membersByOwner.ownerId).getId();
            }
            else {
              internalOwnerId = membersByOwner.ownerId;
            }

            final ApiMemberDTO memberDTO = new ApiMemberDTO(internalOwnerId, membersByOwner.ownerType.name(),
                member.getInternalName(), member.getType());
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

  public static Map<String, List<Member>> convert(final ApiRoleMemberMappingListDTO memberMappingDTOs) {
    Map<String, List<Member>> roleToMembers = new LinkedHashMap<>();
    for (ApiRoleMemberMappingDTO memberMappingDTO : memberMappingDTOs.memberMappings) {
      roleToMembers.put(memberMappingDTO.roleId, convert(memberMappingDTO.members));
    }
    return roleToMembers;
  }

  private static List<Member> convert(final List<ApiMemberDTO> memberDTOs) {
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
