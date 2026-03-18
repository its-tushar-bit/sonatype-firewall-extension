/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LoginErrorResponseHandlerTest
{
  private ErrorResponse errorResponse;

  @Test
  public void testSendErrorWithIOErrorWritingResponse() throws IOException {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final IOException ioException = new IOException();
    when(response.getWriter()).thenThrow(ioException);

    errorResponse = new ErrorResponse(HttpServletResponse.SC_ACCEPTED, null);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> LoginErrorResponseHandler.sendError(response, errorResponse)).withCause(ioException);
  }

  @Test
  public void testSendErrorSetsStatusContentTypeAndMessage() throws IOException {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);

    final String errMessage = "myErrorMessage";
    errorResponse = new ErrorResponse(HttpServletResponse.SC_ACCEPTED, errMessage);

    LoginErrorResponseHandler.sendError(response, errorResponse);

    verifyResponse(response, errorResponse.getStatusCode(), writer, errMessage);
  }

  private void verifyResponse(
      final HttpServletResponse response,
      final int expectedStatusCode,
      final PrintWriter writer,
      final String errMessage) throws IOException
  {
    verify(response).setStatus(expectedStatusCode);
    verify(response).setContentType(ErrorResponse.CONTENT_TYPE);
    verify(response).getWriter();
    verifyNoMoreInteractions(response);

    verify(writer).print(errMessage);
    verify(writer).close();
    verifyNoMoreInteractions(writer);
  }

  private static PrintWriter setupPrintWriter(final HttpServletResponse response) throws IOException {
    final PrintWriter writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);
    return writer;
  }

  @Test
  public void testSendErrorMapsException() throws IOException {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);

    LoginErrorResponseHandler.sendError(response, new AuthenticationException());

    verifyResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer,
        ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }
}
