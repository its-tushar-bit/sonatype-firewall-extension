/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Date;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.session.Session;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionExpirationCookieFilterTest
{
  private static final long TIMEOUT_MS = 30_000L;

  private static final long LAST_ACCESS_MS = 1_700_000_000_000L;

  private static final String EXPIRATION_COOKIE_NAME = SessionExpirationCookieFilter.EXPIRATION_COOKIE_NAME;

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain chain;

  @Mock
  private Subject subject;

  @Mock
  private FrameEmbeddingDetector frameEmbeddingDetector;

  private SessionExpirationCookieFilter filter;

  @Before
  public void setUp() {
    filter = new SessionExpirationCookieFilter(frameEmbeddingDetector);
    ThreadContext.bind(subject);
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void noSession_cookieNotEmitted() throws Exception {
    when(subject.getSession(false)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(response, never()).addCookie(org.mockito.ArgumentMatchers.any());
    verify(chain).doFilter(request, response);
  }

  @Test
  public void sessionPresentOnHttps_cookieHasSecureAndSameSiteLax() throws Exception {
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(true);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    assertThat(cookie.getName()).isEqualTo(EXPIRATION_COOKIE_NAME);
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
  }

  @Test
  public void sessionPresentOnHttp_cookieHasNoSecureButHasSameSiteLax() throws Exception {
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(false);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    assertThat(cookie.getName()).isEqualTo(EXPIRATION_COOKIE_NAME);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
  }

  @Test
  public void cookieValueIsLastAccessPlusTimeout() throws Exception {
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(false);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    long expectedValue = TIMEOUT_MS + LAST_ACCESS_MS;
    assertThat(cookie.getValue()).isEqualTo(Long.toString(expectedValue));
  }

  @Test
  public void chainIsAlwaysCalled() throws Exception {
    when(subject.getSession(false)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void iframeEmbeddingEnabled_cookieHasSameSiteNone() throws Exception {
    // When iframe embedding is enabled AND the request is HTTPS, the expiration cookie is
    // SameSite=None so embedded iframes (cross-site context) see the session expiration updates.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(true);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    assertThat(cookie.getAttribute("SameSite")).isEqualToIgnoringCase("None");
  }

  @Test
  public void iframeEmbeddingEnabled_httpRequest_cookieFallsBackToLax() throws Exception {
    // SameSite=None requires Secure (Chrome 80+, Firefox 79+). On HTTP we cannot set Secure, so we
    // fall back to Lax to avoid browsers silently discarding the cookie.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(true);
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(false);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    assertThat(cookie.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
  }

  @Test
  public void iframeEmbeddingDisabled_cookieKeepsLax() throws Exception {
    // Explicit sanity check of the happy-path default: detector returns false → SameSite=Lax.
    when(frameEmbeddingDetector.isFrameEmbeddingEnabled()).thenReturn(false);
    Session session = mockSession(TIMEOUT_MS, LAST_ACCESS_MS);
    when(subject.getSession(false)).thenReturn(session);
    when(request.isSecure()).thenReturn(true);

    filter.doFilter(request, response, chain);

    jakarta.servlet.http.Cookie cookie = captureAddedCookie();
    assertThat(cookie.getAttribute("SameSite")).isEqualToIgnoringCase("Lax");
  }

  private jakarta.servlet.http.Cookie captureAddedCookie() {
    ArgumentCaptor<jakarta.servlet.http.Cookie> cookieCaptor =
        ArgumentCaptor.forClass(jakarta.servlet.http.Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    return cookieCaptor.getValue();
  }

  private static Session mockSession(long timeout, long lastAccessMs) {
    Session session = mock(Session.class);
    when(session.getTimeout()).thenReturn(timeout);
    when(session.getLastAccessTime()).thenReturn(new Date(lastAccessMs));
    return session;
  }
}
