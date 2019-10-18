/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.codehaus.plexus.util.StringUtils;

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

  @Inject
  public InternalRealm(PasswordService passwordService) {
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
    if (StringUtils.isEmpty(username)) {
      throw new AuthenticationException("The username is required");
    }

    User user = new UserDAO().getByUsername(username);
    if (user != null) {
      // Shiro will verify the password
      return new SimpleAuthenticationInfo(new UserPrincipal(username, user.calculateDisplayName(), true),
          user.getPassword(), getName());
    }

    // The username is not in the CLM db. Leave it to other realms to authenticate the user.
    return null;
  }
}
