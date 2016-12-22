/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * Degenerated realm that verifies a user exists locally or in LDAP but that doesn't verify credentials which supposedly
 * was already done by some 3rd-party SSO frontend. Supports {@link ReverseProxyAuthenticationFilter} and handles login
 * of remote users such that the calling subject becomes "authenticated".
 */
@Named
@Singleton
public class ReverseProxyRealm
    extends AuthenticatingRealm
{
  private final LdapManager ldapManager;

  @Inject
  public ReverseProxyRealm(LdapManager ldapManager) {
    this.ldapManager = ldapManager;
    setAuthenticationTokenClass(ReverseProxyAuthenticationToken.class);
    setCredentialsMatcher(new AllowAllCredentialsMatcher());
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    String username = ((ReverseProxyAuthenticationToken) token).getUsername();
    AuthenticationInfo info = doGetInternalRealmAuthenticationInfo(username);
    if (info == null) {
      info = doGetLdapRealmAuthenticationInfo(username);
    }
    return info;
  }

  private AuthenticationInfo doGetInternalRealmAuthenticationInfo(String username) {
    User user = new UserDAO().getByUsername(username);
    if (user != null) {
      return new SimpleAuthenticationInfo(new UserPrincipal(username, user.calculateDisplayName(), true),
          null, getName());
    }
    return null;
  }

  private AuthenticationInfo doGetLdapRealmAuthenticationInfo(String username) {
    try {
      LdapUser ldapUser = ldapManager.authenticateUserForReverseProxy(username);
      if (ldapUser != null) {
        return new SimpleAuthenticationInfo(new UserPrincipal(username, ldapUser.getRealName(), false,
            ldapUser.getMembership()), null, getName());
      }
      return null;
    }
    catch (NamingException e) {
      throw new AuthenticationException("LDAP naming error while attempting to authenticate user: " + username, e);
    }
  }

}
