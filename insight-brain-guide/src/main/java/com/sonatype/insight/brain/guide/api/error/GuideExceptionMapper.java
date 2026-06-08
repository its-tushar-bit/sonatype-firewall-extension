/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@Provider
public class GuideExceptionMapper
    implements ExceptionMapper<GuideApiException>
{

  private static final Logger log = LoggerFactory.getLogger(GuideExceptionMapper.class);

  @Context
  private HttpServletRequest request;

  @Override
  public Response toResponse(GuideApiException exception) {
    int status = exception.getResponse().getStatus();
    return GuideErrorResponses.build(status, exception, request, log);
  }
}
