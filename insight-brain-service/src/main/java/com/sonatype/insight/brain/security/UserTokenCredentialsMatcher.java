/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordMatcher;

/**
 * Credential matcher for user tokens that handles both the new SHA-256 format and legacy formats.
 *
 * <p>
 * For tokens hashed with the new {@code $sha256$} format, verification is performed directly
 * via {@link UserTokenHashService} (single SHA-256 pass, sub-microsecond).
 * For legacy hashes (Argon2id, iterated SHA-256), delegates to Shiro's built-in {@link PasswordMatcher}.
 * </p>
 */
class UserTokenCredentialsMatcher
    extends PasswordMatcher
{
  private final UserTokenHashService userTokenHashService;

  UserTokenCredentialsMatcher(UserTokenHashService userTokenHashService) {
    this.userTokenHashService = userTokenHashService;
  }

  @Override
  public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
    Object storedCredentials = getStoredPassword(info);
    if (storedCredentials instanceof String storedHash && userTokenHashService.supports(storedHash)) {
      UsernamePasswordToken upToken = (UsernamePasswordToken) token;
      char[] passCode = upToken.getPassword();
      if (passCode == null) {
        return false;
      }
      return userTokenHashService.verifyPassCode(passCode, storedHash);
    }
    // Legacy format — delegate to Shiro's PasswordMatcher (handles Argon2id and iterated SHA-256)
    return super.doCredentialsMatch(token, info);
  }
}
