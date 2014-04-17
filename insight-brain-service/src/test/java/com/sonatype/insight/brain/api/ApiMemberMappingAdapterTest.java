/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembersByOwner;
import com.sonatype.insight.brain.security.MembersByRole;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApiMemberMappingAdapterTest
{

  private ApiMemberMappingAdapter apiMemberMappingAdapter = new ApiMemberMappingAdapter();

  private final String roleId = "testRoleId";

  private final String roleName = "testRoleName";

  private final String roleDescription = "testRoleDescription";

  private final String ownerId = "ownerId";

  private final String ownerName = "ownerName";

  private final String testUserName = "testUserName";

  private final MemberType memberType = MemberType.USER;

  @Test
  public void testConvertToDTOForApplication() {
    final String ownerType = IdUtils.TYPE_APPLICATION;
    final ApplicableMembershipMappings mappings = createApplicableMembershipMappings(ownerType);

    final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = apiMemberMappingAdapter.convert(mappings,
        ownerType);

    assertRoleMemberMappingDTO(apiRoleMemberMappingListDTO);
  }

  @Test
  public void testConvertToDTOForOrganization() {
    final String ownerType = IdUtils.TYPE_ORGANIZATION;
    final ApplicableMembershipMappings mappings = createApplicableMembershipMappings(ownerType);

    final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = apiMemberMappingAdapter.convert(mappings,
        ownerType);

    assertRoleMemberMappingDTO(apiRoleMemberMappingListDTO);
  }

  @Test
  public void testConvertMemberDTOToEntity() {
    final List<ApiMemberDTO> memberDTOs = new ArrayList<>();
    final ApiMemberDTO memberDTO = new ApiMemberDTO();
    memberDTO.type = memberType;
    memberDTO.userOrGroupName = testUserName;
    memberDTOs.add(memberDTO);

    final List<Member> members = apiMemberMappingAdapter.convert(memberDTOs);
    assertThat(members, hasSize(1));
    final Member member = members.get(0);
    assertThat(member.getType(), is(memberType));
    assertThat(member.getInternalName(), is(testUserName));
  }

  private void assertRoleMemberMappingDTO(final ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    assertThat(apiRoleMemberMappingListDTO, notNullValue());
    assertThat(apiRoleMemberMappingListDTO.memberMappings, hasSize(1));
    final ApiRoleMemberMappingDTO roleMemberMappingDTO = apiRoleMemberMappingListDTO.memberMappings.get(0);
    assertThat(roleMemberMappingDTO.roleId, is(roleId));
    assertThat(roleMemberMappingDTO.roleName, is(roleName));
    assertThat(roleMemberMappingDTO.roleDescription, is(roleDescription));
    assertThat(roleMemberMappingDTO.members, hasSize(1));
    final ApiMemberDTO memberDTO = roleMemberMappingDTO.members.get(0);
    assertThat(memberDTO, notNullValue());
    assertThat(memberDTO.type, is(memberType));
    assertThat(memberDTO.userOrGroupName, is(testUserName));
  }

  private ApplicableMembershipMappings createApplicableMembershipMappings(final String ownerType) {
    final ApplicableMembershipMappings mappings = new ApplicableMembershipMappings();
    mappings.membersByRole = new ArrayList<>();
    final MembersByRole membersByRole = new MembersByRole();
    membersByRole.roleId = roleId;
    membersByRole.roleName = roleName;
    membersByRole.roleDescription = roleDescription;
    membersByRole.membersByOwner = new ArrayList<>();
    final MembersByOwner membersByOwner = new MembersByOwner();
    membersByOwner.ownerId = ownerId;
    membersByOwner.ownerName = ownerName;
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
