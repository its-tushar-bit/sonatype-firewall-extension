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
 * Maps the {@link GlobalSearchNotConfiguredException} thrown by {@link GlobalSearchResultsIqLocalClientStub}
 * to HTTP 503 with a stable operational error body. Scoped to that dedicated type so genuine programmer
 * errors elsewhere reach the default 500 handler untouched.
 */
@Provider
@Named
@Singleton
public class GlobalSearchResultsIqLocalClientStubMapper
    implements ExceptionMapper<GlobalSearchNotConfiguredException>
{
  @Override
  public Response toResponse(GlobalSearchNotConfiguredException exception) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "service_unavailable");
    body.put("code", "GLOBAL_SEARCH_NOT_CONFIGURED");
    body.put("message", exception.getMessage());
    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(body)
        .build();
  }
}
