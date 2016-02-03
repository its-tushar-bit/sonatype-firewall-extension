/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecureCookiesFilterTest
{

  private static String COOKIE_1_INSECURE = "JSESSIONID=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/; HttpOnly";

  private static String COOKIE_2_INSECURE = "simple=cookie";

  private static String COOKIE_3_SECURE = SecurityModule.SESSION_COOKIE_NAME
      + "=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/; HttpOnly" + SecureCookiesFilter.SECURE_FLAG;

  private static String COOKIE_4_SECURE = "rememberMe=deleteMe; Path=/; HttpOnly" + SecureCookiesFilter.SECURE_FLAG;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private FilterChain filterChain;

  @Mock
  private ServletResponse notInstanceOfHttpServletResponse;

  private SecureCookiesFilter cookieFilter;

  @Before
  public void setUp() throws Exception {
    this.cookieFilter = new SecureCookiesFilter();
  }

  @Test
  public void testIfRequestIsSecureSetSecureCookieAttributeWhenMissing() throws Exception {
    when(request.isSecure()).thenReturn(true);
    when(response.getHeaders("Set-Cookie")).thenReturn(
        asList(COOKIE_1_INSECURE, COOKIE_2_INSECURE, COOKIE_3_SECURE, COOKIE_4_SECURE));

    cookieFilter.doFilter(request, response, filterChain);

    verify(response).setHeader("Set-Cookie", COOKIE_1_INSECURE + SecureCookiesFilter.SECURE_FLAG);
    verify(response).addHeader("Set-Cookie", COOKIE_2_INSECURE + SecureCookiesFilter.SECURE_FLAG);
    verify(response).addHeader("Set-Cookie", COOKIE_3_SECURE);
    verify(response).addHeader("Set-Cookie", COOKIE_4_SECURE);
  }

  @Test
  public void testIfRequestIsNotSecureDoNotChangeCookieAttributes() throws Exception {
    when(request.isSecure()).thenReturn(false);
    when(response.getHeaders("Set-Cookie")).thenReturn(asList(COOKIE_1_INSECURE, COOKIE_3_SECURE));

    cookieFilter.doFilter(request, response, filterChain);

    verifyZeroInteractions(response);
  }

  @Test
  public void testIfNotInstanceOfHttpServletResponseDoNotProcessResponse() throws Exception {
    cookieFilter.doFilter(request, notInstanceOfHttpServletResponse, filterChain);

    verifyZeroInteractions(response);
  }

  @Test
  public void testIfNoCookieHeadersDoNotChangeTheResponse() throws Exception {
    when(request.isSecure()).thenReturn(true);

    cookieFilter.doFilter(request, response, filterChain);

    verify(response, times(0)).setHeader(anyString(), anyString());
    verify(response, times(0)).addHeader(anyString(), anyString());
  }
}
