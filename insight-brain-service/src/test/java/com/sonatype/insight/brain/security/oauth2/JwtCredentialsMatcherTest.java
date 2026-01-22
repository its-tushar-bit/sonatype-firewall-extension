/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JwtCredentialsMatcherTest
    extends AbstractComponentTest
{
  @Inject
  private JWTGenerator jwtGenerator;

  @Mock
  private ShiroJsonWebTokenValidator shiroJsonWebTokenValidator;

  private JwtCredentialsMatcher credentialsMatcher;

  @Before
  public void before() {
    credentialsMatcher = new JwtCredentialsMatcher(shiroJsonWebTokenValidator);
  }

  @Test
  public void testDoCredentialsMatch() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final Set<String> groups = new HashSet<>(Arrays.asList("admin", "dev", "other"));

    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);
    AuthenticationInfo authenticationInfo =
        new SimpleAuthenticationInfo(new UserPrincipal(sub, sub, OAuth2Realm.ID, groups),
            shiroJsonWebToken.getCredentials(), OAuth2Realm.ID);
    when(shiroJsonWebTokenValidator.isTokenValid(shiroJsonWebToken)).thenReturn(true);

    assertThat(credentialsMatcher.doCredentialsMatch(shiroJsonWebToken, authenticationInfo)).isTrue();
    verify(shiroJsonWebTokenValidator).isTokenValid(shiroJsonWebToken);
  }

  @Test
  public void testDoCredentialsMatch_ReturnsFalseIfSignaturesAreDifferent() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final Set<String> groups = new HashSet<>(Arrays.asList("admin", "dev", "other"));

    String token = jwtGenerator.generateJWT(sub, issuer);
    String anotherToken = jwtGenerator.generateJWT("alice", issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);
    ShiroJsonWebToken anotherShiroJsonWebToken = new ShiroJsonWebToken(anotherToken);
    AuthenticationInfo authenticationInfo =
        new SimpleAuthenticationInfo(new UserPrincipal(sub, sub, OAuth2Realm.ID, groups),
            anotherShiroJsonWebToken.getCredentials(), OAuth2Realm.ID);

    assertThat(credentialsMatcher.doCredentialsMatch(shiroJsonWebToken, authenticationInfo)).isFalse();
    verify(shiroJsonWebTokenValidator, never()).isTokenValid(shiroJsonWebToken);
  }

  @Test
  public void testDoCredentialsMatch_ReturnsFalseIfJwtVerificationFails() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final Set<String> groups = new HashSet<>(Arrays.asList("admin", "dev", "other"));

    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);
    AuthenticationInfo authenticationInfo =
        new SimpleAuthenticationInfo(new UserPrincipal(sub, sub, OAuth2Realm.ID, groups),
            shiroJsonWebToken.getCredentials(), OAuth2Realm.ID);
    when(shiroJsonWebTokenValidator.isTokenValid(shiroJsonWebToken)).thenReturn(false);

    assertThat(credentialsMatcher.doCredentialsMatch(shiroJsonWebToken, authenticationInfo)).isFalse();
    verify(shiroJsonWebTokenValidator).isTokenValid(shiroJsonWebToken);
  }
}
