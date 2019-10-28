/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security Shiro realm that works with user tokens.
 *
 * @since 1.76
 */
@Named
@Singleton
public class UserTokenRealm
    extends AuthenticatingRealm
{
  private static final Logger log = LoggerFactory.getLogger(UserTokenRealm.class);

  public static final String ID = "UserToken";

  private final LdapService ldapService;

  private final UserTokenService userTokenService;

  @Inject
  public UserTokenRealm(PasswordService passwordService, LdapService ldapService, UserTokenService userTokenService) {
    setName("UserTokenRealm");

    this.ldapService = ldapService;
    this.userTokenService = userTokenService;

    // Create and set a password matcher. It will be used by shiro to match hashed passwords.
    PasswordMatcher passwordMatcher = new PasswordMatcher();
    passwordMatcher.setPasswordService(passwordService);
    setCredentialsMatcher(passwordMatcher);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
    String username = usernamePasswordToken.getUsername();
    if (StringUtils.isEmpty(username)) {
      throw new AuthenticationException("The username is required");
    }

    // If the authentication is attempted with a user token,
    // then the provided username must match a user token's userCode.
    UserToken userToken = new UserTokenDAO().getByUserCode(username);
    if (userToken == null) {
      // Leave it to other realms to authenticate the user.
      return null;
    }

    if (userToken.isInternalUser()) {
      return doGetInternalRealmAuthenticationInfo(userToken);
    }
    else {
      return doGetLdapRealmAuthenticationInfo(userToken);
    }
  }

  private SimpleAuthenticationInfo doGetInternalRealmAuthenticationInfo(UserToken userToken) {
    User user = new UserDAO().getByUsername(userToken.getUsername());
    return new SimpleAuthenticationInfo( //
        new UserPrincipal(userToken.getUsername(), user.calculateDisplayName(), ID), //
        userToken.getPassCode(), //
        getName());
  }

  private SimpleAuthenticationInfo doGetLdapRealmAuthenticationInfo(UserToken userToken) {
    String username = userToken.getUsername();
    LdapServer ldapServer = new LdapServerDAO().getById(userToken.getRealmId());

    try {
      LdapUser ldapUser = ldapService.getUserByName(ldapServer, username);
      return new SimpleAuthenticationInfo( //
          new UserPrincipal(username, ldapUser.getRealName(), ID, ldapUser.getMembership()), //
          userToken.getPassCode(), //
          getName());
    }
    catch (NameNotFoundException e) {
      // The LDAP user was deleted.
      new UserTokenDAO().delete(userToken);
      try (AuditSession auditSession =
          AuditData.get().recordSystemEvent(AuditEvent.DELETE_USER_TOKEN, true /* independent */)) {
        userTokenService.deleteAndAuditUserToken(userToken);
      }

      log.info(
          "The '{}' user token was created for the '{}' LDAP user, which doesn't exist anymore."
              + " The user token was deleted.",
          userToken.getUserCode(), username);

      throw new AuthenticationException("Invalid user token.", e);
    }
    catch (NamingException e) {
      throw new AuthenticationException(
          "LDAP naming error while attempting to authenticate by user token.", e);
    }
  }
}
