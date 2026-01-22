/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * Security Shiro realm backed by the CLM ODS database. It is used by shiro for authentication and authorization. It
 * also exposes a method for password encryption.
 *
 * @since 1.7
 */
@Named
@Singleton
public class InternalRealm
    extends AuthenticatingRealm
{
  public static final String DISPLAY_NAME = "IQ Server";

  public static final String ID = User.INTERNAL_REALM_ID;

  private final UserDAO userDAO;

  @Inject
  public InternalRealm(PasswordService passwordService, UserDAO userDAO) {
    this.userDAO = userDAO;
    setName("CLMRealm");

    // Create and set a password matcher. It will be used by shiro to match hashed passwords.
    PasswordMatcher passwordMatcher = new PasswordMatcher();
    passwordMatcher.setPasswordService(passwordService);
    setCredentialsMatcher(passwordMatcher);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;

    String username = usernamePasswordToken.getUsername();
    if (StringUtils.isBlank(username)) {
      throw new AuthenticationException("The username is required");
    }

    User user = userDAO.getByUsername(username);
    if (user != null) {
      // Shiro will verify the password.
      // For internal users, the passsed in username is case insensitive,
      // so it doesn't have to match exactly what is stored in the db.
      // We use the value stored in the db to create the UserPrincipal in order to get consistent values.
      // This is also important for the creation of user tokens, where the user token's username must match exactly the
      // user's username stored in the db.
      return new SimpleAuthenticationInfo(new UserPrincipal(user.getUsername(), user.calculateDisplayName(), ID),
          user.getPassword(), getName());
    }

    // The username is not in the CLM db. Leave it to other realms to authenticate the user.
    return null;
  }
}
