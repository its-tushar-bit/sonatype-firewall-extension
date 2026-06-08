/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import jakarta.ws.rs.core.Response;

/**
 * Thrown by SearchApiClient when the upstream HDS data service responds with 404. Carries the
 * upstream's message verbatim so the client-facing 404 body matches Guide SaaS exactly (where
 * the same message is propagated from the search-server through Spring's NotFoundException).
 *
 * <p>
 * Resources should let this propagate to {@link GuideExceptionMapper}; non-HTTP callers
 * (e.g. the MCP servlet) may catch it explicitly to produce a "no data" result instead of an
 * error.
 */
public class GuideNotFoundException
    extends GuideApiException
{
  public GuideNotFoundException(String message) {
    super(Response.Status.NOT_FOUND, message);
  }
}
