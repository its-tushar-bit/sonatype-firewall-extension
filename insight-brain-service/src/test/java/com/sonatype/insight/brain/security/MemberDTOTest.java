/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.MemberDTO.GroupMemberDTO;
import com.sonatype.insight.brain.security.MemberDTO.UserMemberDTO;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class MemberDTOTest
{
  @Test
  public void testTranscribe_UserMember() {
    Member userMember = member(MemberType.USER);
    MemberDTO memberDTO = MemberDTO.transcribe(userMember);

    assertThat(memberDTO, instanceOf(UserMemberDTO.class));
    UserMemberDTO userMemberDTO = (UserMemberDTO) memberDTO;
    assertThat(userMemberDTO.username, is(userMember.getInternalName()));
  }

  @Test
  public void testTranscribe_GroupMember() {
    Member groupMember = member(MemberType.GROUP);
    MemberDTO memberDTO = MemberDTO.transcribe(groupMember);

    assertThat(memberDTO, instanceOf(GroupMemberDTO.class));
    GroupMemberDTO groupMemberDTO = (GroupMemberDTO) memberDTO;
    assertThat(groupMemberDTO.groupName, is(groupMember.getInternalName()));
  }

  @Test
  public void testTranscribe_NoUnknownMemberType() {
    Set<MemberDTO> memberDTOs = MemberDTO
        .transcribe(Stream.of(MemberType.values()).map(this::member).collect(Collectors.toList()));

    assertThat(memberDTOs, hasSize(MemberType.values().length));
  }

  private Member member(MemberType memberType) {
    Member member = new Member();
    member.setInternalName("internalName");
    member.setType(memberType);
    return member;
  }
}
