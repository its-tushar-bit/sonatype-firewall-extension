/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RequestDataTest
{
  @Test
  public void testNewInstance_Method() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getMethod()).thenReturn("method");

    assertThat(RequestData.newInstance(mockHttpServletRequest).getMethod()).isEqualTo("method");
  }

  @Test
  public void testNewInstance_Uri_NullQueryParams() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getRequestURI()).thenReturn("requestUri");

    assertThat(RequestData.newInstance(mockHttpServletRequest).getUri()).isEqualTo("requestUri");
  }

  @Test
  public void testNewInstance_Uri_QueryParams() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getRequestURI()).thenReturn("requestUri");
    when(mockHttpServletRequest.getQueryString()).thenReturn("queryString");

    assertThat(RequestData.newInstance(mockHttpServletRequest).getUri()).isEqualTo("requestUri?queryString");
  }

  @Test
  public void testNewInstance_RemoteIpAddress() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn("remoteAddr");

    assertThat(RequestData.newInstance(mockHttpServletRequest).getRemoteIpAddress()).isEqualTo("remoteAddr");
  }

  @Test
  public void testNewInstance_Forwarded() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getHeaders(HttpHeaders.FORWARDED))
        .thenReturn(Collections.enumeration(Arrays.asList("forwarded1", "forwarded2", "forwarded3")));

    assertThat(RequestData.newInstance(mockHttpServletRequest).getForwarded())
        .isEqualTo("forwarded1, forwarded2, forwarded3");
  }

  @Test
  public void testNewInstance_ForwardedNull_XForwardedFor() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getHeaders(HttpHeaders.X_FORWARDED_FOR))
        .thenReturn(Collections.enumeration(Arrays.asList("forwarded1", "forwarded2", "forwarded3")));

    assertThat(RequestData.newInstance(mockHttpServletRequest).getForwarded())
        .isEqualTo("forwarded1, forwarded2, forwarded3");
  }

  @Test
  public void testNewInstance_UserAgent() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn("userAgent");

    assertThat(RequestData.newInstance(mockHttpServletRequest).getUserAgent()).isEqualTo("userAgent");
  }

  @Test
  public void testNewInstance_SessionId() {
    HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
    when(mockHttpServletRequest.getCookies()).thenReturn(new Cookie[]{
      new Cookie("cookieName1", "cookieValue1"), new Cookie(SecurityConfiguration.SESSION_COOKIE_NAME, "sessionId"),
      new Cookie("cookieName3", "cookieValue3")
    });

    assertThat(RequestData.newInstance(mockHttpServletRequest).getSessionId()).isEqualTo("sessionId");
  }

  private HttpServletRequest mockHttpServletRequest() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getHeaders(HttpHeaders.FORWARDED)).thenReturn(Collections.emptyEnumeration());
    when(mockHttpServletRequest.getHeaders(HttpHeaders.X_FORWARDED_FOR)).thenReturn(Collections.emptyEnumeration());
    return mockHttpServletRequest;
  }
}
