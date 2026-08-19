/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides various LDAP queries.
 *
 * @since 1.7
 */
class LdapQuery
{
  private static final Logger log = LoggerFactory.getLogger(LdapQuery.class);

  // Visible for testing
  static final String LDAP_MATCHING_RULE_IN_CHAIN = "1.2.840.113556.1.4.1941";

  private static final String LDAP_MATCHING_RULE_IN_CHAIN_SUFFIX = ":" + LDAP_MATCHING_RULE_IN_CHAIN + ":";

  private static interface StringMatcher
  {
    boolean matches(String str, String searchStr);
  }

  private static final StringMatcher EQUALS = new StringMatcher()
  {
    @Override
    public boolean matches(String str, String searchStr) {
      return StringUtils.equalsIgnoreCase(str, searchStr);
    }
  };

  private static final StringMatcher CONTAINS = new StringMatcher()
  {
    @Override
    public boolean matches(String str, String searchStr) {
      return StringUtils.containsIgnoreCase(str, searchStr);
    }
  };

  private static final StringMatcher STARTS_WITH = new StringMatcher()
  {
    @Override
    public boolean matches(String str, String searchStr) {
      return StringUtils.startsWithIgnoreCase(str, searchStr);
    }
  };

  private static final StringMatcher ENDS_WITH = new StringMatcher()
  {
    @Override
    public boolean matches(String str, String searchStr) {
      return StringUtils.endsWithIgnoreCase(str, searchStr);
    }
  };

  private static final Set<String> serversWithoutPaging = ConcurrentHashMap.newKeySet();

  private final LdapCtxFactory ctxFactory;

  private final LdapUserMapping ldapUserMapping;

  public LdapQuery(LdapConnection ldapConnection, LdapUserMapping ldapUserMapping) {
    ctxFactory = new LdapCtxFactory();

    ctxFactory.setUrl(ldapConnection.getUrl());
    ctxFactory.setAuthenticationMechanism(ldapConnection.getAuthenticationMethod().getMethod());
    ctxFactory.setSystemUsername(ldapConnection.getSystemUsername());
    ctxFactory.setSystemPassword(ldapConnection.getSystemPassword());
    ctxFactory.setSaslRealm(ldapConnection.getSaslRealm());
    ctxFactory.setConnectionTimeout(ldapConnection.getConnectionTimeout());
    ctxFactory.setReferral(ldapConnection.isReferralIgnored() ? "ignore" : "follow");

    this.ldapUserMapping = ldapUserMapping;
  }

  private LdapContextHolder getSystemLdapContext() throws NamingException {
    return new LdapContextHolder(ctxFactory.getSystemLdapContext());
  }

