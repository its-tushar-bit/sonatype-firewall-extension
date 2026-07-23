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

/**
 * Maps {@link RateLimitedException} to HTTP 429 Too Many Requests with a small structured body. The
 * {@code Retry-After} header is set to a conservative value that gives the client's queue time to drain
 * before retrying.
 */
@Provider
@Named
@Singleton
public class RateLimitedExceptionMapper
    implements ExceptionMapper<RateLimitedException>
{
  /** Retry-After value in seconds. */
  public static final int RETRY_AFTER_SECONDS = 1;

  @Override
  public Response toResponse(RateLimitedException exception) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "rate_limited");
    body.put("code", "GLOBAL_SEARCH_RATE_LIMITED");
    body.put("message", "Too many concurrent Global Search requests");

    return Response.status(429)
        .header("Retry-After", Integer.toString(RETRY_AFTER_SECONDS))
        .header("Cache-Control", "no-store")
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(body)
        .build();
  }
}
