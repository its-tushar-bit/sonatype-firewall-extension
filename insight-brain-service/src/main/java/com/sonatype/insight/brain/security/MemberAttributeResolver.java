/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.security.MemberType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves Member attributes given a type and internalName
 *
 * @since 1.7
 */
public class MemberAttributeResolver
{
  private final Map<MemberKey, Member> resolvedMembers = new HashMap<>();

  private static final Logger log = LoggerFactory.getLogger(MemberAttributeResolver.class);

  private final UserDirectory userDirectory;

  public MemberAttributeResolver(UserDirectory userDirectory) {
    this.userDirectory = userDirectory;
  }

  public void resolve(List<Member> members) {
    List<Member> unresolvedMembers = resolveCachedMembers(members);

    // Get new members based on internal user names.
    UserDirectory.QueryResult result = userDirectory.getMembersByNames(unresolvedMembers);
    if (result.hasException()) {
      log.error("An exception occurred while trying to resolve user names; " +
          "attempting to resolve user names using the local Nexus IQ realm.",
          result.getException());
    }

    // Add new members to the resolved members cache.
    addResolvedMembers(result.get());
    // Resolve remaining members using the updated cache.
    unresolvedMembers = resolveCachedMembers(unresolvedMembers);

    // Use the user name for the display name for the remaining unresolved members.
    for (Member unresolvedMember : unresolvedMembers) {
      unresolvedMember.setDisplayName(unresolvedMember.getInternalName());
      resolvedMembers.put(new MemberKey(unresolvedMember.getInternalName(), unresolvedMember.getType()),
          unresolvedMember);
    }
  }

  private void addResolvedMembers(Collection<Member> members) {
    for (Member member : members) {
      MemberKey key = new MemberKey(member.getInternalName(), member.getType());
      if (!resolvedMembers.containsKey(key)) {
        resolvedMembers.put(key, member);
      }
    }
  }

  /**
   * @param members The partial members to completely resolve.
   * @return Unresolved members, not found in the local cache.
   */
  private List<Member> resolveCachedMembers(Collection<Member> members) {
    List<Member> unresolvedMembers = new ArrayList<>(members);

    for (Member member : members) {
      MemberKey key = new MemberKey(member.getInternalName(), member.getType());
      Member existingMember = resolvedMembers.get(key);

      if (existingMember != null) {
        member.setDisplayName(existingMember.getDisplayName());
        member.setEmail(existingMember.getEmail());
        member.setRealm(existingMember.getRealm());
        member.setDn(existingMember.getDn());
        unresolvedMembers.remove(member);
      }
    }

    return unresolvedMembers;
  }

  private static class MemberKey
  {
    private final String name;

    private final MemberType type;

    public MemberKey(final String name, final MemberType type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MemberKey)) {
        return false;
      }
      MemberKey key = (MemberKey) o;
      return key.name.equals(name) && key.type.equals(type);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + type.hashCode();
    }
  }
}
