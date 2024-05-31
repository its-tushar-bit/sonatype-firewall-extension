/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
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
  public void testIsLoginAttempt_FalseWhenOAuthFeatureEnabledAndNoBearerToken() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic user:password");

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isFalse();
  }

  @Test
  public void testIsLoginAttempt_TrueWhenOAuthFeatureEnabledAndBearerTokenIsSent() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer a-bearer-token");

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isTrue();
  }

  @Test
  public void testIsLoginAttempt_TrueWhenIdTokenIsPresentOnSession() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final Subject mockedSubject = mock(Subject.class);
    final Session mockedSession = mock(Session.class);
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      String token = jwtGenerator.generateJWT(sub, issuer);

      securityUtils.when(SecurityUtils::getSubject).thenReturn(mockedSubject);
      when(mockedSubject.getSession(false)).thenReturn(mockedSession);
      when(mockedSession.getAttribute(JwtAuthenticationFilter.ID_TOKEN_PARAM)).thenReturn(token);

      assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isTrue();
    }
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
  public void testCreateToken_ShouldCreateJwtToken_FromIdToken() {
    final String subject = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final Subject mockedSubject = mock(Subject.class);
    final Session mockedSession = mock(Session.class);

    try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
      String token = jwtGenerator.generateJWT(subject, issuer);

      securityUtils.when(SecurityUtils::getSubject).thenReturn(mockedSubject);
      when(mockedSubject.getSession(false)).thenReturn(mockedSession);
      when(mockedSession.getAttribute(JwtAuthenticationFilter.ID_TOKEN_PARAM)).thenReturn(token);

      AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);

      assertThat(authenticationToken).isInstanceOf(ShiroJsonWebToken.class);
      ShiroJsonWebToken shiroJsonWebToken = (ShiroJsonWebToken) authenticationToken;
      assertThat(shiroJsonWebToken.getPrincipal().getIssuer()).isEqualTo(issuer);
      assertThat(shiroJsonWebToken.getPrincipal().getSubject()).isEqualTo(subject);
      securityUtils.verify(SecurityUtils::getSubject, times(3));
      verify(mockedSubject, times(3)).getSession(false);
      verify(mockedSession).getAttribute(JwtAuthenticationFilter.ID_TOKEN_PARAM);
      verify(mockedSession).removeAttribute(JwtAuthenticationFilter.ID_TOKEN_PARAM);
      verify(mockedSession).removeAttribute(JwtAuthenticationFilter.ACCESS_TOKEN_PARAM);
    }
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

    String token = jwtGenerator.generateJWT(sub, issuer);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(String.format("Bearer %s", token));
    doThrow(new AuthenticationException()).when(subject).login(any(AuthenticationToken.class));

    assertThat(jwtAuthenticationFilter.onAccessDenied(request, response)).isFalse();
  }
}
