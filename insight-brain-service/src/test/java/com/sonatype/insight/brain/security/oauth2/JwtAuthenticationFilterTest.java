/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.OidcToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;

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

  @Inject
  private OidcTokenDAO oidcTokenDAO;

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
  public void testIsLoginAttempt_TrueWhenTokenIdIsPresentInCookieAndOidcTokenIsOnDBAndIsLoginRequest() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    String token = jwtGenerator.generateJWT(sub, issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);

    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, oidcToken.getId())});
    when(request.getPathInfo()).thenReturn(JwtAuthenticationFilter.LOGIN_REQUEST);

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isTrue();
    verify(request).getCookies();
  }

  @Test
  public void testIsLoginAttempt_FalseWhenTokenIdIsPresentInCookieAndOidcTokenIsNotOnDBAndIsLoginRequest() {
    final HttpServletRequest request = mock(HttpServletRequest.class);

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    String tokenId = IdUtil.newUUID();

    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, tokenId)});
    when(request.getPathInfo()).thenReturn(JwtAuthenticationFilter.LOGIN_REQUEST);

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isFalse();
    verify(request).getCookies();
  }

  @Test
  public void testIsLoginAttempt_TrueWhenOidcTokenIsPresentInCookieAndIsLoginRequest() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    String token = jwtGenerator.generateJWT(sub, issuer);

    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, token)});
    when(request.getPathInfo()).thenReturn(JwtAuthenticationFilter.LOGIN_REQUEST);

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isTrue();
    verify(request).getCookies();
  }

  @Test
  public void testIsLoginAttempt_FalseWhenOidcTokenIsPresentInCookieAndIsNotLoginRequest() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    String token = jwtGenerator.generateJWT(sub, issuer);

    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, token)});
    when(request.getPathInfo()).thenReturn("not/login/request/path");

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isFalse();
    verify(request).getCookies();
  }

  @Test
  public void testIsLoginAttempt_FalseWhenTokenIdIsPresentInCookieAndIsNotLoginRequest() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);

    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    String token = jwtGenerator.generateJWT(sub, issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);

    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, oidcToken.getId())});
    when(request.getPathInfo()).thenReturn("not/login/request/path");

    assertThat(jwtAuthenticationFilter.isLoginAttempt(request, null)).isFalse();
    verify(request).getCookies();
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
  public void testCreateToken_ShouldCreateJwtToken_FromOidcToken() {
    final String subject = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);
    String token = jwtGenerator.generateJWT(subject, issuer);
    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, token)});

    AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);

    assertThat(authenticationToken).isInstanceOf(ShiroJsonWebToken.class);
    ShiroJsonWebToken shiroJsonWebToken = (ShiroJsonWebToken) authenticationToken;
    assertThat(shiroJsonWebToken.getPrincipal().getIssuer()).isEqualTo(issuer);
    assertThat(shiroJsonWebToken.getPrincipal().getSubject()).isEqualTo(subject);
    verify(request).getCookies();
  }

  @Test
  public void testCreateToken_ShouldCreateJwtToken_FromTokenId() {
    final String subject = "bob";
    final String issuer = "https://an-idp.com";
    final HttpServletRequest request = mock(HttpServletRequest.class);
    String token = jwtGenerator.generateJWT(subject, issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);

    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, oidcToken.getId())});
    AuthenticationToken authenticationToken = jwtAuthenticationFilter.createToken(request, null);

    assertThat(authenticationToken).isInstanceOf(ShiroJsonWebToken.class);
    ShiroJsonWebToken shiroJsonWebToken = (ShiroJsonWebToken) authenticationToken;
    assertThat(shiroJsonWebToken.getPrincipal().getIssuer()).isEqualTo(issuer);
    assertThat(shiroJsonWebToken.getPrincipal().getSubject()).isEqualTo(subject);
    verify(request).getCookies();
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

    String token = jwtGenerator.generateJWT(sub, issuer);
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(String.format("Bearer %s", token));
    doThrow(new AuthenticationException()).when(subject).login(any(AuthenticationToken.class));

    assertThat(jwtAuthenticationFilter.onAccessDenied(request, response)).isFalse();
  }
}
