/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserFriendlyBasicHttpAuthenticationFilterTest
    extends AbstractComponentTest
{
  private final UserFriendlyBasicHttpAuthenticationFilter userFriendlyBasicHttpAuthenticationFilter =
      new UserFriendlyBasicHttpAuthenticationFilter();

  @Test
  public void testOnLoginFailure_CallsSendError() throws IOException {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);

    final AuthenticationException authException = new AuthenticationException();

    assertThat(userFriendlyBasicHttpAuthenticationFilter.onLoginFailure(null, authException, null, response)).isFalse();

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType(ErrorResponse.CONTENT_TYPE);
    verify(response).getWriter();
    verifyNoMoreInteractions(response);

    verify(writer).print(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
    verify(writer).close();
    verifyNoMoreInteractions(writer);
  }

  @Test
  public void testSendChallenge_WhenResponseIsAlreadyCommitted() {
    final ServletResponse response = mock(ServletResponse.class);
    when(response.isCommitted()).thenReturn(true);

    assertThat(userFriendlyBasicHttpAuthenticationFilter.sendChallenge(null, response)).isFalse();

    verify(response).isCommitted();
    verifyNoMoreInteractions(response);
  }

  @Test
  public void testSendChallenge_WhenResponseIsNotYetCommitted() throws Exception {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    when(response.getWriter()).thenReturn(mock(PrintWriter.class));

    assertThat(userFriendlyBasicHttpAuthenticationFilter.sendChallenge(null, response)).isFalse();

    verify(response).isCommitted();
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response, never()).setHeader(eq("WWW-Authenticate"), anyString());
  }

  @Test
  public void testOnAccessDenied_NoLogin_ReturnsTrue() throws Exception {
    assertThat(userFriendlyBasicHttpAuthenticationFilter.onAccessDenied(mock(HttpServletRequest.class), null)).isTrue();
  }

  @Test
  public void testOnAccessDenied_FailedLogin_ReturnsFalse() throws Exception {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getHeader("Authorization")).thenReturn("basic:authorization");
    doThrow(new AuthenticationException()).when(subject).login(any(AuthenticationToken.class));
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);
    when(mockHttpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));

    assertThat(userFriendlyBasicHttpAuthenticationFilter
        .onAccessDenied(mockHttpServletRequest, mockHttpServletResponse)).isFalse();
  }
}
