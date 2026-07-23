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
 * Maps {@link StaleCursorException} to HTTP 410 Gone with a structured JSON body and a
 * {@code X-Global-Search-Retry} hint header so the frontend can re-issue the request from page 1 cleanly.
 */
@Provider
@Named
@Singleton
public class StaleCursorExceptionMapper
    implements ExceptionMapper<StaleCursorException>
{
  /** Header set on the 410 response with the suggested recovery action. */
  public static final String RETRY_HINT_HEADER = "X-Global-Search-Retry";

  /** Value of {@link #RETRY_HINT_HEADER}: callers should drop the cursor and request page 1. */
  public static final String RETRY_HINT_VALUE = "retry-from-page-1";

  @Override
  public Response toResponse(StaleCursorException exception) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "stale_cursor");
    body.put("code", "GLOBAL_SEARCH_CURSOR_STALE");
    body.put("message", exception.getMessage());
    body.put("retryHint", RETRY_HINT_VALUE);

    return Response.status(Response.Status.GONE)
        .header(RETRY_HINT_HEADER, RETRY_HINT_VALUE)
        .header("Cache-Control", "no-store")
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(body)
        .build();
  }
}
