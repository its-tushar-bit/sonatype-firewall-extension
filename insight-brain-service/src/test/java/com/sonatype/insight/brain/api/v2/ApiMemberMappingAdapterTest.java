/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembersByOwner;
import com.sonatype.insight.brain.security.MembersByRole;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiMemberMappingAdapterTest
    extends AbstractComponentTest
{
  private final String roleId = "testRoleId";

  private final String testUserName = "testUserName";

  private final MemberType memberType = MemberType.USER;

  @Inject
  private ApiMemberMappingAdapter apiMemberMappingAdapter;

  @Test
  public void testConvertToDTOForApplication() {
    final OwnerType ownerType = OwnerType.APPLICATION;
    final ApplicableMembershipMappings mappings = createApplicableMembershipMappings(ownerType);

    final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO =
        apiMemberMappingAdapter.convert(mappings, ownerType);

    assertRoleMemberMappingDTO(apiRoleMemberMappingListDTO);
  }

  @Test
  public void testConvertToDTOForOrganization() {
    final OwnerType ownerType = OwnerType.ORGANIZATION;
    final ApplicableMembershipMappings mappings = createApplicableMembershipMappings(ownerType);

    final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO =
        apiMemberMappingAdapter.convert(mappings, ownerType);

    assertRoleMemberMappingDTO(apiRoleMemberMappingListDTO);
  }

  @Test
  public void testConvertRoleMemberMappingListToRoleMemberMap() {
    ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = new ApiRoleMemberMappingListDTO();
    apiRoleMemberMappingListDTO.memberMappings = new ArrayList<>();
    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = new ApiRoleMemberMappingDTO();
    apiRoleMemberMappingDTO.roleId = roleId;
    apiRoleMemberMappingDTO.members = new ArrayList<>();
    final ApiMemberDTO memberDTO = new ApiMemberDTO();
    memberDTO.type = memberType;
    memberDTO.userOrGroupName = testUserName;
    apiRoleMemberMappingDTO.members.add(memberDTO);
    apiRoleMemberMappingListDTO.memberMappings.add(apiRoleMemberMappingDTO);

    Map<String, List<Member>> roleToMembers = ApiMemberMappingAdapter.convert(apiRoleMemberMappingListDTO);
    assertRoleToMemberMap(roleToMembers);
  }

  private void assertRoleToMemberMap(Map<String, List<Member>> roleToMembers) {
    assertThat(roleToMembers).hasSize(1);
    List<Member> members = roleToMembers.get(roleId);
    assertThat(members).hasSize(1);
    Member member = members.get(0);
    assertThat(member.getType()).isEqualTo(memberType);
    assertThat(member.getInternalName()).isEqualTo(testUserName);
  }

  private void assertRoleMemberMappingDTO(final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO) {
    assertThat(apiRoleMemberMappingListDTO).isNotNull();
    assertThat(apiRoleMemberMappingListDTO.memberMappings).hasSize(1);
    final ApiRoleMemberMappingDTO roleMemberMappingDTO = apiRoleMemberMappingListDTO.memberMappings.get(0);
    assertThat(roleMemberMappingDTO.roleId).isEqualTo(roleId);
    assertThat(roleMemberMappingDTO.members).hasSize(1);
    final ApiMemberDTO memberDTO = roleMemberMappingDTO.members.get(0);
    assertThat(memberDTO).isNotNull();
    assertThat(memberDTO.type).isEqualTo(memberType);
    assertThat(memberDTO.userOrGroupName).isEqualTo(testUserName);
  }

  private ApplicableMembershipMappings createApplicableMembershipMappings(final OwnerType ownerType) {
    final ApplicableMembershipMappings mappings = new ApplicableMembershipMappings();
    mappings.membersByRole = new ArrayList<>();
    final MembersByRole membersByRole = new MembersByRole();
    membersByRole.roleId = roleId;
    membersByRole.membersByOwner = new ArrayList<>();
    final MembersByOwner membersByOwner = new MembersByOwner();
    switch (ownerType) {
      case APPLICATION:
        Application app = tempEntity.newApplicationWithParent();
        membersByOwner.ownerId = app.getPublicId();
        membersByOwner.ownerName = app.getName();
        break;
      case ORGANIZATION:
        Organization org = tempEntity.newOrganization();
        membersByOwner.ownerId = org.getPublicId();
        membersByOwner.ownerName = org.getName();
        break;
      default:
        throw new IllegalArgumentException("Unexpected owner type " + ownerType);
    }
    membersByOwner.ownerType = ownerType;
    final Member member = new Member();
    member.setInternalName(testUserName);
    member.setType(memberType);
    membersByOwner.members = new ArrayList<>();
    membersByOwner.members.add(member);
    membersByRole.membersByOwner.add(membersByOwner);
    mappings.membersByRole.add(membersByRole);

    return mappings;
  }
}
