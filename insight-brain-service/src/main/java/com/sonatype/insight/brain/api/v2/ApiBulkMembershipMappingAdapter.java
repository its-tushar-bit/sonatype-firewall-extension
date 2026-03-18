/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicableMembershipMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberWithDetailsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMembersByOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleWithMembersByOwnerDTO;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembersByOwner;
import com.sonatype.insight.brain.security.MembersByRole;

/**
 * Adapter to convert between internal membership mapping structures and public API DTOs
 * for bulk operations.
 *
 * @since 1.197.0
 */
@Named
@Singleton
public class ApiBulkMembershipMappingAdapter
{
  public ApiApplicableMembershipMappingsDTO toApiDTO(final ApplicableMembershipMappings internal) {
    return new ApiApplicableMembershipMappingsDTO(
        internal.membersByRole.stream()
            .map(this::toApiRoleWithMembersByOwnerDTO)
            .toList(),
        internal.groupSearchEnabled);
  }

  private ApiRoleWithMembersByOwnerDTO toApiRoleWithMembersByOwnerDTO(final MembersByRole internal) {
    return new ApiRoleWithMembersByOwnerDTO(
        internal.roleId,
        internal.roleName,
        internal.roleDescription,
        internal.membersByOwner.stream()
            .map(this::toApiMembersByOwnerDTO)
            .toList());
  }

  private ApiMembersByOwnerDTO toApiMembersByOwnerDTO(final MembersByOwner internal) {
    return new ApiMembersByOwnerDTO(
        internal.ownerId,
        internal.ownerName,
        internal.ownerType != null ? internal.ownerType.name() : null,
        internal.members.stream()
            .map(this::toApiMemberWithDetailsDTO)
            .toList());
  }

  private ApiMemberWithDetailsDTO toApiMemberWithDetailsDTO(final Member internal) {
    return new ApiMemberWithDetailsDTO(
        internal.getType(),
        internal.getInternalName(),
        internal.getDisplayName(),
        internal.getEmail(),
        internal.getRealm());
  }

  public List<Member> toInternalMembers(final List<ApiMemberWithDetailsDTO> apiMembers) {
    if (apiMembers == null) {
      return new ArrayList<>();
    }
    return apiMembers.stream()
        .map(this::toInternalMember)
        .toList();
  }

  private Member toInternalMember(final ApiMemberWithDetailsDTO apiMember) {
    Member member = new Member();
    member.setType(apiMember.type());
    member.setInternalName(apiMember.internalName());
    member.setDisplayName(apiMember.displayName());
    member.setEmail(apiMember.email());
    member.setRealm(apiMember.realm());
    return member;
  }
}
