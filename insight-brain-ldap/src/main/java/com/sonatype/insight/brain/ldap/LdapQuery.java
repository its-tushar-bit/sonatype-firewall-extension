/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

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

  public void testConnection() throws NamingException {
    LdapContext ctx = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

  public void authenticateUser(String username, char[] password) throws NamingException {
    LdapUser ldapUser = getUser(username);

    if (StringUtils.isBlank(umap.getUserPasswordAttribute())) {
      authenticateWithBind(ldapUser, password);
    }
    else {
      authenticateWithPassword(ldapUser, password);
    }
  }

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

  public void authenticateWithPassword(LdapUser user, char[] password) throws NamingException {
    byte[] receivedCredentials = Strings.getBytesUtf8(password != null ? new String(password) : null);
    byte[] storedCredentials = Strings.getBytesUtf8(user.getPassword());

    if (!PasswordUtil.compareCredentials(receivedCredentials, storedCredentials)) {
      throw new AuthenticationException("LDAP user with username '" + user.getUsername() + "' cannot be authenticated.");
    }
  }

  public LdapUser getUser(String username) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserPasswordAttribute(),
        umap.getUserRealNameAttribute(), umap.getUserEmailAttribute());

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> result = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      result = searchUsers(ctx, username, attributes, 1);
      if (result.hasMoreElements()) {
        return createUser(result.nextElement());
      }
      throw new NameNotFoundException("LDAP user with username '" + username + "' does not exist");
    }
    finally {
      LdapUtils.closeEnumeration(result);
      LdapUtils.closeContext(ctx);
    }
  }

  public List<LdapUser> getUsers(long maxResults) throws NamingException {
    String[] attributes = pickAttributes(umap.getUserIDAttribute(), umap.getUserRealNameAttribute(),
        umap.getUserEmailAttribute());

    LdapContext ctx = null;
    NamingEnumeration<SearchResult> result = null;
    try {
      ctx = ctxFactory.getSystemLdapContext();
      result = searchUsers(ctx, null, attributes, maxResults);
      List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
      if (result.hasMoreElements()) {
        ldapUsers.add(createUser(result.nextElement()));
      }
      return ldapUsers;
    }
    finally {
      LdapUtils.closeEnumeration(result);
      LdapUtils.closeContext(ctx);
    }
  }

  private NamingEnumeration<SearchResult> searchUsers(LdapContext ctx, String username, String[] attributes,
      long maxResults) throws NamingException
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

    // mandatory objectClass
    ldapFilter.append("(objectClass=").append(umap.getUserObjectClass()).append(')');

    // mandatory username
    ldapFilter.append('(').append(umap.getUserIDAttribute()).append('=').append(username != null ? username : "*")
        .append(')');

    // optional user filter
    if (StringUtils.isNotBlank(umap.getUserFilter())) {
      ldapFilter.append('(').append(umap.getUserFilter()).append(')');
    }

    ldapFilter.append(')');

    return ctx.search(baseDN, ldapFilter.toString(), controls);
  }

  private LdapUser createUser(SearchResult result) throws NamingException {
    LdapUser user = new LdapUser();

    Attributes attributes = result.getAttributes();

    user.setUsername(getAttributeValue(attributes, umap.getUserIDAttribute()));
    user.setPassword(getAttributeValue(attributes, umap.getUserPasswordAttribute()));
    user.setRealName(getAttributeValue(attributes, umap.getUserRealNameAttribute()));
    user.setEmail(getAttributeValue(attributes, umap.getUserEmailAttribute()));

    user.setDn(result.getNameInNamespace());

    return user;
  }

  private static String[] pickAttributes(String... names) {
    List<String> result = new ArrayList<String>(names.length);
    for (String n : names) {
      if (StringUtils.isNotBlank(n)) {
        result.add(n);
      }
    }
    return result.toArray(new String[result.size()]);
  }

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
}
