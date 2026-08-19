/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import com.nimbusds.jwt.SignedJWT;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

public class JwtCredentialsMatcher
    implements CredentialsMatcher
{
  private final ShiroJsonWebTokenValidator shiroJsonWebTokenValidator;

  public JwtCredentialsMatcher(final ShiroJsonWebTokenValidator shiroJsonWebTokenValidator) {
    this.shiroJsonWebTokenValidator = shiroJsonWebTokenValidator;
  }

  @Override
  public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
    final ShiroJsonWebToken jwtToken = (ShiroJsonWebToken) token;
    final SignedJWT tokenCredentials = jwtToken.getCredentials();
    final SignedJWT infoCredentials = (SignedJWT) info.getCredentials();

    // Checking tokens have the same signature
    if (!tokenCredentials.getSignature().equals(infoCredentials.getSignature())) {
      return false;
    }

    // Verify JWT token signature and claims
    return shiroJsonWebTokenValidator.isTokenValid(jwtToken);
  }
}
