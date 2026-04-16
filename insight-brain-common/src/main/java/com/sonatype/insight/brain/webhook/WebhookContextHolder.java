/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

/**
 * ThreadLocal holder for webhook context (lifecycle/firewall).
 * Used to provide context-aware serialization of webhook event type display names.
 */
public class WebhookContextHolder
{
  private static final ThreadLocal<String> context = new ThreadLocal<>();

  private WebhookContextHolder() {
    // Utility class
  }

  /**
   * Set the current webhook context for this thread.
   *
   * @param ctx the context ("lifecycle" or "firewall")
   */
  public static void setContext(String ctx) {
    context.set(ctx);
  }

  /**
   * Get the current webhook context for this thread.
   *
   * @return the context, or null if not set
   */
  public static String getContext() {
    return context.get();
  }

  /**
   * Clear the webhook context for this thread.
   * Should be called in a finally block to prevent memory leaks.
   */
  public static void clear() {
    context.remove();
  }
}
