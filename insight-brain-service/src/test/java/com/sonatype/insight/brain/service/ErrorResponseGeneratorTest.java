/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.security.ExpiredUserTokenException;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorResponseGeneratorTest
{
  private static final String ID_PREFIX = " (ID ";

  private final ErrorResponseGenerator generator = new ErrorResponseGenerator();

  @Test
  public void testGetStatusCode_HandleShiroExceptions() {
    assertThat(generator.mapExceptionAndLog(new UnauthorizedException()).getStatusCode()).isEqualTo(403);
    assertThat(generator.mapExceptionAndLog(new UnauthenticatedException()).getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testBuildErrorResponseCallsSuperWhenUnhandled() {
    final ErrorResponse errorResponse = generator.mapExceptionAndLog(new Exception());
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    assertThat(errorResponse.getMessageBody())
        .startsWith(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase() + ID_PREFIX)
        .endsWith(")");
  }

  @Test
  public void testBuildErrorResponseWithUnauthenticatedException() {
    final ErrorResponse errorResponse = generator.mapExceptionAndLog(new UnauthenticatedException());
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(errorResponse.getMessageBody()).isEqualTo(Response.Status.UNAUTHORIZED.getReasonPhrase());
  }

  @Test
  public void testBuildErrorResponseWithUnauthorizedException() {
    final ErrorResponse errorResponse = generator.mapExceptionAndLog(new UnauthorizedException());
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(errorResponse.getMessageBody()).isEqualTo(Status.FORBIDDEN.getReasonPhrase());
  }

  @Test
  public void testBuildErrorResponseWithAuthenticationExceptionAndNullCause() {
    final ErrorResponse errorResponse = generator.mapExceptionAndLog(new AuthenticationException());
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNamingException() {
    final ErrorResponse errorResponse = generator
        .mapExceptionAndLog(new AuthenticationException(new NamingException()));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    assertThat(errorResponse.getMessageBody()).startsWith(ErrorResponseGenerator.MSG_LDAP_FAILURE + ID_PREFIX)
        .endsWith(")");
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNamingExceptionTimeout() {
    final ErrorResponse errorResponse = generator
        .mapExceptionAndLog(new AuthenticationException(new NamingException("timeout")));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    assertThat(errorResponse.getMessageBody()).startsWith(ErrorResponseGenerator.MSG_LDAP_TIMEOUT + ID_PREFIX)
        .endsWith(")");
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseNameNotFoundException() {
    final ErrorResponse errorResponse = generator
        .mapExceptionAndLog(new AuthenticationException(new NameNotFoundException()));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  @Test
  public void testBuildErrorResponseWithAuthcCauseJavaxAuthenticationException() {
    final ErrorResponse errorResponse = generator
        .mapExceptionAndLog(new AuthenticationException(new javax.naming.AuthenticationException()));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  @Test
  public void testBuildErrorResponse_JsonUnparsable() {
    final ErrorResponse errorResponse = generator.buildErrorResponse(new JsonParseException(null, "error"));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_JSON_UNPARSABLE);
  }

  @Test
  public void testBuildErrorResponse_JsonUnmappable() {
    final ErrorResponse errorResponse = generator.buildErrorResponse(new JsonMappingException(null, "error"));
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_JSON_UNMAPPABLE);
  }

  @Test
  public void testBuildErrorResponse_ExpiredUserTokenException() {
    final ErrorResponse errorResponse = generator.mapExceptionAndLog(new ExpiredUserTokenException());
    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(errorResponse.getMessageBody()).isEqualTo(ErrorResponseGenerator.MSG_EXPIRED_TOKEN);
  }
}
