/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security Shiro realm backed by the CLM ODS database. It is used by shiro for authentication and authorization. It
 * also exposes a method for password encryption.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class CLMRealm
    extends AuthorizingRealm
{
  private static final Logger log = LoggerFactory.getLogger(CLMRealm.class);

  private final PasswordService passwordService;

  public CLMRealm() {
    setName("CLMRealm");

    passwordService = new DefaultPasswordService();

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

    User user = new UserDAO().getByUsernameLowercase(username.toLowerCase(Locale.ENGLISH));
    if (user != null) {
      // Shiro will verify the password
      return new SimpleAuthenticationInfo(username, user.getPassword(), getName());
    }

    // The username is not in the CLM db. Leave it to other realms to authenticate the user.
    return null;
  }

  /**
   * Encrypts the given password. The returned string can be saved as hashed password.
   */
  public String encryptPassword(String password) {
    if (password == null) {
      return null;
    }

    long start = System.currentTimeMillis();

    String encryptedPassword = passwordService.encryptPassword(password);

    log.debug("Encrypted password in {} ms", System.currentTimeMillis() - start);

    return encryptedPassword;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    // TODO To be implemented when we add support for authorization
    return null;
  }
}
