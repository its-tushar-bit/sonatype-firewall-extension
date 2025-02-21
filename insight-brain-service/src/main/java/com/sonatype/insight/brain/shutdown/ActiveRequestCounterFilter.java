/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Named
@Singleton
public class ActiveRequestCounterFilter
    implements Filter
{
  private static final String SHUTDOWN_PATH = "/tasks/shutdown";

  private final ShutdownHandler shutdownHandler;

  private final LongAdder activeRequestsBeforeShutdown;

  @Inject
  public ActiveRequestCounterFilter(final ShutdownHandler shutdownHandler) {
    this(shutdownHandler, new LongAdder());
  }

  // Visible for testing
  ActiveRequestCounterFilter(final ShutdownHandler shutdownHandler, final LongAdder activeRequestsBeforeShutdown) {
    this.shutdownHandler = shutdownHandler;
    this.activeRequestsBeforeShutdown = activeRequestsBeforeShutdown;
    this.shutdownHandler.add(() -> activeRequestsBeforeShutdown.sum() != 0, ShutdownPriority.ACTIVE_REQUESTS);
  }

  public boolean isShutdownPath(final String path) {
    return path.equals(SHUTDOWN_PATH);
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    String path = ((HttpServletRequest) request).getRequestURI();
    boolean shouldCount = !shutdownHandler.isAfterGracePeriod() && !isShutdownPath(path);
    try {
      if (shouldCount) {
        activeRequestsBeforeShutdown.increment();
      }
      chain.doFilter(request, response);
    }
    finally {
      if (shouldCount) {
        activeRequestsBeforeShutdown.decrement();
      }
    }
  }
}
