/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Provider
@Priority(ShutdownContainerRequestFilter.PRIORITY)
@Order(Integer.MAX_VALUE - ShutdownContainerRequestFilter.PRIORITY)
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

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    if (shutdownHandler.isTriggered()) {
      requestContext.abortWith(Response.status(Status.SERVICE_UNAVAILABLE).build());
    }
  }
}
