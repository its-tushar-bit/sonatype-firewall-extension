/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.ws.rs.core.UriBuilder;

/**
 * Predicate-like helper for excluding user telemetry requests from request logging.
 *
 * @since 1.50
 */
public class UserTelemetryRequestLoggingFilter
{
  private final String uriToFilter = "/"
      + UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH).path(UserTelemetryResource.EVENTS).toString();

  public boolean shouldSkip(final String requestUri) {
    return requestUri != null && (requestUri.equals(uriToFilter) || requestUri.startsWith(uriToFilter + "/"));
  }
}
