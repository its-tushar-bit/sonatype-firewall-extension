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

import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;

/**
 * Degenerated LDAP realm that verifies a user exists in LDAP but that doesn't verify credentials which supposedly was
 * already done by some 3rd-party SSO frontend. Supports {@link ReverseProxyAuthenticationFilter} and handles login of
 * remote users such that the calling subject becomes "authenticated".
 */
@Named
@Singleton
public class ReverseProxyRealm
    extends JndiLdapRealm
{
  private final LdapManager ldapManager;

  @Inject
  public ReverseProxyRealm(LdapManager ldapManager) {
    this.ldapManager = ldapManager;
    setAuthenticationTokenClass(ReverseProxyAuthenticationToken.class);
    setContextFactory(null);
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return super.supports(token) && ldapManager.isLdapEnabled();
  }

  @Override
  protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory contextFactory)
      throws NamingException
  {
    String username = ((ReverseProxyAuthenticationToken) token).getUsername();

    LdapUser ldapUser = ldapManager.getUser(username);

    return new SimpleAuthenticationInfo(new UserPrincipal(username, ldapUser.getRealName(), false,
        ldapUser.getMembership()), null, getName());
  }
}
