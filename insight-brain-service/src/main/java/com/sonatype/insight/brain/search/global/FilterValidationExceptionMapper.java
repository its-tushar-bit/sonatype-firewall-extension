/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps {@link FilterValidationException} to a 400 JSON body {@code {code, message}} where
 * {@code message} is the static, input-free {@link FilterValidationException.Code#clientMessage()}.
 * The exception's input-bearing {@code detail} is logged server-side only, never returned in the
 * body — clients key off the machine-readable {@code code}.
 */
@Provider
@Named
@Singleton
public class FilterValidationExceptionMapper
    implements ExceptionMapper<FilterValidationException>
{
  private static final Logger log = LoggerFactory.getLogger(FilterValidationExceptionMapper.class);

  @Override
  public Response toResponse(final FilterValidationException exception) {
    // info, not debug: these are client-side rejections that must be diagnosable in prod without
    // enabling debug. getDetail is the server-side input-bearing field and stays out of the client body.
    log.info("Filter validation rejected [{}]: {}", exception.getCode(), exception.getDetail());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", exception.getCode().name());
    body.put("message", exception.getCode().clientMessage());

    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(body)
        .build();
  }
}
