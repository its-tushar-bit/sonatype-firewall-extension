/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Degenerated realm that doesn't verify credentials which supposedly was already done by some 3rd-party SSO frontend.
 * If found, additional information about the specified username (e.g. group memberships) is loaded from the internal
 * user database or LDAP. Supports {@link ReverseProxyAuthenticationFilter} and handles login of remote users such that
 * the calling subject becomes "authenticated".
 */
@Named
@Singleton
public class ReverseProxyRealm
    extends AuthenticatingRealm
{
  private static final Logger log = LoggerFactory.getLogger(ReverseProxyRealm.class);

  public static final String ID = "ReverseProxy";

  private final LdapService ldapService;

  private final UserDAO userDAO;

  @Inject
  public ReverseProxyRealm(LdapService ldapService, UserDAO userDAO) {
    this.ldapService = ldapService;
    this.userDAO = userDAO;
    setAuthenticationTokenClass(ReverseProxyAuthenticationToken.class);
    setCredentialsMatcher(new AllowAllCredentialsMatcher());
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    String username = ((ReverseProxyAuthenticationToken) token).getUsername();
    AuthenticationInfo info = doGetInternalRealmAuthenticationInfo(username);
    if (info == null) {
      info = doGetLdapRealmAuthenticationInfo(username);
      if (info == null) {
        info = new SimpleAuthenticationInfo(new UserPrincipal(username, username, ID), null, getName());
        log.debug("Found no user information for '{}'", username);
      }
      else {
        log.debug("Found user information for '{}' in LDAP", username);
      }
    }
    else {
      log.debug("Found user information for '{}' in local database", username);
    }
    return info;
  }

  private AuthenticationInfo doGetInternalRealmAuthenticationInfo(String username) {
    User user = userDAO.getByUsername(username);
    if (user != null) {
      return new SimpleAuthenticationInfo(new UserPrincipal(user.getUsername(), user.calculateDisplayName(), ID),
          null, getName());
    }
    return null;
  }

  private AuthenticationInfo doGetLdapRealmAuthenticationInfo(String username) {
    try {
      LdapUser ldapUser = ldapService.getUserByName(username);
      if (ldapUser != null) {
        return new SimpleAuthenticationInfo(
            new UserPrincipal(ldapUser.getUsername(), ldapUser.getRealName(), ID, ldapUser.getMembership()), null,
            getName());
      }
      return null;
    }
    catch (NameNotFoundException e) {
      // LDAP servers could be successfully queried for user but none have a matching record
      return null;
    }
    catch (NamingException e) {
      throw new AuthenticationException("LDAP naming error while attempting to authenticate user: " + username, e);
    }
  }
}
