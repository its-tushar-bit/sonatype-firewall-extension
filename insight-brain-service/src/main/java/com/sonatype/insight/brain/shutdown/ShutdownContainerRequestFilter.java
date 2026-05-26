/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import datadog.trace.api.Trace;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Named
@Provider
@Priority(ShutdownContainerRequestFilter.PRIORITY)
public class ShutdownContainerRequestFilter
    implements ContainerRequestFilter
{
  // high priority (i.e. low number) to get called before others like LicenseAwareContainerDynamicFeature
  static final int PRIORITY = Priorities.AUTHENTICATION / 3;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public ShutdownContainerRequestFilter(final ShutdownHandler shutdownHandler) {
    this.shutdownHandler = shutdownHandler;
  }

  @Trace
  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    if (shutdownHandler.isAfterGracePeriod()) {
      requestContext.abortWith(Response.status(Status.SERVICE_UNAVAILABLE).build());
    }
  }
}
