/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.ws.rs.core.Response;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * Extends base error generator to handle additional exceptions like from Shiro.
 * 
 * @since 1.7
 */
public class ErrorResponseGenerator
    extends com.sonatype.insight.error.ErrorResponseGenerator
{
  public ErrorResponseGenerator() {
    super(false);
  }

  @Override
  protected int getStatusCode(Throwable e) {
    if (e instanceof UnauthorizedException) {
      return Response.Status.FORBIDDEN.getStatusCode();
    }
    else if (e instanceof UnauthenticatedException) {
      return Response.Status.UNAUTHORIZED.getStatusCode();
    }
    return super.getStatusCode(e);
  }
}
