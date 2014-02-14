/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;

/**
 * Shiro realm that uses {@link LdapManager} for authentication.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapRealm
    extends JndiLdapRealm
{
  private final LdapManager ldapManager;

  @Inject
  public LdapRealm(LdapManager ldapManager) {
    setAuthenticationTokenClass(UsernamePasswordToken.class);
    this.ldapManager = ldapManager;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return super.supports(token) && ldapManager.isLdapEnabled();
  }

  @Override
  public LdapContextFactory getContextFactory() {
    return null; // avoid creating factory here as we create our own later
  }

  @Override
  protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory contextFactory)
      throws NamingException
  {
    String username = ((UsernamePasswordToken) token).getUsername();
    char[] password = ((UsernamePasswordToken) token).getPassword();

    LdapUser ldapUser = ldapManager.authenticateUser(username, password);
    Set<String> membership = ldapUser.getMembership();

    return new SimpleAuthenticationInfo(new UserPrincipal(username, ldapUser.getRealName(), false, membership), null, getName());
  }
}
