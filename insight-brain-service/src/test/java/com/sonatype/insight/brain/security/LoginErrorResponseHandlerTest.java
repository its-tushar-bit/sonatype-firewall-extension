/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.eclipse.jetty.server.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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

    try {
      LoginErrorResponseHandler.sendError(response, errorResponse);
      fail("Expected exception");
    }
    catch (RuntimeException e) {
      assertThat(e.getCause(), is((Throwable) ioException));
    }
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

  private void verifyResponse(final HttpServletResponse response,
                              final int expectedStatusCode,
                              final PrintWriter writer,
                              final String errMessage)
      throws IOException
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

    verifyResponse(response, Response.SC_UNAUTHORIZED, writer, ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

}
