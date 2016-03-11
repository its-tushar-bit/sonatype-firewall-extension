/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class ErrorResponseGeneratorTest
{
  private static final String ID_PREFIX = " (ID ";

  private final ErrorResponseGenerator generator = new ErrorResponseGenerator();

  @Test
  public void testGetStatusCode_HandleShiroExceptions() {
    assertThat(generator.mapException(new UnauthorizedException()).getStatusCode(), is(403));
    assertThat(generator.mapException(new UnauthenticatedException()).getStatusCode(), is(401));
  }

  @Test
  public void testBuildErrorResponseCallsSuperWhenUnhandled() {
    final ErrorResponse errorResponse = generator.mapException(new Exception());
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    assertThat(errorResponse.getMessageBody(),
        allOf(startsWith(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase() + ID_PREFIX), endsWith(")")));
  }

  @Test
  public void testBuildErrorResponseWithUnauthenticatedException() {
    final ErrorResponse errorResponse = generator.mapException(new UnauthenticatedException());
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_UNAUTHORIZED));
    assertThat(errorResponse.getMessageBody(), is(Response.Status.UNAUTHORIZED.getReasonPhrase()));
  }

  @Test
  public void testBuildErrorResponseWithUnauthorizedException() {
    final ErrorResponse errorResponse = generator.mapException(new UnauthorizedException());
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_FORBIDDEN));
    assertThat(errorResponse.getMessageBody(), is(Status.FORBIDDEN.getReasonPhrase()));
  }

  @Test
  public void testBuildErrorResponseWithAuthenticationExceptionAndNullCause() {
    final ErrorResponse errorResponse = generator.mapException(new AuthenticationException());
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_UNAUTHORIZED));
    assertThat(errorResponse.getMessageBody(), is(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNamingException() {
    final ErrorResponse errorResponse = generator.mapException(new AuthenticationException(new NamingException()));
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    assertThat(errorResponse.getMessageBody(),
        allOf(startsWith(ErrorResponseGenerator.MSG_LDAP_FAILURE + ID_PREFIX), endsWith(")")));
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNamingExceptionTimeout() {
    final ErrorResponse errorResponse = generator
        .mapException(new AuthenticationException(new NamingException("timeout")));
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    assertThat(errorResponse.getMessageBody(),
        allOf(startsWith(ErrorResponseGenerator.MSG_LDAP_TIMEOUT + ID_PREFIX), endsWith((")"))));
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNameNotFoundException() {
    final ErrorResponse errorResponse = generator
        .mapException(new AuthenticationException(new NameNotFoundException()));
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_UNAUTHORIZED));
    assertThat(errorResponse.getMessageBody(), is(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseJavaxAuthenticationException() {
    final ErrorResponse errorResponse = generator
        .mapException(new AuthenticationException(new javax.naming.AuthenticationException()));
    assertThat(errorResponse.getStatusCode(), is(HttpServletResponse.SC_UNAUTHORIZED));
    assertThat(errorResponse.getMessageBody(), is(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
  }
}
