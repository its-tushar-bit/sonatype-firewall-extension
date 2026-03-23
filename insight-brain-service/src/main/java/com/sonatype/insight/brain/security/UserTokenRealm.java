/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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

import com.atlassian.crowd.exception.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.lang.util.ByteSource;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.PrincipalCollection;
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

  private final CrowdClientFactory crowdClientFactory;

  private final LdapServerDAO ldapServerDAO;

  private final UserTokenDAO userTokenDAO;

  private final UserDAO userDAO;

  private final SsoUserService ssoUserService;

  @Inject
  public UserTokenRealm(
      PasswordService passwordService,
      LdapService ldapService,
      UserTokenService userTokenService,
      CrowdClientFactory crowdClientFactory,
      LdapServerDAO ldapServerDAO,
      UserTokenDAO userTokenDAO,
      UserDAO userDAO,
      SsoUserService ssoUserService)
  {
    this.ldapServerDAO = ldapServerDAO;
    this.userTokenDAO = userTokenDAO;
    this.userDAO = userDAO;
    this.ssoUserService = ssoUserService;
    setName("UserTokenRealm");

    this.ldapService = ldapService;
    this.userTokenService = userTokenService;

    // Create and set a password matcher. It will be used by shiro to match hashed passwords.
    PasswordMatcher passwordMatcher = new PasswordMatcher();
    passwordMatcher.setPasswordService(passwordService);
    setCredentialsMatcher(passwordMatcher);
    this.crowdClientFactory = crowdClientFactory;
  }

  @Override
  protected void assertCredentialsMatch(
      final AuthenticationToken token,
      final AuthenticationInfo info) throws AuthenticationException
  {
    super.assertCredentialsMatch(token, info);
    SimpleAuthenticationInfoWithUserToken simpleAuthenticationInfoWithUserToken =
        (SimpleAuthenticationInfoWithUserToken) info;
    UserToken userToken = simpleAuthenticationInfoWithUserToken.getUserToken();
    userToken.setLastAccessTime(new Date());
    try {
      userTokenDAO.update(userToken);
    }
    catch (org.jooq.exception.DataAccessException e) {
      throw new AuthenticationException("User token '%s' no longer exists.".formatted(userToken.getUserCode()), e);
    }
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
    String username = usernamePasswordToken.getUsername();
    if (StringUtils.isBlank(username)) {
      throw new AuthenticationException("The username is required");
    }

    // If the authentication is attempted with a user token,
    // then the provided username must match a user token's userCode.
    UserToken userToken = userTokenDAO.getByUserCode(username);
    if (userToken == null) {
      // Leave it to other realms to authenticate the user.
      return null;
    }

    if (userTokenService.isTokenExpired(userToken)) {
      log.info("User token '{}' for user '{}' has expired", userToken.getUserCode(), userToken.getUsername());
      throw new ExpiredUserTokenException();
    }

    if (userToken.isInternalUser()) {
      return doGetInternalRealmAuthenticationInfo(userToken);
    }
    else if (userToken.isSsoUser()) {
      return doGetSsoRealmAuthenticationInfo(userToken);
    }
    else if (CrowdRealm.ID.equals(userToken.getRealmId())) {
      return doGetCrowdRealmAuthenticationInfo(userToken);
    }
    else {
      return doGetLdapRealmAuthenticationInfo(userToken);
    }
  }

  private SimpleAuthenticationInfoWithUserToken doGetInternalRealmAuthenticationInfo(UserToken userToken) {
    User user = userDAO.getByUsername(userToken.getUsername());
    return new SimpleAuthenticationInfoWithUserToken( //
        new UserPrincipal(userToken.getUsername(), user.calculateDisplayName(), ID), //
        userToken.getPassCode(), //
        getName(),
        userToken);
  }

  private SimpleAuthenticationInfoWithUserToken doGetSsoRealmAuthenticationInfo(UserToken userToken) {
    SsoUser ssoUser = ssoUserService.getByUsername(userToken.getUsername());
    return new SimpleAuthenticationInfoWithUserToken( //
        new UserPrincipal(ssoUser.getUsername(), ssoUser.calculateDisplayName(), ID, ssoUser.getGroups()), //
        userToken.getPassCode(), //
        getName(),
        userToken);
  }

  private SimpleAuthenticationInfoWithUserToken doGetCrowdRealmAuthenticationInfo(UserToken userToken) {
    CrowdClient crowdClient = crowdClientFactory.createCrowdClient();

    if (crowdClient == null) {
      return null;
    }

    try {
      return new SimpleAuthenticationInfoWithUserToken(crowdClient.getUser(userToken), userToken.getPassCode(),
          getName(), userToken);
    }
    catch (UserNotFoundException e) {
      // The Crowd user was deleted.
      try (AuditSession auditSession = AuditData.get()
          .recordSystemEvent(AuditEvent.DELETE_USER_TOKEN, true /* independent */))
      {
        userTokenService.deleteAndAuditUserToken(userToken);
      }
      log.info(
          "The '{}' user token was created for the '{}' Crowd user, which doesn't exist anymore."
              + " The user token was deleted.",
          userToken.getUserCode(), userToken.getUsername());

      throw new AuthenticationException("Invalid user token.", e);
    }
    catch (Exception e) {
      throw new AuthenticationException(
          String.format("Could not authenticate the '%s' Crowd user with their '%s' user token.",
              userToken.getUsername(), userToken.getUserCode()),
          e);
    }
  }

  private SimpleAuthenticationInfoWithUserToken doGetLdapRealmAuthenticationInfo(UserToken userToken) {
    String username = userToken.getUsername();
    LdapServer ldapServer = ldapServerDAO.getById(userToken.getRealmId());

    try {
      LdapUser ldapUser = ldapService.getUserByName(ldapServer, username);
      return new SimpleAuthenticationInfoWithUserToken( //
          new UserPrincipal(username, ldapUser.getRealName(), ID, ldapUser.getMembership()), //
          userToken.getPassCode(), //
          getName(),
          userToken);
    }
    catch (NameNotFoundException e) {
      // The LDAP user was deleted.
      try (AuditSession auditSession =
          AuditData.get().recordSystemEvent(AuditEvent.DELETE_USER_TOKEN, true /* independent */))
      {
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

  private static class SimpleAuthenticationInfoWithUserToken
      extends SimpleAuthenticationInfo
  {
    private final UserToken userToken;

    public SimpleAuthenticationInfoWithUserToken(final UserToken userToken) {
      this.userToken = userToken;
    }

    public SimpleAuthenticationInfoWithUserToken(
        final Object principal,
        final Object credentials,
        final String realmName,
        final UserToken userToken)
    {
      super(principal, credentials, realmName);
      this.userToken = userToken;
    }

    public SimpleAuthenticationInfoWithUserToken(
        final Object principal,
        final Object hashedCredentials,
        final ByteSource credentialsSalt,
        final String realmName,
        final UserToken userToken)
    {
      super(principal, hashedCredentials, credentialsSalt, realmName);
      this.userToken = userToken;
    }

    public SimpleAuthenticationInfoWithUserToken(
        final PrincipalCollection principals,
        final Object credentials,
        final UserToken userToken)
    {
      super(principals, credentials);
      this.userToken = userToken;
    }

    public SimpleAuthenticationInfoWithUserToken(
        final PrincipalCollection principals,
        final Object hashedCredentials,
        final ByteSource credentialsSalt,
        final UserToken userToken)
    {
      super(principals, hashedCredentials, credentialsSalt);
      this.userToken = userToken;
    }

    public UserToken getUserToken() {
      return userToken;
    }
  }
}
