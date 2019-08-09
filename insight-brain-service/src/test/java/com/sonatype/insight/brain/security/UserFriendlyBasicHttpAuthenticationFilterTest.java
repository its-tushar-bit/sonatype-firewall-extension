/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserFriendlyBasicHttpAuthenticationFilterTest
{
  private UserFriendlyBasicHttpAuthenticationFilter userFriendlyBasicHttpAuthenticationFilter =
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
  public void testSendChallenge_WhenResponseIsNotYetCommitted() {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);

    assertThat(userFriendlyBasicHttpAuthenticationFilter.sendChallenge(null, response)).isFalse();

    verify(response).isCommitted();
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setHeader(eq("WWW-Authenticate"), anyString());
    verifyNoMoreInteractions(response);
  }
}
