/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/**
 * Request-scoped holder for the {@link GuideChannel} of the in-flight Guide call. Set by the REST
 * filter (UI/API) or the MCP servlet (MCP) at request entry and cleared in a finally block. Read by
 * the credit-telemetry aspect at the SearchApiClient convergence point, which sits below the surface
 * split and so cannot otherwise tell the surfaces apart.
 */
public final class GuideChannelContext
{
  private static final ThreadLocal<GuideChannel> CHANNEL = new ThreadLocal<>();

  private GuideChannelContext() {
  }

  public static void set(final GuideChannel channel) {
    CHANNEL.set(channel);
  }

  /** Defaults to {@link GuideChannel#API} when unset (plain REST API call with no UI marker). */
  public static GuideChannel getOrDefault() {
    GuideChannel channel = CHANNEL.get();
    return channel == null ? GuideChannel.API : channel;
  }

  public static void clear() {
    CHANNEL.remove();
  }
}
