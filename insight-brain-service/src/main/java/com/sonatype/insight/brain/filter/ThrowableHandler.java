/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.io.PrintWriter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

@Named
public class ThrowableHandler
    extends HandlerWrapper
{
  private final JaxRsExceptionMapper jaxRsExceptionMapper;

  @Inject
  public ThrowableHandler(final JaxRsExceptionMapper jaxRsExceptionMapper) {
    this.jaxRsExceptionMapper = jaxRsExceptionMapper;
  }

  @Override
  public void handle(
      String target,
      Request baseRequest,
      HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException
  {
    try {
      super.handle(target, baseRequest, request, response);
    }
    catch (Throwable t) {
      if (!response.isCommitted()) {
        // Note if "t" is an Error, JaxRsExceptionMapper will try to find/log it and then exit if configured to do so
        Response errorResponse = jaxRsExceptionMapper.toResponse(t);
        response.setStatus(errorResponse.getStatus());
        response.setContentType(errorResponse.getMediaType().toString());
        try (PrintWriter printWriter = response.getWriter()) {
          printWriter.print(errorResponse.getEntity());
        }
      }
    }
  }
}
