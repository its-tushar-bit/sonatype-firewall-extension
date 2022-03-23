/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.validation.constraints.NotNull;

import com.sonatype.insight.brain.configuration.ldap.LdapGroup;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facade that accesses either the internal user data or LDAP data.
 * 
 * @since 1.11.0
 */
@Named
@Singleton
public class UserDirectory
{
  public static final char QUERY_WILDCARD = '*';

  private static final char SQL_QUERY_WILDCARD = '%';
  
  private static final String IGNORING_MEMBER_MESSAGE = "Ignoring {} {} from {}, as they were already found in {}.";

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
      this.members = (members != null) ? members : new ArrayList<>();
      this.exception = exception;
    }

    public Exception getException() {
      return exception;
    }

    /**
     * @return The list of members produced by a query. If an exception occurs when querying a realm, it is still
     *         likely that this list contains member information from the other realms.
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

  private final CrowdConfigurationDAO crowdConfigurationDAO;

  private final LdapService ldapService;

  private final CrowdClientFactory crowdClientFactory;

  private final InsightConfig insightConfig;

  @Inject
  public UserDirectory(
      UserDAO userDao,
      CrowdConfigurationDAO crowdConfigurationDAO,
      LdapService ldapService,
      CrowdClientFactory crowdClientFactory,
      InsightConfig insightConfig)
  {
    this.userDao = userDao;
    this.crowdConfigurationDAO = crowdConfigurationDAO;
    this.ldapService = ldapService;
    this.crowdClientFactory = crowdClientFactory;
    this.insightConfig = insightConfig;
  }

  public UserDirectory(UserDAO userDao, LdapService ldapService, CrowdClientFactory crowdClientFactory) {
    this(userDao, null, ldapService, crowdClientFactory, null);
  }

  /**
   * @param members The members that should be populated and returned
   * @return A query result containing members or exceptions. If an exception is encountered it is still likely that the
   *         result contains user/group information.
   */
  public QueryResult getMembersByName(Collection<Member> members) {
    if (members == null || members.isEmpty()) {
      return new QueryResult(new ArrayList<Member>());
    }

    QueryResult result = getUsersByName(getNameByType(members, MemberType.USER));

    Set<String> groupNames = getNameByType(members, MemberType.GROUP);
    purgeNullNames(groupNames);
    for (String groupName : groupNames) {
      if (Group.AUTHENTICATED_USERS_GROUP_ID.equalsIgnoreCase(groupName)) {
        result.get().add(newAuthenticatedUsersGroup());
        break;
      }
    }
    List<NamingException> namingExceptions = new ArrayList<>();
    List<Exception> otherExceptions = new ArrayList<>();
    if (!groupNames.isEmpty()) {
      boolean groupSearchHasBeenDisabled = false;

      for (LdapServer ldapServer : new LdapServerDAO().getAll()) {
        String ldapName = ldapServer.getName();
        if (ldapService.isGroupSearchEnabled(ldapServer)) {
          try {
            for (LdapGroup group : ldapService.getGroupsByName(ldapServer,
                groupNames.toArray(new String[groupNames.size()]))) {
              final String groupName = group.getGroupname();
              Member member = new Member(MemberType.GROUP, groupName, groupName, null, ldapName);
              member.setDn(group.getDn());
              result.get().add(member);
              groupNames.remove(groupName);
            }
          }
          catch (NamingException e) {
            namingExceptions.add(e);
          }
          catch (Exception e) {
            otherExceptions.add(e);
          }
        }
        else {
          groupSearchHasBeenDisabled = true;
        }
      }
      if (groupSearchHasBeenDisabled) {
        for (Member m : purgeMembersNullUsernames(members)) {
          if (MemberType.GROUP.equals(m.getType())) {
            result.get().add(m);
          }
        }
      }
    }

    if (!groupNames.isEmpty()) {
      CrowdClient crowdClient = crowdClientFactory.createCrowdClient();
      if (crowdClient != null) {
        try {
          Set<Member> crowdMembers = crowdClient.searchGroupsByGroupNames(groupNames);
          result.get().addAll(crowdMembers);
          groupNames.removeAll(crowdMembers.stream().map(Member::getInternalName).collect(Collectors.toSet()));
        }
        catch (Exception e) {
          otherExceptions.add(e);
        }
      }
    }

    if (result.hasException()) {
      Exception exception = result.getException();
      if (exception instanceof NamingException) {
        namingExceptions.add((NamingException) exception);
      }
      else {
        otherExceptions.add(exception);
      }
    }

    return new QueryResult(result.members, mergeExceptions(namingExceptions, otherExceptions));
  }

  /**
   * 
   * @param origUserNames the user names to find
   * @return A query result containing members or exceptions. If an exception is encountered it is still likely that the
   *         result contains user information.
   */
  public QueryResult getUsersByName(Set<String> origUserNames) {
    if (origUserNames == null || origUserNames.isEmpty()) {
      return new QueryResult(new ArrayList<Member>());
    }

    List<Member> members = new LinkedList<>();
    Set<String> userNames = new HashSet<>(origUserNames);
    purgeNullNames(userNames);

    if (userNames.isEmpty()) {
      return new QueryResult(members);
    }

    Set<String> sortedUserNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    sortedUserNames.addAll(userNames);
    List<User> internalUsers = userDao.getByUsernames(sortedUserNames);
    for (User internalUser : internalUsers) {
      Member member = new Member(MemberType.USER, internalUser.getUsername(), internalUser.calculateDisplayName(),
          internalUser.getEmail(), InternalRealm.DISPLAY_NAME);
      members.add(member);
      sortedUserNames.remove(internalUser.getUsername());
    }

    List<NamingException> namingExceptions = new ArrayList<>();
    List<Exception> otherExceptions = new ArrayList<>();
    for (LdapServer ldapServer : new LdapServerDAO().getAll()) {
      if (ldapService.isLdapEnabled(ldapServer) && !sortedUserNames.isEmpty()) {
        try {
          String ldapName = ldapServer.getName();

          for (LdapUser user : ldapService
              .getUsersByName(ldapServer, sortedUserNames.toArray(new String[sortedUserNames.size()]))) {
            Member member =
                new Member(MemberType.USER, user.getUsername(), user.getRealName(), user.getEmail(), ldapName);
            member.setDn(user.getDn());
            members.add(member);
            sortedUserNames.remove(user.getUsername());
          }
        }
        catch (NamingException e) {
          namingExceptions.add(e);
        }
        catch (Exception e) {
          otherExceptions.add(e);
        }
      }
    }

    if (!sortedUserNames.isEmpty()) {
      CrowdClient crowdClient = crowdClientFactory.createCrowdClient();
      if (crowdClient != null) {
        try {
          Set<Member> crowdMembers = crowdClient.searchUsersByUsernames(sortedUserNames);
          members.addAll(crowdMembers);
          sortedUserNames.removeAll(crowdMembers.stream().map(Member::getInternalName).collect(Collectors.toSet()));
        }
        catch (Exception e) {
          otherExceptions.add(e);
        }
      }
    }

    return new QueryResult(members, mergeExceptions(namingExceptions, otherExceptions));
  }

  /**
   * @param query The partial name query that searches for users based on first name, last name, or in the case of LDAP,
   *          names.
   * @param groupsEnabled True, if LDAP and/or Crowd groups should also be searched.
   * @return A query result containing members or exceptions. If an exception is encountered it is still likely that the
   *         result contains user/group information.
   */
  public QueryResult getMembersByQuery(String query, boolean groupsEnabled) {
    List<Member> members = new ArrayList<>();

    if (StringUtils.isBlank(query)) {
      return new QueryResult(members);
    }

    // LDAP users are shaded by any user from the internal realm that has the same user name.
    Map<String, Member> users = new LinkedHashMap<>();
    Map<String, Member> groups = new LinkedHashMap<>();

    String internalRealmQuery = query.replace(QUERY_WILDCARD, SQL_QUERY_WILDCARD);
    for (User internalUser : userDao.findUsersByName(internalRealmQuery)) {
      Member member = new Member(MemberType.USER, internalUser.getUsername(), internalUser.calculateDisplayName(),
          internalUser.getEmail(), InternalRealm.DISPLAY_NAME);
      users.put(member.getInternalNameLowerCase(), member);
    }

    List<NamingException> namingExceptions = new ArrayList<>();
    List<Exception> otherExceptions = new ArrayList<>();
    try {
      List<LdapServer> ldapServers = new LdapServerDAO().getAll();
      CrowdClient crowdClient = crowdClientFactory.createCrowdClient();
      // searching for users
      addLDAPUsersByQuery(users, ldapServers, query, namingExceptions, otherExceptions);
      addCrowdUsersByQuery(users, crowdClient, query, otherExceptions);
      // searching for groups
      if (groupsEnabled) {
        addLDAPGroupsByQuery(groups, ldapServers, query, namingExceptions, otherExceptions);
        addCrowdGroupsByQuery(groups, crowdClient, query, otherExceptions);
      }
    }
    catch (Exception e) {
      otherExceptions.add(e);
    }

    if (groupsEnabled) {
      Member group = newAuthenticatedUsersGroup();
      if (group.getDisplayName().matches("(?i)" + Pattern.quote(query).replace(QUERY_WILDCARD + "", "\\E.*\\Q"))) {
        groups.put(group.getInternalNameLowerCase(), group);
      }
    }

    members.addAll(users.values());
    members.addAll(groups.values());

    return new QueryResult(members, mergeExceptions(namingExceptions, otherExceptions));
  }

  private void addLDAPUsersByQuery(
      Map<String, Member> users,
      List<LdapServer> ldapServers,
      String query,
      List<NamingException> namingExceptions,
      List<Exception> otherExceptions)
  {
    for (LdapServer ldapServer : ldapServers) {
      if (ldapService.isLdapEnabled(ldapServer)) {
        try {
          for (LdapUser user : ldapService.findUsersByName(ldapServer, query, 100)) {
            Member member = new Member(MemberType.USER, user.getUsername(), user.getRealName(), user.getEmail(),
                ldapServer.getName());
            member.setDn(user.getDn());
            String key = member.getInternalNameLowerCase();
            // Ignore any user that was already discovered in the other realms.
            if (!users.containsKey(key)) {
              users.put(key, member);
            }
            else {
              log.debug(IGNORING_MEMBER_MESSAGE, "user", key, member.getRealm(), users.get(key).getRealm());
            }
          }
        }
        catch (NamingException e) {
          namingExceptions.add(e);
        }
        catch (Exception e) {
          otherExceptions.add(e);
        }
      }
    }
  }

  private void addCrowdUsersByQuery(
      Map<String, Member> users,
      CrowdClient crowdClient,
      String query,
      List<Exception> otherExceptions)
  {
    if (crowdClient != null) {
      try {
        for (Member crowdMember : crowdClient.searchUsersByDisplayName(query)) {
          String key = crowdMember.getInternalNameLowerCase();
          // Ignore any user that was already discovered in the other realms.
          if (!users.containsKey(key)) {
            users.put(key, crowdMember);
          }
          else {
            log.debug(IGNORING_MEMBER_MESSAGE, "user", key, crowdMember.getRealm(), users.get(key).getRealm());
          }
        }
      }
      catch (Exception e) {
        otherExceptions.add(e);
      }
    }
  }

  private void addLDAPGroupsByQuery(
      Map<String, Member> groups,
      List<LdapServer> ldapServers,
      String query,
      List<NamingException> namingExceptions,
      List<Exception> otherExceptions)
  {
    for (LdapServer ldapServer : ldapServers) {
      try {
        if (ldapService.isGroupSearchEnabled(ldapServer)) {
          for (LdapGroup group : ldapService.findGroupsByName(ldapServer, query, 100)) {
            final String groupName = group.getGroupname();
            Member member = new Member(MemberType.GROUP, groupName, groupName, null, ldapServer.getName());
            member.setDn(group.getDn());
            String key = member.getInternalNameLowerCase();
            // Ignore any group that was already discovered in the other realms.
            if (!groups.containsKey(key)) {
              groups.put(key, member);
            }
            else {
              log.debug(IGNORING_MEMBER_MESSAGE, "group", key, member.getRealm(), groups.get(key).getRealm());
            }
          }
        }
      }
      catch (NamingException e) {
        namingExceptions.add(e);
      }
      catch (Exception e) {
        otherExceptions.add(e);
      }
    }
  }

  private void addCrowdGroupsByQuery(
      Map<String, Member> groups,
      CrowdClient crowdClient,
      String query,
      List<Exception> otherExceptions)
  {
    if (crowdClient != null) {
      try {
        for (Member crowdMember : crowdClient.searchGroupsByGroupNames(Collections.singleton(query))) {
          String key = crowdMember.getInternalNameLowerCase();
          // Ignore any group that was already discovered in the other realms.
          if (!groups.containsKey(key)) {
            groups.put(key, crowdMember);
          }
          else {
            log.debug(IGNORING_MEMBER_MESSAGE, "group", key, crowdMember.getRealm(), groups.get(key).getRealm());
          }
        }
      }
      catch (Exception e) {
        otherExceptions.add(e);
      }
    }
  }

  private Exception mergeExceptions(List<NamingException> namingExceptions, List<Exception> otherExceptions) {
    Exception result;
    if (!otherExceptions.isEmpty()) {
      result = new Exception();
    }
    else if (!namingExceptions.isEmpty()) {
      result = new NamingException();
    }
    else {
      return null;
    }

    for (NamingException e : namingExceptions) {
      result.addSuppressed(e);
    }
    for (Exception e : otherExceptions) {
      result.addSuppressed(e);
    }
    return result;
  }

  private Member newAuthenticatedUsersGroup() {
    return new Member(MemberType.GROUP, Group.AUTHENTICATED_USERS_GROUP_ID,
        Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME, null, InternalRealm.DISPLAY_NAME);
  }

  public boolean isGroupSearchDisabled() {
    return ldapService.isDynamicGroupSearchDisabled() &&
        (!insightConfig.isExperimentalFeatureEnabled(ExperimentalFeature.CROWD_INTEGRATION) ||
            crowdConfigurationDAO.get() == null);
  }

  public boolean isLdapUser(final User user) throws NamingException {
    String[] userNames = { user.getUsername() };
    for (LdapServer ldapServer : new LdapServerDAO().getAll()) {
      if (ldapService.isLdapEnabled(ldapServer) && !ldapService.getUsersByName(ldapServer, userNames).isEmpty()) {
        return true;
      }
    }
    return false;
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

    QueryResult result = getUsersByName(userNames);
    if (result.hasException()) {
      log.error(
          "An exception occurred while trying to resolve user names; validating users against local IQ Server realm.",
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

  private static Set<String> getNameByType(Iterable<Member> members, MemberType type) {
    Set<String> names = new LinkedHashSet<>();
    if (members != null) {
      for (Member member : members) {
        if (type.equals(member.getType())) {
          names.add(member.getInternalName());
        }
      }
    }
    return names;
  }

  private static Iterable<Member> purgeMembersNullUsernames(Iterable<Member> members) {
    if (members != null) {
      for (Iterator<Member> iter = members.iterator(); iter.hasNext();) {
        Member member = iter.next();
        if (StringUtils.isBlank(member.getInternalName())) {
          iter.remove();
        }
      }
    }
    return members;
  }

  private static void purgeNullNames(Iterable<String> userNames) {
    for (Iterator<String> iter = userNames.iterator(); iter.hasNext();) {
      String member = iter.next();
      if (StringUtils.isBlank(member)) {
        iter.remove();
      }
    }
  }
}
