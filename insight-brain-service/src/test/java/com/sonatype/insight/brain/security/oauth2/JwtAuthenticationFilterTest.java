/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.io.PrintWriter;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JwtAuthenticationFilterTest
    extends AbstractComponentTest
{
  public static final String AUTHORIZATION_HEADER = "Authorization";

  @Inject
  private JWTGenerator jwtGenerator;

  @Inject
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  public void testIsLoginAttempt_FalseWhenOAuthFeatureDisabled() {
    assertThat(jwtAuthenticationFilter.isLoginAttempt(null, null)).isFalse();
  }

  @Test
  public void testIsLoginAttempt_TrueWhenOAuthFeatureEnabledAndBearerTokenIsSent() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer a-bearer-token");

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isTrue();
  }

  @Test
  public void testCreateToken_ShouldCreateJwtToken() {
    final String subject = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);

    String token = jwtGenerator.generateJWT(subject, issuer);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(String.format("Bearer %s", token));

    AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);

    assertThat(authenticationToken).isInstanceOf(ShiroJsonWebToken.class);
    ShiroJsonWebToken shiroJsonWebToken = (ShiroJsonWebToken) authenticationToken;
    assertThat(shiroJsonWebToken.getPrincipal().getIssuer()).isEqualTo(issuer);
    assertThat(shiroJsonWebToken.getPrincipal().getSubject()).isEqualTo(subject);
  }

  @Test
  public void testCreateToken_SkipIfNotBearerTokenPresent() {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic user:password");

    AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);
    assertThat(authenticationToken).isInstanceOf(BearerToken.class);

    BearerToken bearerToken = (BearerToken) authenticationToken;
    assertThat(bearerToken.getToken()).isEmpty();
  }

  @Test
  public void testCreateToken_SkipIfNotAuthorizationHeaderPresent() {
    final HttpServletRequest request = mock(HttpServletRequest.class);

    AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);
    assertThat(authenticationToken).isInstanceOf(BearerToken.class);

    BearerToken bearerToken = (BearerToken) authenticationToken;
    assertThat(bearerToken.getToken()).isEmpty();
  }

  @Test
  public void testOnAccessDenied_NoLogin_ReturnsTrue() throws Exception {
    assertThat(jwtAuthenticationFilter.onAccessDenied(mock(HttpServletRequest.class), null)).isTrue();
  }

  @Test
  public void testOnAccessDenied_FailedLogin_ReturnsFalse() throws Exception {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = mock(PrintWriter.class);

    String token = jwtGenerator.generateJWT(sub, issuer);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(String.format("Bearer %s", token));
    when(response.getWriter()).thenReturn(writer);
    doThrow(new AuthenticationException()).when(subject).login(any(AuthenticationToken.class));

    assertThat(jwtAuthenticationFilter.onAccessDenied(request, response)).isFalse();
    verify(writer).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }
}
