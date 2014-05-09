/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.validation.constraints.NotNull;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapGroup;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facade that accesses either the internal CLM user data or LDAP data.
 * 
 * @since 1.11.0
 */
@Named
@Singleton
public class UserDirectory
{
  /**
   * @since 1.11.0
   */
  public static class QueryResult
  {
    private List<Member> members;
    private Exception exception;

    public QueryResult(List<Member> members) {
      this(members, null);
    }

    public QueryResult(List<Member> members, Exception exception) {
      this.members = (members != null) ? members : new ArrayList<Member>();
      this.exception = exception;
    }

    public Exception getException() {
      return exception;
    }

    /**
     * @return The list of members produced by a query, if an exception occurred then this list will only represent
     *         results local to CLM.
     */
    public @NotNull List<Member> get() {
      return members;
    }

    public boolean hasException() {
      return exception != null;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(UserDirectory.class);

  private final UserDAO userDao;

  private final LdapManager ldapManager;

  @Inject
  public UserDirectory(UserDAO userDao, LdapManager ldapManager) {
    this.userDao = userDao;
    this.ldapManager = ldapManager;
  }

  /**
   * @param names The internal names of users that should be returned.
   * @param groupsEnabled True, if LDAP groups should also be searched.
   * @return A query result containing members or exceptions. If an exception is encountered it is still likely that the
   *         result contains local user information.
   */
  public QueryResult getMembersByNames(Set<String> names, boolean groupsEnabled) {
    List<Member> members = new ArrayList<>();
    Set<String> cleanNames = purgeNullUsernames(names);
    if (cleanNames.isEmpty()) {
      return new QueryResult(members);
    }

    List<User> clmUsers = userDao.getByUsernames(cleanNames);
    for (User user : clmUsers) {
      Member member = new Member(MemberType.USER, user.getUsername(), user.calculateDisplayName(), user.getEmail(),
          CLMRealm.DISPLAY_NAME);
      members.add(member);
    }

    if (ldapManager.isLdapEnabled()) {
      try {
        String ldapName = ldapManager.getLdapServerName();

        for (LdapUser user : ldapManager.getUsers(cleanNames.toArray(new String[cleanNames.size()]), cleanNames.size())) {
          Member member = new Member(MemberType.USER, user.getUsername(), user.getRealName(), user.getEmail(), ldapName);
          members.add(member);
        }

        if (groupsEnabled && ldapManager.isLdapGroupEnabled()) {
          for (LdapGroup group : ldapManager.getGroups(cleanNames.toArray(new String[cleanNames.size()]),
              cleanNames.size())) {
            final String groupName = group.getGroupname();
            Member member = new Member(MemberType.GROUP, groupName, groupName, null, ldapName);
            members.add(member);
          }
        }
      }
      catch (Exception e) {
        return new QueryResult(members, e);
      }
    }

    return new QueryResult(members);
  }

  /**
   * @param query The partial name query that searches for users based on first name, last name, or in the case of LDAP
   *          name fragments.
   * @param groupsEnabled True, if LDAP groups should also be searched.
   * @return A query result containing members or exceptions. If an exception is encountered it is still likely that the
   *         result contains local user information.
   */
  public QueryResult getMembersByQuery(String query, boolean groupsEnabled) {
    List<Member> members = new ArrayList<>();

    if (StringUtils.isBlank(query)) {
      return new QueryResult(members);
    }

    // Users are shaded by any user from the CLM domain that has the same user name.
    Map<String, Member> users = new LinkedHashMap<>();
    Map<String, Member> groups = new LinkedHashMap<>();

    for (User user : userDao.findUsersByName(query)) {
      Member member = new Member(MemberType.USER, user.getUsername(), user.calculateDisplayName(), user.getEmail(),
          CLMRealm.DISPLAY_NAME);
      users.put(member.getInternalNameLowerCase(), member);
    }

    Exception ldapException = null;
    if (ldapManager.isLdapEnabled()) {
      try {
        String ldapName = ldapManager.getLdapServerName();

        for (LdapUser user : ldapManager.findUsersByName(query, 100)) {
          Member member = new Member(MemberType.USER, user.getUsername(), user.getRealName(), user.getEmail(), ldapName);
          String key = member.getInternalNameLowerCase();
          // Ignore any user that was already discovered in the CLM realm.
          if (!users.containsKey(key)) {
            users.put(key, member);
          }
        }
        if (groupsEnabled && ldapManager.isLdapGroupEnabled()) {
          for (LdapGroup group : ldapManager.findGroupsByName(query, 100)) {
            final String groupName = group.getGroupname();
            Member member = new Member(MemberType.GROUP, groupName, groupName, null, ldapName);
            groups.put(groupName, member);
          }
        }
      }
      catch (Exception e) {
        ldapException = e;
      }
    }

    members.addAll(users.values());
    members.addAll(groups.values());

    return new QueryResult(members, ldapException);
  }

  public boolean isLdapUser(final User user) throws NamingException {
    if (!ldapManager.isLdapEnabled()) {
      return false;
    }

    String[] userNames = { user.getUsername() };
    List<LdapUser> ldapUsers = ldapManager.getUsers(userNames, userNames.length);
    return !ldapUsers.isEmpty();
  }

  /**
   * Validate the users in the list and return a list of the invalid users.
   * 
   * @param userNames The set of user name to lookup.
   * @return Set of invalid user names.
   */
  public Set<String> validateUsers(final Set<String> userNames) {
    if (userNames == null || userNames.isEmpty()) {
      return Collections.emptySet();
    }

    QueryResult result = getMembersByNames(userNames, false);
    if (result.hasException()) {
      log.error("An exception occurred while trying to resolve user names; validating users against local CLM realm.",
          result.getException());
    }

    Collator caseInsensitiveCollator = Collator.getInstance(Locale.ENGLISH);
    // For the English locale primary strength ignores case.
    caseInsensitiveCollator.setStrength(Collator.PRIMARY);
    // Using a tree set because we want to store items based on comparison, not hash value.
    TreeSet<CollationKey> notFoundCollationKeys = buildCollationKeysTreeSet(userNames, caseInsensitiveCollator);

    for (Member member : result.get()) {
      if (member.getInternalName() != null) {
        notFoundCollationKeys.remove(caseInsensitiveCollator.getCollationKey(member.getInternalName()));
      }
    }

    return toSourceStringSet(notFoundCollationKeys);
  }

  private Set<String> toSourceStringSet(Set<CollationKey> keys) {
    Set<String> sources = new HashSet<>();
    for (CollationKey key : keys) {
      sources.add(key.getSourceString());
    }
    return sources;
  }

  private TreeSet<CollationKey> buildCollationKeysTreeSet(Set<String> strings, Collator collator) {
    TreeSet<CollationKey> collationKeys = new TreeSet<>();
    for (String string : strings) {
      if (string != null) {
        collationKeys.add(collator.getCollationKey(string));
      }
    }
    return collationKeys;
  }

  private Set<String> purgeNullUsernames(Set<String> names) {
    Set<String> result = new LinkedHashSet<>();
    if (names != null) {
      for (String name : names) {
        if (!StringUtils.isBlank(name)) {
          result.add(name);
        }
      }
    }
    return result;
  }
}
