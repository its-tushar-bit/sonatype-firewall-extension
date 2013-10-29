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
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.DefaultHashService;
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

  private final DefaultPasswordService passwordService;

  private static int hashIterations = DefaultPasswordService.DEFAULT_HASH_ITERATIONS;

  // This reduces the test execution time for this module by ~30%.
  // In my tests, it doesn't make a big difference if we use 1 or 100 for hashIterations. I didn't want to use 1 because
  // it is a very special value and I chose 10 without any really good reason. :)
  public static void useWeakHashIterationForTestsOnly() {
    hashIterations = 10;
  }

  public CLMRealm() {
    setName("CLMRealm");

    passwordService = new DefaultPasswordService();
    // We create a DefaultHashService instance only to be able to change the default hash iteration count. Using the
    // default (500000), a password encryption takes about 500ms on my machine. Using 1000, it takes about 30ms.
    DefaultHashService hashService = new DefaultHashService();
    hashService.setHashAlgorithmName(DefaultPasswordService.DEFAULT_HASH_ALGORITHM);
    hashService.setHashIterations(hashIterations);
    hashService.setGeneratePublicSalt(true);
    passwordService.setHashService(hashService);

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
    if (password == null || password.trim().isEmpty()) {
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
