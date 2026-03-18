/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.ldap.DefaultLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;

/**
 * Shiro realm that uses {@link LdapService} for authentication.
 *
 * @since 1.7
 */
@Named
@Singleton
public class LdapRealm
    extends DefaultLdapRealm
{
  private final LdapService ldapService;

  private final LdapServerDAO ldapServerDAO;

  @Inject
  public LdapRealm(final LdapService ldapService, final LdapServerDAO ldapServerDAO) {
    this.ldapServerDAO = ldapServerDAO;
    setAuthenticationTokenClass(UsernamePasswordToken.class);
    this.ldapService = ldapService;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    if (super.supports(token)) {
      for (LdapServer ldapServer : ldapServerDAO.getAll()) {
        if (ldapService.isLdapEnabled(ldapServer)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public LdapContextFactory getContextFactory() {
    return null; // avoid creating factory here as we create our own later
  }

  @Override
  protected AuthenticationInfo queryForAuthenticationInfo(
      AuthenticationToken token,
      LdapContextFactory contextFactory) throws NamingException
  {
    String username = ((UsernamePasswordToken) token).getUsername();
    char[] password = ((UsernamePasswordToken) token).getPassword();

    LdapUser ldapUser = ldapService.authenticateUser(username, password);
    Set<String> membership = ldapUser.getMembership();

    return new SimpleAuthenticationInfo(
        new UserPrincipal(ldapUser.getUsername(), ldapUser.getRealName(), ldapUser.getServerId(), membership), null,
        getName());
  }
}
