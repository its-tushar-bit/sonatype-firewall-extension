/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.security.MemberType;

import com.google.common.annotations.VisibleForTesting;

public abstract class MemberDTO
{
  @VisibleForTesting
  static final class UserMemberDTO
      extends MemberDTO
  {
    public String username;

    private UserMemberDTO(Member member) {
      username = member.getInternalName();
    }
  }

  @VisibleForTesting
  static final class GroupMemberDTO
      extends MemberDTO
  {
    public String groupName;

    private GroupMemberDTO(Member member) {
      groupName = member.getInternalName();
    }
  }

  public static Set<MemberDTO> transcribe(Collection<Member> members) {
    return members.stream().map(MemberDTO::transcribe).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @VisibleForTesting
  static MemberDTO transcribe(Member member) {
    MemberType memberType = member.getType();
    switch (memberType) {
      case USER:
        return new UserMemberDTO(member);
      case GROUP:
        return new GroupMemberDTO(member);
      default:
        throw new IllegalArgumentException("Type of role member " + memberType + " is unexpected.");
    }
  }
}
