/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides various LDAP queries.
 * 
 * @since 1.7
 */
class LdapQuery
{
  private final LdapCtxFactory ctxFactory;

  private final LdapUserMapping umap;

  public LdapQuery(LdapConnection conn, LdapUserMapping umap) {
    ctxFactory = new LdapCtxFactory();

    ctxFactory.setUrl(conn.getUrl());
    ctxFactory.setAuthenticationMechanism(conn.getAuthenticationMethod().getMethod());
    ctxFactory.setSystemUsername(conn.getSystemUsername());
    ctxFactory.setSystemPassword(conn.getSystemPassword());
    ctxFactory.setSaslRealm(conn.getSaslRealm());

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
  public void authenticateUser(String username, char[] password) throws NamingException {
    LdapUser ldapUser = getUser(username);

    if (StringUtils.isBlank(umap.getUserPasswordAttribute())) {
      authenticateWithBind(ldapUser, password);
    }
    else {
      authenticateWithPassword(ldapUser, password);
    }
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
   */
  public LdapUser getUser(String username) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserPasswordAttribute(),
        umap.getUserRealNameAttribute(), umap.getUserEmailAttribute(), umap.getUserMemberOfGroupAttribute());

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      results = searchUsersByUsername(ctx, username, attributes, 1);
      if (results.hasMoreElements()) {
        return createUser(ctx, results.nextElement());
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
   */
  public List<LdapUser> getUsers(long maxResults) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserRealNameAttribute(),
        umap.getUserEmailAttribute(), umap.getUserMemberOfGroupAttribute());

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      results = searchUsersByUsername(ctx, null, attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      while (results.hasMoreElements()) {
        ldapUsers.add(createUser(ctx, results.nextElement()));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Query for list of users whos realname attribute matches the supplied nameFragment. This nameFragment will be prefixed and
   * suffixed with a wildcard to find matches.
   * 
   * @param nameFragment String to match against
   * @param maxResults maximum number of results to pull from ldap, don't want to overload the system
   * @return List of LdapUser objects matching the search criteria
   * @throws NamingException if there are problems accessing the ldap context
   */
  public List<LdapUser> queryUsersByName(String nameFragment, long maxResults) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserRealNameAttribute(),
        umap.getUserEmailAttribute());

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> results = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      // TODO: query sanitization will be applied with this ticket
      // https://issues.sonatype.org/browse/CLM-1083
      results = searchUsersByName(ctx, "*" + nameFragment + "*", attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      while (results.hasMoreElements()) {
        ldapUsers.add(createUser(ctx, results.nextElement()));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(results);
      LdapUtils.closeContext(ctx);
    }
  }

  /**
   * Populates LDAP user information from the given search result.
   */
  private LdapUser createUser(LdapContext ctx, SearchResult result) throws NamingException {
    LdapUser user = new LdapUser();

    user.setDn(result.getNameInNamespace());

    Attributes attributes = result.getAttributes();

    user.setUsername(getAttributeValue(attributes, umap.getUserIDAttribute()));
    user.setPassword(getAttributeValue(attributes, umap.getUserPasswordAttribute()));
    user.setRealName(getAttributeValue(attributes, umap.getUserRealNameAttribute()));
    user.setEmail(getAttributeValue(attributes, umap.getUserEmailAttribute()));

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
  
  /**
   * Search ldap server for all users whose realname attribute matches the supplied name
   */
  private NamingEnumeration<SearchResult> searchUsersByName(LdapContext ctx, String name, String[] attributes, long maxResults) throws NamingException
  {
    return searchUsersByAttributes(ctx, Collections.singletonMap(umap.getUserRealNameAttribute(), name != null ? name : "*"), attributes, maxResults);
  }
  
  /**
   * Search ldap server for all users whose userid attribute matches the supplied name
   */
  private NamingEnumeration<SearchResult> searchUsersByUsername(LdapContext ctx, String username, String[] attributes,
      long maxResults) throws NamingException
  {
    // mandatory username
    return searchUsersByAttributes(ctx, Collections.singletonMap(umap.getUserIDAttribute(), username != null ? username : "*"),
        attributes, maxResults);
  }

  /**
   * Search ldap server for all users, based upon the supplied attributes
   * 
   * @param ctx
   * @param attributeValues list of attribute values that will be passed to ldap to perform the query
   * @param attributes list of attributes that we are requesting ldap to send back to us for each user
   * @param maxResults limit the number of results returned
   * @return
   * @throws NamingException
   */
  private NamingEnumeration<SearchResult> searchUsersByAttributes(LdapContext ctx, Map<String, String> attributeValues,
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
      ldapFilter.append('(').append('|');
    }
    for (Entry<String, String> entry : attributeValues.entrySet()) {
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

    return ctx.search(baseDN, ldapFilter.toString(), controls);
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

    return ctx.search(baseDN, ldapFilter.toString(), controls);
  }

  /**
   * Attempts to convert the given list of distinguished names to a list of simple names.
   */
  private static Set<String> getSimpleNames(Set<String> names) {
    if (names == null || names.isEmpty()) {
      return names; // no conversion required
    }
    Set<String> simpleNames = new LinkedHashSet<String>(names.size());
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
   * Returns the given sequence of names with any null elements removed.
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