  /**
   * Tests the LDAP connection by performing a basic attributes query.
   */
  public void testConnection() throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      ctxHolder.ctx.getAttributes(""); // make sure we have enough access
    }
  }

  /**
   * Authenticates the given user and password against LDAP.
   */
  public LdapUser authenticateUser(String username, char[] password, boolean withMembership) throws NamingException {
    // Verify non-empty password
    // Per RFC 4513, section 5.1.2, clients should disallow an empty password input
    // to a Name/Password Authentication user interface
    if (password == null || password.length == 0) {
      throw new AuthenticationException("Password must not be empty");
    }

    LdapUser ldapUser = getUser(username, withMembership);

    if (StringUtils.isBlank(ldapUserMapping.getUserPasswordAttribute())) {
      authenticateWithBind(ldapUser, password);
    }
    else {
      authenticateWithPassword(ldapUser, password);
    }

    return ldapUser;
  }

  /**
   * Authenticates the given user and password by delegating to the LDAP server via bind.
   */
  private void authenticateWithBind(LdapUser user, char[] password) throws NamingException {
    String boundName = user.getDn();
    if (ctxFactory.getAuthenticationMechanism().endsWith("MD5")) {
      boundName = user.getUsername();
    }

    LdapUtils.closeContext(ctxFactory.getLdapContext(boundName, password));
  }

  /**
   * Authenticates the given user and password by comparing against the credentials from LDAP.
   */
  private void authenticateWithPassword(LdapUser user, char[] password) throws NamingException {
    byte[] receivedCredentials = Strings.getBytesUtf8(password != null ? new String(password) : null);
    byte[] storedCredentials = Strings.getBytesUtf8(user.getPassword());

    if (!PasswordUtil.compareCredentials(receivedCredentials, storedCredentials)) {
      throw new AuthenticationException(
          "LDAP user with username '" + user.getUsername() + "' cannot be authenticated.");
    }
  }

  /**
   * Queries LDAP for a specific user; includes stored credentials when password attribute is set.
   *
   * @param username The username to lookup
   * @param withMembership when true include group membership, otherwise don't
   * @return LdapUser object for the given username
   * @throws NameNotFoundException if the username doesn't exist in ldap
   * @throws NamingException if there are problems accessing the ldap context
   */
  public LdapUser getUser(String username, boolean withMembership) throws NamingException {
    String[] attributes = pickAttributes( //
        ldapUserMapping.getUserIDAttribute(), //
        ldapUserMapping.getUserRealNameAttribute(), //
        ldapUserMapping.getUserEmailAttribute(), //
        withMembership ? ldapUserMapping.getUserMemberOfGroupAttribute() : null, //
        ldapUserMapping.getUserPasswordAttribute() //
    );

    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      String[] usernames = {notNull(username)};
      try (SearchResults results = searchUsersByUsernames(ctxHolder.ctx, usernames, attributes, 1)) {
        if (results.hasMoreElements()) {
          SearchResult result = results.nextElement();
          return createUser(ctxHolder.ctx, result.getNameInNamespace(), result.getAttributes(), withMembership);
        }
      }
      throw new NameNotFoundException("LDAP user with username '" + username + "' does not exist");
    }
  }

  /**
   * Queries LDAP for all users up to a limited number; result never includes stored credentials but does include group
   * memberships.
   *
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapUser objects
   * @throws NamingException if there are problems accessing the ldap context
   */
  public List<LdapUser> getUsers(long maxResults) throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      return findUsersByName(ctxHolder.ctx, "*", maxResults, true);
    }
  }

  /**
   * Queries LDAP for list of users whose UserID attribute matches one of the names provided by the names parameter
   */
  public List<LdapUser> getUsersByName(String[] usernames) throws NamingException {
    String[] attributes = pickAttributes(ldapUserMapping.getUserIDAttribute(),
        ldapUserMapping.getUserRealNameAttribute(), ldapUserMapping.getUserEmailAttribute());
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      try (SearchResults results = searchUsersByUsernames(ctxHolder.ctx, usernames, attributes, 0)) {
        return getUsersFromResults(ctxHolder, results);
      }
    }
  }

  /**
   * Search ldap server for all users whose "real name" (full name) attribute matches one of the supplied names,
   * don't allow asterisk, exact match only
   */
  public List<LdapUser> getUsersByRealName(String[] realNames) throws NamingException {
    final String[] attributes = pickAttributes(ldapUserMapping.getUserIDAttribute(),
        ldapUserMapping.getUserRealNameAttribute(), ldapUserMapping.getUserEmailAttribute());

    try (final LdapContextHolder ctxHolder = getSystemLdapContext()) {
      try (final SearchResults results = searchUsersByRealName(ctxHolder.ctx, realNames, attributes, 0)) {
        return getUsersFromResults(ctxHolder, results);
      }
    }
  }

  private SearchResults searchUsersByRealName(
      LdapContext ctx,
      String[] realNames,
      String[] attributes,
      long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.putAll(escapeAttribute(ldapUserMapping.getUserRealNameAttribute(), false),
        escapeAttributes(realNames, false));
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  public List<LdapUser> getUsersByEmail(String[] emails) throws NamingException {
    final String[] attributes = pickAttributes(ldapUserMapping.getUserIDAttribute(),
        ldapUserMapping.getUserRealNameAttribute(), ldapUserMapping.getUserEmailAttribute());

    try (final LdapContextHolder ctxHolder = getSystemLdapContext()) {
      try (final SearchResults results = searchUsersByEmail(ctxHolder.ctx, emails, attributes, 0)) {
        return getUsersFromResults(ctxHolder, results);
      }
    }
  }

  private SearchResults searchUsersByEmail(
      LdapContext ctx,
      String[] emails,
      String[] attributes,
      long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.putAll(escapeAttribute(ldapUserMapping.getUserEmailAttribute(), false),
        escapeAttributes(emails, false));
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  public List<LdapGroup> getGroupsByName(String[] groupNames) throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      switch (ldapUserMapping.getGroupMappingType()) {
        case DYNAMIC:
          return getDynamicGroupsByNames(ctxHolder.ctx, groupNames);
        case STATIC:
          return getStaticGroupsByNames(ctxHolder.ctx, groupNames);
        default:
          throw newUnknownGroupMappingTypeException();
      }
    }
  }

  /**
   * Query for list of users whose realname attribute matches the supplied name. Group membership is not included in the
   * result.
   *
   * @param query String to match against
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapUser objects matching the search criteria
   * @throws NamingException if there are problems accessing the ldap context
   */
  public List<LdapUser> findUsersByName(String query, long maxResults) throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      return findUsersByName(ctxHolder.ctx, query, maxResults, false);
    }
  }

  private List<LdapUser> findUsersByName(
      LdapContext ctx,
      String query,
      long maxResults,
      boolean withMembership) throws NamingException
  {
    String[] attributes = pickAttributes( //
        ldapUserMapping.getUserIDAttribute(), //
        ldapUserMapping.getUserRealNameAttribute(), //
        ldapUserMapping.getUserEmailAttribute(), //
        withMembership ? ldapUserMapping.getUserMemberOfGroupAttribute() : null //
    );

    try (SearchResults results = searchUsersByRealName(ctx, notNull(query), attributes, maxResults)) {
      List<LdapUser> ldapUsers = new ArrayList<>();
      while (results.hasMoreElements()) {
        SearchResult result = results.nextElement();
        ldapUsers.add(createUser(ctx, result.getNameInNamespace(), result.getAttributes(), withMembership));
      }
      return ldapUsers;
    }
  }

  /**
   * Query for list of groups whose Group ID attribute matches the supplied name.
   *
   * @param query String to match against
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapGroup objects matching the search criteria
   */
  public List<LdapGroup> findGroupsByName(String query, long maxResults) throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      switch (ldapUserMapping.getGroupMappingType()) {
        case DYNAMIC:
          return findDynamicGroupsByName(ctxHolder.ctx, notNull(query), maxResults);
        case STATIC:
          return findStaticGroupsByName(ctxHolder.ctx, notNull(query), maxResults);
        default:
          throw newUnknownGroupMappingTypeException();
      }
    }
  }

  /**
   * Populates LDAP user information from the given search result.
   */
  private LdapUser createUser(
      LdapContext ctx,
      String dn,
      Attributes attributes,
      boolean withMembership) throws NamingException
  {
    LdapUser user = new LdapUser();

    user.setDn(dn);
    user.setServerId(ldapUserMapping.getServerId());
    user.setUsername(getAttributeValue(attributes, ldapUserMapping.getUserIDAttribute()));
    user.setPassword(getAttributeValue(attributes, ldapUserMapping.getUserPasswordAttribute()));
    user.setRealName(getAttributeValue(attributes, ldapUserMapping.getUserRealNameAttribute()));
    user.setEmail(getAttributeValue(attributes, ldapUserMapping.getUserEmailAttribute()));

    if (withMembership) {
      user.setMembership(getGroupMemberships(ctx, user, attributes));
    }

    return user;
  }

  /**
   * Returns group memberships for a given user or, for dynamic groupings, a given set of attributes.
   */
  private Set<String> getGroupMemberships(
      LdapContext ctx,
      LdapUser user,
      Attributes attributes) throws NamingException
  {
    Set<String> memberships;
    switch (ldapUserMapping.getGroupMappingType()) {
      case DYNAMIC:
        memberships = getAttributeValues(attributes, ldapUserMapping.getUserMemberOfGroupAttribute());
        break;
      case STATIC:
        memberships = getUserMembership(ctx, user); // search groups using current context
        break;
      default:
        memberships = null;
        break;
    }
    return getSimpleNames(memberships);
  }

  /**
   * Queries LDAP for group membership for the given user.
   */
  private Set<String> getUserMembership(LdapContext ctx, LdapUser user) throws NamingException {
    String groupIdAttribute = ldapUserMapping.getGroupIDAttribute();
    try (SearchResults results = searchGroups(ctx, user, pickAttributes(groupIdAttribute))) {
      Set<String> membership = new LinkedHashSet<>();
      while (results.hasMoreElements()) {
        Attributes attributes = results.nextElement().getAttributes();
        membership.addAll(getAttributeValues(attributes, groupIdAttribute));
      }
      return membership;
    }
  }

  private SearchResults searchUsersByUsernames(
      LdapContext ctx,
      String[] usernames,
      String[] attributes,
      long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.putAll(escapeAttribute(ldapUserMapping.getUserIDAttribute(), false),
        escapeAttributes(usernames, false));
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  /**
   * Search ldap server for all users whose realname attribute matches the supplied name
   */
  private SearchResults searchUsersByRealName(
      LdapContext ctx,
      String query,
      String[] attributes,
      long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getUserRealNameAttribute(), false),
        escapeAttribute(query, true));
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  private List<LdapGroup> getDynamicGroupsByNames(LdapContext ctx, String[] groupNames) throws NamingException {
    String[] attributes = pickAttributes(ldapUserMapping.getUserMemberOfGroupAttribute());
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getUserIDAttribute(), false), "*");

    try (SearchResults results = searchUsersByAttributes(ctx, attributeValues, attributes, 0)) {
      return buildGroupsFromDynamicSearchResults(groupNames, results, EQUALS, 0);
    }
  }

  private List<LdapGroup> getStaticGroupsByNames(LdapContext ctx, String[] groupNames) throws NamingException {
    String[] attributes = pickAttributes(ldapUserMapping.getGroupIDAttribute());
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.putAll(escapeAttribute(ldapUserMapping.getGroupIDAttribute(), false),
        escapeAttributes(groupNames, false));

    try (SearchResults results = searchGroupsByAttributes(ctx, attributeValues, attributes, 0)) {
      return buildGroupsFromStaticSearchResults(results);
    }
  }

  private List<LdapGroup> findDynamicGroupsByName(
      LdapContext ctx,
      String query,
      long maxResults) throws NamingException
  {
    String[] attributes = pickAttributes(ldapUserMapping.getUserMemberOfGroupAttribute());
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getUserIDAttribute(), false), "*");

    // Max results is ignored since all users must be returned to deduce unique dynamic groups
    try (SearchResults results = searchUsersByAttributes(ctx, attributeValues, attributes, 0)) {
      StringMatcher stringMatcher;
      if (query.startsWith("*")) {
        if (query.endsWith("*")) {
          stringMatcher = CONTAINS;
        }
        else {
          stringMatcher = ENDS_WITH;
        }
        query = query.replace("*", "");
      }
      else if (query.endsWith("*")) {
        stringMatcher = STARTS_WITH;
        query = query.replace("*", "");
      }
      else {
        stringMatcher = EQUALS;
      }
      return buildGroupsFromDynamicSearchResults(new String[]{query}, results, stringMatcher, maxResults);
    }
  }

  private List<LdapGroup> findStaticGroupsByName(
      LdapContext ctx,
      String query,
      long maxResults) throws NamingException
  {
    String[] attributes = pickAttributes(ldapUserMapping.getGroupIDAttribute());
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getGroupIDAttribute(), false), escapeAttribute(query, true));

    try (SearchResults results = searchGroupsByAttributes(ctx, attributeValues, attributes, maxResults)) {
      return buildGroupsFromStaticSearchResults(results);
    }
  }

  /**
   * Builds LdapGroup from a list of user objects who belong to dynamic groups. Uses UserMemberOfGroupAttribute.
   * Results are case insensitively filtered by Strings in the queries array, using the specified StringMatcher.
   */
  private List<LdapGroup> buildGroupsFromDynamicSearchResults(
      String[] queries,
      NamingEnumeration<SearchResult> results,
      StringMatcher stringmatcher,
      long maxResults) throws NamingException
  {
    Map<String, LdapGroup> ldapGroups = new LinkedHashMap<>();
    while (results.hasMoreElements()) {
      SearchResult result = results.nextElement();
      Set<String> groupDns =
          getAttributeValues(result.getAttributes(), ldapUserMapping.getUserMemberOfGroupAttribute());
      if (groupDns != null) {
        for (String groupDn : groupDns) {
          String groupName = getSimpleName(groupDn);
          if (groupNameMatches(groupName, queries, stringmatcher)
              && !ldapGroups.containsKey(groupName.toLowerCase(Locale.ENGLISH)))
          {
            ldapGroups.put(groupName.toLowerCase(Locale.ENGLISH), createGroup(groupDn, groupName));

            if (ldapGroups.size() == maxResults) {
              return new ArrayList<>(ldapGroups.values());
            }
          }
        }
      }
    }
    return new ArrayList<>(ldapGroups.values());
  }

  /**
   * Return whether the name parameter exists within the queries array.
   */
  private static boolean groupNameMatches(String name, String[] queries, StringMatcher stringMatcher) {
    for (String query : queries) {
      if (query == null) {
        return true;
      }
      if (stringMatcher.matches(name, query)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builds LdapGroup from a list of group objects.
   */
  private List<LdapGroup> buildGroupsFromStaticSearchResults(
      NamingEnumeration<SearchResult> results) throws NamingException
  {
    List<LdapGroup> ldapGroups = new ArrayList<>();
    while (results.hasMoreElements()) {
      SearchResult result = results.nextElement();
      ldapGroups
          .add(createGroup(result, getAttributeValue(result.getAttributes(), ldapUserMapping.getGroupIDAttribute())));
    }
    return ldapGroups;
  }

  private LdapGroup createGroup(SearchResult result, String groupName) {
    LdapGroup group = new LdapGroup();
    group.setDn(result.getNameInNamespace());
    group.setGroupname(groupName);
    return group;
  }

  private LdapGroup createGroup(String groupDn, String groupName) {
    LdapGroup group = new LdapGroup();
    group.setDn(groupDn);
    group.setGroupname(groupName);
    return group;
  }

  /**
   * Search ldap server for all users, based upon the supplied attributes
   *
   * @param attributeValues list of attribute values that will be passed to ldap to perform the query
   * @param attributes list of attributes that we are requesting ldap to send back to us for each user
   * @param maxResults limit the number of results returned. Unlimited if <= 0.
   */
  private SearchResults searchUsersByAttributes(
      LdapContext ctx,
      Multimap<String, String> attributeValues,
      String[] attributes,
      long maxResults) throws NamingException
  {
    SearchControls controls = new SearchControls();

    controls.setDerefLinkFlag(true);
    controls
        .setSearchScope(ldapUserMapping.isUserSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    if (maxResults > 0) {
      controls.setCountLimit(maxResults);
    }

    StringBuilder ldapFilter = new StringBuilder("(&");
    // select user objects
    ldapFilter.append("(objectClass=").append(escapeAttribute(ldapUserMapping.getUserObjectClass(), false)).append(')');
    appendAttributeValues(ldapFilter, attributeValues);
    // optional user filter
    if (StringUtils.isNotBlank(ldapUserMapping.getUserFilter())) {
      ldapFilter.append('(').append(ldapUserMapping.getUserFilter()).append(')');
    }
    ldapFilter.append(')');

    return search(ctx, ldapUserMapping.getUserBaseDN(), ldapFilter.toString(), controls);
  }

  /**
   * Search ldap server for all groups, based upon the supplied attributes
   */
  private SearchResults searchGroupsByAttributes(
      LdapContext ctx,
      Multimap<String, String> attributeValues,
      String[] attributes,
      long maxResults) throws NamingException
  {
    SearchControls controls = new SearchControls();

    controls.setDerefLinkFlag(true);
    controls.setSearchScope(
        ldapUserMapping.isGroupSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    if (maxResults > 0) {
      controls.setCountLimit(maxResults);
    }

    StringBuilder ldapFilter = new StringBuilder("(&");
    ldapFilter.append("(objectClass=")
        .append(escapeAttribute(ldapUserMapping.getGroupObjectClass(), false))
        .append(')');
    appendAttributeValues(ldapFilter, attributeValues);
    ldapFilter.append(')');

    return search(ctx, ldapUserMapping.getGroupBaseDN(), ldapFilter.toString(), controls);
  }

  private void appendAttributeValues(StringBuilder ldapFilter, Multimap<String, String> attributeValues) {
    if (attributeValues.size() > 1) {
      ldapFilter.append("(|");
    }
    for (Entry<String, String> entry : attributeValues.entries()) {
      ldapFilter.append('(').append(entry.getKey()).append('=').append(entry.getValue()).append(')');
    }
    if (attributeValues.size() > 1) {
      ldapFilter.append(')');
    }
  }

  /**
   * Searches for LDAP group records that declare the given user as a member.
   */
  private SearchResults searchGroups(LdapContext ctx, LdapUser user, String[] attributes) throws NamingException {
    SearchControls controls = new SearchControls();

    controls.setSearchScope(
        ldapUserMapping.isGroupSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    String member = escapeAttribute(ldapUserMapping.getGroupMemberFormat(), true);
    if (StringUtils.isNotBlank(member)) {
      member = member.replace("${username}", escapeAttribute(user.getUsername(), false))
          .replace("${dn}",
              escapeAttribute(user.getDn(), false));
    }
    else {
      member = escapeAttribute(user.getUsername(), false);
    }

    StringBuilder ldapFilter = new StringBuilder("(&");

    // select group objects
    ldapFilter.append("(objectClass=")
        .append(escapeAttribute(ldapUserMapping.getGroupObjectClass(), false))
        .append(')');

    // select groupids
    ldapFilter.append('(').append(escapeAttribute(ldapUserMapping.getGroupIDAttribute(), false)).append("=*)");

    // membership filter
    ldapFilter.append('(')
        .append(escapeAttribute(ldapUserMapping.getGroupMemberAttribute(), false))
        .append('=')
        .append(member)
        .append(')');

    ldapFilter.append(')');

    return search(ctx, ldapUserMapping.getGroupBaseDN(), ldapFilter.toString(), controls);
  }

  SearchResults search(LdapContext ctx, String baseDN, String filter, SearchControls controls) throws NamingException {
    baseDN = StringUtils.defaultString(baseDN);
    log.debug("Executing LDAP query with filter {} in context {} with scope {} and limit {}", filter, baseDN,
        controls.getSearchScope(), controls.getCountLimit());
    NamingEnumeration<SearchResult> results;
    if (controls.getCountLimit() > 0) {
      results = ctx.search(baseDN, filter, controls);
    }
    else {
      // when asking for all results, use paged search to overcome server-side result limits (usually 1000)
      try {
        results = new PagedNamingEnumeration(ctx, baseDN, filter, controls, 100);
      }
      catch (NoPermissionException e) {
        boolean notYetLogged = serversWithoutPaging.add(ctxFactory.getUrl());
        log.warn("Paged search not allowed by LDAP server, {}, falling back to non-paged search", e.getMessage(),
            notYetLogged ? e : null);
        results = ctx.search(baseDN, filter, controls);
      }
    }
    return new SearchResults(results);
  }

  /**
   * Queries LDAP for list of users who are members of the specified group.
   *
   * @since 1.14.0
   */
  public List<LdapUser> getUsersByGroup(String groupDn) throws NamingException {
    try (LdapContextHolder ctxHolder = getSystemLdapContext()) {
      switch (ldapUserMapping.getGroupMappingType()) {
        case DYNAMIC:
          return getUsersByDynamicGroup(ctxHolder.ctx, groupDn);
        case STATIC:
          return getUsersByStaticGroup(ctxHolder.ctx, groupDn);
        default:
          throw newUnknownGroupMappingTypeException();
      }
    }
  }

  private List<LdapUser> getUsersByStaticGroup(LdapContext ctx, String groupDn) throws NamingException {
    String groupMemberAttribute = ldapUserMapping.getGroupMemberAttribute();
    if (groupMemberAttribute.endsWith(LDAP_MATCHING_RULE_IN_CHAIN_SUFFIX)) {
      groupMemberAttribute =
          groupMemberAttribute.substring(0, groupMemberAttribute.indexOf(LDAP_MATCHING_RULE_IN_CHAIN_SUFFIX));
    }
    String[] attributes =
        pickAttributes(ldapUserMapping.getGroupIDAttribute(), groupMemberAttribute);
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getGroupIDAttribute(), false),
        escapeAttribute(getSimpleName(groupDn), false));

    List<LdapUser> users = new ArrayList<>();
    try (SearchResults results = searchGroupsByAttributes(ctx, attributeValues, attributes, 0)) {
      while (results.hasMoreElements()) {
        SearchResult result = results.next();
        Set<String> members = getAttributeValues(result.getAttributes(), groupMemberAttribute);
        if (members != null && !members.isEmpty()) {
          users.addAll(getUsersFromGroupMembers(ctx, ldapUserMapping.getGroupMemberFormat(), members));
        }
      }
    }
    return users;
  }

  private List<LdapUser> getUsersByDynamicGroup(LdapContext ctx, String groupDn) throws NamingException {
    String[] attributes = pickAttributes(ldapUserMapping.getUserIDAttribute(),
        ldapUserMapping.getUserRealNameAttribute(), ldapUserMapping.getUserEmailAttribute());
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(escapeAttribute(ldapUserMapping.getUserMemberOfGroupAttribute(), false),
        escapeAttribute(groupDn, false));

    List<LdapUser> users = new ArrayList<>();
    try (SearchResults results = searchUsersByAttributes(ctx, attributeValues, attributes, 0)) {
      while (results.hasMoreElements()) {
        SearchResult result = results.nextElement();
        users.add(createUser(ctx, result.getNameInNamespace(), result.getAttributes(), false /* withMembership */));
      }
    }
    return users;
  }

  private List<LdapUser> getUsersFromGroupMembers(
      LdapContext ctx,
      String memberFormat,
      Set<String> members) throws NamingException
  {
    if (StringUtils.isBlank(memberFormat)) {
      log.debug("Member format is null or blank, unable to look up LDAP users.");
    }
    else if (memberFormat.equals("${username}")) {
      return getUsersByName(members.toArray(new String[members.size()]));
    }
    else if (memberFormat.equals("${dn}")) {
      return lookupUsersByDn(ctx, members);
    }
    else if (memberFormat.contains("${username}")) {
      // Only need to capture the first instance of username.
      String usernameRegex = memberFormat.replaceFirst("\\$\\{username\\}", "(.*)")
          .replaceAll("\\$\\{username\\}", ".*")
          .replaceAll("\\$\\{dn\\}", ".*");
      Pattern usernamePattern = Pattern.compile(usernameRegex, Pattern.CASE_INSENSITIVE);
      List<String> usernames = parseGroupMembers(usernamePattern, members);
      return getUsersByName(usernames.toArray(new String[usernames.size()]));
    }
    else if (memberFormat.contains("${dn}")) {
      String dnRegex = memberFormat.replaceFirst("\\$\\{dn\\}", "(.*)").replaceAll("\\$\\{dn\\}", ".*");
      Pattern dnPattern = Pattern.compile(dnRegex, Pattern.CASE_INSENSITIVE);
      List<String> dns = parseGroupMembers(dnPattern, members);
      return lookupUsersByDn(ctx, dns);
    }
    else {
      log.debug("No expression found in member format, unable to look up LDAP users.");
    }
    return Collections.emptyList();
  }

  private List<LdapUser> lookupUsersByDn(LdapContext ctx, Collection<String> dns) throws NamingException {
    List<LdapUser> result = new ArrayList<>(dns.size());
    for (String dn : dns) {
      dn = removeContextDn(ctx.getNameInNamespace(), dn);
      try {
        LdapContext member = (LdapContext) ctx.lookup(dn);
        LdapUser user = createUser(ctx, member.getNameInNamespace(), member.getAttributes(""), false);
        result.add(user);
      }
      catch (NameNotFoundException e) {
        log.debug("Unable to find user with DN {}.", dn, e);
      }
    }
    return result;
  }

  private static List<String> parseGroupMembers(Pattern memberPattern, Collection<String> members) {
    List<String> usernames = new ArrayList<>(members.size());

    for (String member : members) {
      if (!StringUtils.isBlank(member)) {
        Matcher memberMatcher = memberPattern.matcher(member);
        if (memberMatcher.find()) {
          // Group 0 represents the entire string, so we want just the first group which would be the username or dn.
          usernames.add(memberMatcher.group(1));
        }
      }
    }

    return usernames;
  }

  /**
   * Remove context DN from given DN so lookups can be performed directly against the current context.
   */
  private static String removeContextDn(String contextDn, String dn) {
    try {
      LdapName dnName = new LdapName(dn);
      LdapName contextDnName = new LdapName(contextDn);
      // remove contextDn part if Dn's hierarchy starts with it.
      if (dnName.startsWith(contextDnName)) {
        return new LdapName(dnName.getRdns().subList(contextDnName.size(), dnName.size())).toString();
      }
    }
    catch (InvalidNameException e) {
      log.debug("Invalid DN found when matching context-DN against search DN. contextDN={}, DN={}", contextDn, dn, e);
    }
    return dn;
  }

  // note i have this method here simply to save from having to type the fully qualified LdapUtils classname all over
  // the place
  private String escapeAttribute(String attribute, boolean allowAsterisk) {
    return com.sonatype.insight.brain.model.configuration.ldap.LdapUtils.escapeLdapQueryAttribute(attribute,
        allowAsterisk);
  }

  private List<String> escapeAttributes(String[] attributes, boolean allowAsterisk) {
    List<String> escapedValues = new ArrayList<>(attributes.length);
    for (String attribute : attributes) {
      escapedValues.add(escapeAttribute(attribute, allowAsterisk));
    }
    return escapedValues;
  }

  /**
   * Attempts to convert the given list of distinguished names to a list of simple names.
   */
  private static Set<String> getSimpleNames(Set<String> names) {
    if (names == null || names.isEmpty()) {
      return names; // no conversion required
    }

    // Membership set needs to be case insensitive since group names are case insensitive
    Set<String> simpleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (String n : names) {
      simpleNames.add(getSimpleName(n));
    }
    return simpleNames;
  }

  /**
   * Attempts to convert the given distinguished name (cn=users,ou=group) to a simple name (users).
   */
  private static String getSimpleName(String name) {
    try {
      LdapName dn = new LdapName(name);
      return String.valueOf(dn.getRdn(dn.size() - 1).getValue());
    }
    catch (InvalidNameException e) {
      return name; // not parsable as a DN; use original string
    }
  }

  /**
   * Returns the given sequence of names with any null/empty elements removed.
   */
  private static String[] pickAttributes(String... names) {
    List<String> result = new ArrayList<>(names.length);
    for (String n : names) {
      if (StringUtils.isNotBlank(n)) {
        result.add(n);
      }
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the value under the given attribute name; null if the named attribute doesn't exist.
   */
  private static String getAttributeValue(Attributes attributes, String name) throws NamingException {
    if (name != null) {
      Attribute attribute = attributes.get(name);
      if (attribute != null) {
        Object value = attribute.get();
        if (value instanceof byte[]) {
          return Strings.utf8ToString((byte[]) value);
        }
        if (value != null) {
          return String.valueOf(value);
        }
      }
    }
    return null;
  }

  /**
   * Returns all values under the given attribute name; null if the named attribute doesn't exist.
   */
  private static Set<String> getAttributeValues(Attributes attributes, String name) throws NamingException {
    if (name != null) {
      Attribute attribute = attributes.get(name);
      if (attribute != null) {
        Set<String> values = new LinkedHashSet<>();
        NamingEnumeration<?> en = attribute.getAll();
        try {
          while (en.hasMoreElements()) {
            values.add(String.valueOf(en.nextElement()));
          }
        }
        finally {
          LdapUtils.closeEnumeration(en);
        }
        return values;
      }
    }
    return null;
  }

  private static String notNull(String value) {
    return value != null ? value : "";
  }

  private IllegalStateException newUnknownGroupMappingTypeException() {
    return new IllegalStateException("Unknown group mapping " + ldapUserMapping.getGroupMappingType());
  }

  private List<LdapUser> getUsersFromResults(
      final LdapContextHolder ctxHolder,
      final SearchResults results) throws NamingException
  {
    final List<LdapUser> ldapUsers = new ArrayList<>();

    while (results.hasMoreElements()) {
      final SearchResult result = results.nextElement();
      ldapUsers.add(createUser(ctxHolder.ctx, result.getNameInNamespace(), result.getAttributes(), false));
    }

    return ldapUsers;
  }
}
