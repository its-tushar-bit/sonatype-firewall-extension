/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Named;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.organization.PartialDeletionException;
import com.sonatype.insight.brain.security.ExpiredUserTokenException;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * Extends base error generator to handle additional exceptions like from Shiro.
 *
 * @since 1.7
 */
@Named
public class ErrorResponseGenerator
    extends com.sonatype.insight.jaxrs.error.ErrorResponseGenerator
{
  public static final String MSG_LOGIN_FAILURE_DEFAULT = "Invalid credentials. Please try again.";

  public static final String MSG_MISSING_CREDENTIALS = "Missing credentials.";

  static final String MSG_LDAP_FAILURE =
      "Authentication failed due to LDAP error. Please contact your IT administrator.";

  static final String MSG_LDAP_TIMEOUT = "Authentication failed due to LDAP timeout. Please try again.";

  static final String MSG_JSON_UNPARSABLE = "JSON data could not be parsed.";

  static final String MSG_JSON_UNMAPPABLE = "JSON data does not match expected format.";

  static final String MSG_EXPIRED_TOKEN = "User token has expired. Please generate a new token.";

  @Override
  protected ErrorResponse buildErrorResponse(final Throwable e) {
    if (e instanceof ExpiredUserTokenException) {
      return new ErrorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), MSG_EXPIRED_TOKEN);
    }
    if (e instanceof AuthenticationException) {
      final Throwable cause = e.getCause();
      if (cause instanceof NamingException) {
        if (cause instanceof javax.naming.AuthenticationException || cause instanceof NameNotFoundException) {
          // invalid credentials, fall through to default handling
        }
        else if (String.valueOf(cause.getMessage()).contains("timeout")) {
          // javax.naming.NamingException: LDAP response read timed out, timeout used:30000ms
          return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), MSG_LDAP_TIMEOUT);
        }
        else {
          return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), MSG_LDAP_FAILURE);
        }
      }
      return new ErrorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), MSG_LOGIN_FAILURE_DEFAULT);
    }
    else if (e instanceof UnauthorizedException) {
      return new ErrorResponse(Response.Status.FORBIDDEN.getStatusCode(), null);
    }
    else if (e instanceof UnauthenticatedException) {
      return new ErrorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), null);
    }
    else if (e instanceof JsonProcessingException
        && !(e instanceof JsonGenerationException || e instanceof InvalidDefinitionException))
    {
      String msg = e instanceof JsonMappingException ? MSG_JSON_UNMAPPABLE : MSG_JSON_UNPARSABLE;
      return new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), msg);
    }
    else if (e instanceof PartialDeletionException) {
      return new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
    }
    return super.buildErrorResponse(e);
  }
}
