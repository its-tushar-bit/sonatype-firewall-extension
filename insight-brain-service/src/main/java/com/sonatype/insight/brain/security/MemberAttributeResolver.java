/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapGroup;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.MembershipMappingResource.Member;

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

  private final UserDAO userDAO = new UserDAO();

  @SuppressWarnings("hiding")
  private static final Logger log = LoggerFactory.getLogger(MemberAttributeResolver.class);

  private final LdapManager ldapManager;

  public MemberAttributeResolver(final LdapManager ldapManager) {
    this.ldapManager = ldapManager;
  }

  public void resolve(List<Member> members) {
    HashMap<MemberKey, Member> unresolvedMembers = new HashMap<>();

    // First check already resolved members
    for (Member member : members) {
      String internalName = member.internalName;
      MemberKey key = new MemberKey(member.internalName, member.type);
      Member existingMember = resolvedMembers.get(key);

      // Then check if user is in the CLM Realm using UserDAO
      if (existingMember == null) {
        User user = null;
        if (key.type.equals(MemberType.USER)) {
          user = userDAO.getByUsernameLowercase(internalName.toLowerCase(Locale.ENGLISH));
        }
        if (user != null) {
          member.displayName = user.getFirstName() + " " + user.getLastName();
          member.email = user.getEmail();
          member.realm = CLMRealm.DISPLAY_NAME;

          resolvedMembers.put(key, member);
        }
        else {
          unresolvedMembers.put(key, member);
        }
      } else {
        member.displayName = existingMember.displayName;
        member.email = existingMember.email;
        member.realm = existingMember.realm;
      }
    }

    // Resolution is complete if there are no unresolved members
    if (unresolvedMembers.isEmpty()) {
      return;
    }

    // If LDAP is enabled, try to resolve the RealName and Email from LDAP
    if (ldapManager.isLdapEnabled()) {
      try {
        String ldapServerName = ldapManager.getLdapServerName();

        HashMap<String, Member> unresolvedUsers = new HashMap<>();
        HashMap<String, Member> unresolvedGroups = new HashMap<>();
        for (Member unresolvedMember : unresolvedMembers.values()) {
          if (unresolvedMember.type.equals(MemberType.USER)) {
            unresolvedUsers.put(unresolvedMember.internalName, unresolvedMember);
          } else {
            unresolvedGroups.put(unresolvedMember.internalName, unresolvedMember);
          }
        }

        // Only search for users if there are unresolved users
        if (!unresolvedUsers.isEmpty()) {
          List<LdapUser> ldapUsers = ldapManager.getUsers(unresolvedUsers.keySet().toArray(new String[0]), unresolvedMembers.keySet().size());
          for (LdapUser ldapUser : ldapUsers) {
            final String userName = ldapUser.getUsername();

            MemberKey memberKey = new MemberKey(userName, MemberType.USER);
            Member member = unresolvedMembers.get(memberKey);
            member.displayName = ldapUser.getRealName();
            member.email = ldapUser.getEmail();
            member.realm = ldapServerName;

            resolvedMembers.put(memberKey, member);
            unresolvedMembers.remove(memberKey);
          }
        }

        // Only search for groups if there are unresolved groups
        if (!unresolvedGroups.isEmpty()) {
          List<LdapGroup> ldapGroups = ldapManager.getGroups(unresolvedGroups.keySet().toArray(new String[0]), unresolvedGroups.keySet().size());
          for (LdapGroup ldapGroup : ldapGroups) {
            final String groupName = ldapGroup.getGroupname();

            MemberKey memberKey = new MemberKey(groupName, MemberType.GROUP);
            Member member = unresolvedMembers.get(memberKey);
            member.displayName = groupName;
            member.realm = ldapServerName;

            resolvedMembers.put(memberKey, member);
            unresolvedMembers.remove(memberKey);
          }
        }
      }
      catch (NamingException ex) {
        log.error("LDAP exception when trying to resolve user names", ex);
      }
    }

    // Use the unresolved names as the display names for anything still unresolved
    for (MemberKey unresolvedMember : unresolvedMembers.keySet()) {
      Member member = unresolvedMembers.get(unresolvedMember);
      member.displayName = unresolvedMember.name;

      resolvedMembers.put(unresolvedMember, member);
    }
  }

  private static class MemberKey {
    private final String name;
    private final MemberType type;

    public MemberKey(final String username, final MemberType type) {
      this.name = username;
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
      MemberKey key = (MemberKey)o;
      return key.name.equals(name) && key.type.equals(type);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + type.hashCode();
    }
  }
}