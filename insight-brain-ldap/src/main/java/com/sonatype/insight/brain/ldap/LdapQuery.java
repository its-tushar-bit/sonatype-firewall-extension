/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.AuthenticationException;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
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

  private final LdapCtxFactory ctxFactory;

  private final LdapUserMapping umap;

  public LdapQuery(LdapConnection conn, LdapUserMapping umap) {
    ctxFactory = new LdapCtxFactory();

    ctxFactory.setUrl(conn.getUrl());
    ctxFactory.setAuthenticationMechanism(conn.getAuthenticationMethod().getMethod());
    ctxFactory.setSystemUsername(conn.getSystemUsername());
    ctxFactory.setSystemPassword(conn.getSystemPassword());
    ctxFactory.setSaslRealm(conn.getSaslRealm());
    ctxFactory.setConnectionTimeout(conn.getConnectionTimeout());

    this.umap = umap;
  }

  /**
   * Tests the LDAP connection by performing a basic attributes query.
   */
  public void testConnection() throws NamingException {
    LdapContext ctx = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      ctx.getAttributes(""); // make sure we have enough access
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Authenticates the given user and password against LDAP.
   */
  public LdapUser authenticateUser(String username, char[] password, boolean withMembership) throws NamingException {
    LdapUser ldapUser = getUser(username, withMembership);

    if (StringUtils.isBlank(umap.getUserPasswordAttribute())) {
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
  public void authenticateWithBind(LdapUser user, char[] password) throws NamingException {
    String boundName = user.getDn();
    if (ctxFactory.getAuthenticationMechanism().endsWith("MD5")) {
      boundName = user.getUsername();
    }

    LdapContext ctx = null;
    try {
      ctx = ctxFactory.getLdapContext(boundName, password);
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Authenticates the given user and password by comparing against the credentials from LDAP.
   */
  public void authenticateWithPassword(LdapUser user, char[] password) throws NamingException {
    byte[] receivedCredentials = Strings.getBytesUtf8(password != null ? new String(password) : null);
    byte[] storedCredentials = Strings.getBytesUtf8(user.getPassword());

    if (!PasswordUtil.compareCredentials(receivedCredentials, storedCredentials)) {
      throw new AuthenticationException("LDAP user with username '" + user.getUsername() + "' cannot be authenticated.");
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
        umap.getUserIDAttribute(), //
        umap.getUserRealNameAttribute(), //
        umap.getUserEmailAttribute(), //
        withMembership ? umap.getUserMemberOfGroupAttribute() : null, //
        umap.getUserPasswordAttribute() //
    );

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      results = searchUsersByUsername(ctx, username, attributes, 1);
      if (results.hasMoreElements()) {
        return createUser(ctx, results.nextElement(), withMembership);
      }
      throw new NameNotFoundException("LDAP user with username '" + username + "' does not exist");
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Queries LDAP for all users up to a limited number; result never includes stored credentials.
   * 
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @param withMembership when true include group membership, otherwise don't
   * @return List of LdapUser objects
   * @throws NamingException if there are problems accessing the ldap context
   */
  public List<LdapUser> getUsers(long maxResults, boolean withMembership) throws NamingException {
    String[] attributes = pickAttributes( //
        umap.getUserIDAttribute(), //
        umap.getUserRealNameAttribute(), //
        umap.getUserEmailAttribute(), //
        withMembership ? umap.getUserMemberOfGroupAttribute() : null //
    );

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      results = searchUsersByUsername(ctx, null, attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      while (results.hasMoreElements()) {
        ldapUsers.add(createUser(ctx, results.nextElement(), withMembership));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Queries LDAP for list of users whose UserID attribute matches one of the names provided by the names parameter
   */
  public List<LdapUser> getUsers(String[] names, long maxResults) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserRealNameAttribute(),
        umap.getUserEmailAttribute());
    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      // TODO: query sanitization will be applied with this ticket
      // https://issues.sonatype.org/browse/CLM-1083
      results = searchUsersByUsernames(ctx, names, attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      while (results.hasMoreElements()) {
        ldapUsers.add(createUser(ctx, results.nextElement(), false));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  public List<LdapGroup> getGroups(String[] names, long maxResults) throws NamingException {
    LdapContext ctx = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      // TODO: query sanitization will be applied with this ticket
      // https://issues.sonatype.org/browse/CLM-1083
      return searchGroupsByGroupnames(ctx, names, maxResults);
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Query for list of users whos realname attribute matches the supplied nameFragment. This nameFragment will be prefixed and
   * suffixed with a wildcard to find matches. Group membership is not included in the result.
   * 
   * @param nameFragment String to match against
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapUser objects matching the search criteria
   * @throws NamingException if there are problems accessing the ldap context
   */
  public List<LdapUser> queryUsersByName(String nameFragment, long maxResults) throws NamingException {
    String[] attributes = pickAttributes( //
        umap.getUserIDAttribute(), //
        umap.getUserRealNameAttribute(), //
        umap.getUserEmailAttribute() //
    );

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      // TODO: query sanitization will be applied with this ticket
      // https://issues.sonatype.org/browse/CLM-1083
      results = searchUsersByName(ctx, nameFragment, attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      while (results.hasMoreElements()) {
        ldapUsers.add(createUser(ctx, results.nextElement(), false));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Query for list of groups whose Group ID attribute matches the supplied nameFragment. This nameFragment will be prefixed and
   * suffixed with a wildcard to find matches.
   *
   * @param nameFragment String to match against
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapGroup objects matching the search criteria
   */
  public List<LdapGroup> queryGroupsByName(String nameFragment, long maxResults) throws NamingException {
    LdapContext ctx = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      return searchGroupsByGroupname(ctx, nameFragment, maxResults);
    } finally {
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Populates LDAP user information from the given search result.
   */
  private LdapUser createUser(LdapContext ctx, SearchResult result, boolean withMembership) throws NamingException {
    LdapUser user = new LdapUser();

    user.setDn(result.getNameInNamespace());

    Attributes attributes = result.getAttributes();

    user.setUsername(getAttributeValue(attributes, umap.getUserIDAttribute()));
    user.setPassword(getAttributeValue(attributes, umap.getUserPasswordAttribute()));
    user.setRealName(getAttributeValue(attributes, umap.getUserRealNameAttribute()));
    user.setEmail(getAttributeValue(attributes, umap.getUserEmailAttribute()));

    if (withMembership) {
      Set<String> membership;
      switch (umap.getGroupMappingType()) {
        case DYNAMIC:
          membership = getAttributeValues(attributes, umap.getUserMemberOfGroupAttribute());
          break;
        case STATIC:
          membership = getUserMembership(ctx, user); // search groups using current context
          break;
        default:
          membership = null;
          break;
      }
      user.setMembership(getSimpleNames(membership));
    }

    return user;
  }

  /**
   * Queries LDAP for group membership for the given user.
   */
  private Set<String> getUserMembership(LdapContext ctx, LdapUser user) throws NamingException {
    NamingEnumeration<SearchResult> results = null;
    try {
      String groupIdAttribute = umap.getGroupIDAttribute();
      results = searchGroups(ctx, user, pickAttributes(groupIdAttribute));
      Set<String> membership = new LinkedHashSet<String>();
      while (results.hasMoreElements()) {
        Attributes attributes = results.nextElement().getAttributes();
        membership.addAll(getAttributeValues(attributes, groupIdAttribute));
      }
      return membership;
    }
    finally {
      LdapUtils.closeEnumeration(results);
    }
  }

  private NamingEnumeration<SearchResult> searchUsersByUsernames(LdapContext ctx, String[] names, String[] attributes,
                                                                 long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.putAll(umap.getUserIDAttribute(), Arrays.asList(names));
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  /**
   * Search ldap server for all users whose realname attribute matches the supplied name
   */
  private NamingEnumeration<SearchResult> searchUsersByName(LdapContext ctx, String query, String[] attributes, long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(umap.getUserRealNameAttribute(), query != null ? "*" + query + "*" : "*");
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }
  
  /**
   * Search ldap server for all users whose userid attribute matches the supplied name
   */
  private NamingEnumeration<SearchResult> searchUsersByUsername(LdapContext ctx, String username, String[] attributes,
      long maxResults) throws NamingException
  {
    Multimap<String, String> attributeValues = ArrayListMultimap.create();
    attributeValues.put(umap.getUserIDAttribute(), username != null ? username : "*"); // mandatory username
    return searchUsersByAttributes(ctx, attributeValues, attributes, maxResults);
  }

  /**
   * Search ldap server for all groups whose Group ID attribute equals the supplied names
   */
  private List<LdapGroup> searchGroupsByGroupnames(LdapContext ctx, String[] groupnames, long maxResults)
      throws NamingException
  {
    NamingEnumeration<SearchResult> results = null;
    try {
      switch (umap.getGroupMappingType()) {
        case DYNAMIC: {
          String[] attributes = pickAttributes(umap.getUserMemberOfGroupAttribute());
          Multimap<String, String> attributeValues = ArrayListMultimap.create();
          attributeValues.put(umap.getUserIDAttribute(), "*");

          // Max results is ignored since all users must be returned to deduce unique dynamic groups
          results = searchUsersByAttributes(ctx, attributeValues, attributes, 0);
          return buildGroupsFromDynamicSearchResults(groupnames, results, true, maxResults);
        }
        case STATIC: {
          String[] attributes = pickAttributes(umap.getGroupIDAttribute());
          Multimap<String, String> attributeValues = ArrayListMultimap.create();
          attributeValues.putAll(umap.getGroupIDAttribute(), Arrays.asList(groupnames));

          results = searchGroupsByAttributes(ctx, attributeValues, attributes, maxResults);
          return buildGroupsFromStaticSearchResults(results);
        }
        default: {
          throw new IllegalStateException(String.format("%s group mapping does not exist", umap.getGroupMappingType()));
        }
      }
    }
    finally {
      LdapUtils.closeEnumeration(results);
    }
  }

  /**
   * Search ldap server for all groups whose Group ID attribute matches the supplied name
   */
  private List<LdapGroup> searchGroupsByGroupname(LdapContext ctx, String query, long maxResults)
      throws NamingException
  {
    NamingEnumeration<SearchResult> results = null;
    try {
      switch (umap.getGroupMappingType()) {
        case DYNAMIC: {
          String[] attributes = pickAttributes(umap.getUserMemberOfGroupAttribute());
          Multimap<String, String> attributeValues = ArrayListMultimap.create();
          attributeValues.put(umap.getUserIDAttribute(), "*");
          attributeValues.put(umap.getUserMemberOfGroupAttribute(), query != null ? "*" + query + "*" : "*");

          // Max results is ignored since all users must be returned to deduce unique dynamic groups
          results = searchUsersByAttributes(ctx, attributeValues, attributes, 0);
          return buildGroupsFromDynamicSearchResults(new String[]{query}, results, false, maxResults);
        }
        case STATIC: {
          String[] attributes = pickAttributes(umap.getGroupIDAttribute());
          Multimap<String, String> attributeValues = ArrayListMultimap.create();
          attributeValues.put(umap.getGroupIDAttribute(), query != null ? "*" + query + "*" : "*");

          results = searchGroupsByAttributes(ctx, attributeValues, attributes, maxResults);
          return buildGroupsFromStaticSearchResults(results);
        }
        default: {
          throw new IllegalStateException(String.format("%s group mapping does not exist", umap.getGroupMappingType()));
        }
      }
    }
    finally {
      LdapUtils.closeEnumeration(results);
    }
  }

  /**
   * Builds LdapGroup from a list of user objects who belong to dynamic groups. Uses UserMemberOfGroupAttribute.
   * Results are case insensitively filtered by Strings in the queries array, either through contains or equality
   * depending on the exact parameter.
   *
   * @param exact whether to filter results by substrings in queries or exact strings
   */
  private List<LdapGroup> buildGroupsFromDynamicSearchResults(String[] queries, NamingEnumeration<SearchResult> results,
                                                              boolean exact, long maxResults) throws NamingException
  {
    Map<String, LdapGroup> ldapGroups = new LinkedHashMap<>();
    while (results.hasMoreElements()) {
      SearchResult result = results.nextElement();
      Set<String> groupNames = getSimpleNames(getAttributeValues(result.getAttributes(), umap.getUserMemberOfGroupAttribute()));
      if (groupNames != null) {
        for (String groupName : groupNames) {
          if (groupNameMatches(groupName, queries, exact) && !ldapGroups.containsKey(groupName.toLowerCase(Locale.ENGLISH))) {
            ldapGroups.put(groupName.toLowerCase(Locale.ENGLISH), createGroup(result, groupName));

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
   * Return whether the name parameters exists within the queries array.
   *
   * @param exact whether to filter results by substrings in queries or exact strings
   */
  private static boolean groupNameMatches(String name, String[] queries, boolean exact) {
    for (String query : queries) {
      if (query == null)  {
        return true;
      }
      if (exact) {
        if (StringUtils.equalsIgnoreCase(name, query)) {
          return true;
        }
      } else {
        if (StringUtils.containsIgnoreCase(name, query)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Builds LdapGroup from a list of group objects.
   */
  private List<LdapGroup> buildGroupsFromStaticSearchResults(NamingEnumeration<SearchResult> results)
      throws NamingException
  {
    List<LdapGroup> ldapGroups = new ArrayList<>();
    while (results.hasMoreElements()) {
      SearchResult result = results.nextElement();
      ldapGroups.add(createGroup(result, getAttributeValue(result.getAttributes(), umap.getGroupIDAttribute())));
    }
    return ldapGroups;
  }

  private LdapGroup createGroup(SearchResult result, String groupname) {
    LdapGroup group = new LdapGroup();
    group.setDn(result.getNameInNamespace());
    group.setGroupname(groupname);

    return group;
  }

  /**
   * Search ldap server for all users, based upon the supplied attributes
   * 
   * @param attributeValues list of attribute values that will be passed to ldap to perform the query
   * @param attributes list of attributes that we are requesting ldap to send back to us for each user
   * @param maxResults limit the number of results returned
   */
  private NamingEnumeration<SearchResult> searchUsersByAttributes(LdapContext ctx, Multimap<String, String> attributeValues,
      String[] attributes, long maxResults) throws NamingException
  {
    SearchControls controls = new SearchControls();

    controls.setDerefLinkFlag(true);
    controls.setSearchScope(umap.isUserSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    if (maxResults > 0) {
      controls.setCountLimit(maxResults);
    }

    String baseDN = StringUtils.defaultString(umap.getUserBaseDN());

    StringBuilder ldapFilter = new StringBuilder("(&");

    // select user objects
    ldapFilter.append("(objectClass=").append(umap.getUserObjectClass()).append(')');

    if (attributeValues.size() > 1) {
      ldapFilter.append("(|");
    }
    for (Entry<String, String> entry : attributeValues.entries()) {
      ldapFilter.append('(').append(entry.getKey()).append('=').append(entry.getValue()).append(')');
    }
    if (attributeValues.size() > 1) {
      ldapFilter.append(')');
    }

    // optional user filter
    if (StringUtils.isNotBlank(umap.getUserFilter())) {
      ldapFilter.append('(').append(umap.getUserFilter()).append(')');
    }

    ldapFilter.append(')');

    String ldapFilterString = ldapFilter.toString();
    if(log.isDebugEnabled()){
      log.debug("Executing LdapQuery searchUsersByAttributes with ldapFilter: {}", ldapFilterString);
    }

    return ctx.search(baseDN, ldapFilterString, controls);
  }

  /**
   * Search ldap server for all groups, based upon the supplied attributes
   */
   private NamingEnumeration<SearchResult> searchGroupsByAttributes(LdapContext ctx, Multimap<String, String> attributeValues,
      String[] attributes, long maxResults) throws NamingException
  {
    SearchControls controls = new SearchControls();

    controls.setDerefLinkFlag(true);
    controls.setSearchScope(umap.isGroupSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    if (maxResults > 0) {
      controls.setCountLimit(maxResults);
    }

    String baseDN = StringUtils.defaultString(umap.getGroupBaseDN());

    StringBuilder ldapFilter = new StringBuilder("(&");

    ldapFilter.append("(objectClass=").append(umap.getGroupObjectClass()).append(')');

    if (attributeValues.size() > 1) {
      ldapFilter.append("(|");
    }
    for (Entry<String, String> entry : attributeValues.entries()) {
      ldapFilter.append('(').append(entry.getKey()).append('=').append(entry.getValue()).append(')');
    }
    if (attributeValues.size() > 1) {
      ldapFilter.append(')');
    }

    ldapFilter.append(')');

    String ldapFilterString = ldapFilter.toString();
    if(log.isDebugEnabled()){
      log.debug("Executing LdapQuery searchGroupsByAttributes with ldapFilter: {}", ldapFilterString);
    }

    return ctx.search(baseDN, ldapFilterString, controls);
  }

  /**
   * Searches for LDAP group records that declare the given user as a member.
   */
  private NamingEnumeration<SearchResult> searchGroups(LdapContext ctx, LdapUser user, String[] attributes)
      throws NamingException
  {
    SearchControls controls = new SearchControls();

    controls.setSearchScope(umap.isGroupSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(attributes);

    String baseDN = StringUtils.defaultString(umap.getGroupBaseDN());
    String member = umap.getGroupMemberFormat();
    if (StringUtils.isNotBlank(member)) {
      member = member.replace("${username}", user.getUsername()).replace("${dn}", user.getDn());
    }
    else {
      member = user.getUsername();
    }

    StringBuilder ldapFilter = new StringBuilder("(&");

    // select group objects
    ldapFilter.append("(objectClass=").append(umap.getGroupObjectClass()).append(')');

    // select groupids
    ldapFilter.append('(').append(umap.getGroupIDAttribute()).append("=*)");

    // membership filter
    ldapFilter.append('(').append(umap.getGroupMemberAttribute()).append('=').append(member).append(')');

    ldapFilter.append(')');

    String ldapFilterString = ldapFilter.toString();
    if(log.isDebugEnabled()){
      log.debug("Executing LdapQuery searchGroups with ldapFilter: {}", ldapFilterString);
    }

    return ctx.search(baseDN, ldapFilterString, controls);
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
    List<String> result = new ArrayList<String>(names.length);
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
        Set<String> values = new LinkedHashSet<String>();
        for (NamingEnumeration<?> e = attribute.getAll(); e.hasMoreElements();) {
          values.add(String.valueOf(e.nextElement()));
        }
        return values;
      }
    }
    return null;
  }
}
