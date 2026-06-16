/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Marks the Guide surface for credit telemetry on {@code /api/v2/guide/*} requests. The SPA sends
 * {@code X-Guide-Client: ui}; its absence on a Guide request means an external API client. The MCP path
 * sets {@code MCP} separately in the servlet. Scoped to Guide paths (this {@code @Provider} otherwise fires
 * globally), and cleared on response so the ThreadLocal never leaks across pooled request threads.
 */
@Named
@Singleton
@Provider
public class GuideChannelRequestFilter
    implements ContainerRequestFilter, ContainerResponseFilter
{
  static final String CLIENT_HEADER = "X-Guide-Client";

  // No leading slash: ContainerRequestContext.getUriInfo().getPath() strips it (see SearchLicenseFilter).
  private static final String GUIDE_API_PATH_PREFIX = "api/v2/guide/";

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (!requestContext.getUriInfo().getPath().startsWith(GUIDE_API_PATH_PREFIX)) {
      return;
    }
    boolean isUi = "ui".equalsIgnoreCase(requestContext.getHeaderString(CLIENT_HEADER));
    GuideChannelContext.set(isUi ? GuideChannel.UI : GuideChannel.API);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    GuideChannelContext.clear();
  }
}
