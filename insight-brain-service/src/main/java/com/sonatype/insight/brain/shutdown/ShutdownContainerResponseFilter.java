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
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import datadog.trace.api.Trace;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Provider
@Priority(ShutdownContainerResponseFilter.PRIORITY)
@Order(Integer.MAX_VALUE - ShutdownContainerResponseFilter.PRIORITY)
public class ShutdownContainerResponseFilter
    implements ContainerResponseFilter
{
  static final int PRIORITY = Priorities.HEADER_DECORATOR;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public ShutdownContainerResponseFilter(final ShutdownHandler shutdownHandler) {
    this.shutdownHandler = shutdownHandler;
  }

  @Trace
  @Override
  public void filter(
      final ContainerRequestContext containerRequestContext,
      final ContainerResponseContext containerResponseContext)
      throws IOException
  {
    if (shutdownHandler.isTriggered()) {
      // Override HTTP/1.1 default `keep-alive`, telling client to make new connection, which can route to new pods.
      containerResponseContext.getHeaders().add("Connection", "close");
    }
  }
}
