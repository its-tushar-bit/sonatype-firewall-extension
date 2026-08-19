/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;

/**
 * Utility class for webhook event type display name conversion.
 * Provides context-aware display names for webhook event types based on Lifecycle vs Firewall context.
 */
public final class WebhookEventTypeDisplayUtil
{
  private WebhookEventTypeDisplayUtil() {
    // Utility class - no instantiation
  }

  /**
   * Returns the context-specific display name for a webhook event type.
   * Firewall context uses different terminology than Lifecycle context.
   *
   * @param eventType the webhook event type
   * @param isFirewallContext true if the context is Firewall, false for Lifecycle
   * @return the appropriate display name for the given context
   */
  public static String getContextualDisplayName(WebhookEventType eventType, boolean isFirewallContext) {
    if (isFirewallContext) {
      switch (eventType) {
        case APPLICATION_EVALUATION:
          return "Container Evaluation";
        case ORG_APP_MANAGEMENT:
          return "Organization and Repository Management";
        default:
          return eventType.getDisplayName();
      }
    }
    return eventType.getDisplayName();
  }
}
