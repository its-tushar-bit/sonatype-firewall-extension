/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.webhook;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sonatype.insight.brain.webhook.WebhookContextHolder;
import com.sonatype.insight.brain.webhook.WebhookEventTypeDisplayUtil;

/**
 * Custom JSON serializer for webhook event types that provides context-aware display names.
 * Uses WebhookContextHolder to determine whether to use Lifecycle or Firewall display names.
 */
public class WebhookEventTypeSetSerializer
    extends JsonSerializer<Set<WebhookEventType>>
{
  @Override
  public void serialize(
      Set<WebhookEventType> value,
      JsonGenerator gen,
      SerializerProvider provider) throws IOException
  {
    // Get context from ThreadLocal (set by REST resource)
    // Default to "lifecycle" if not set for backward compatibility
    String context = WebhookContextHolder.getContext();
    if (context == null) {
      context = "lifecycle";
    }
    boolean isFirewallContext = "firewall".equalsIgnoreCase(context);

    gen.writeStartArray();
    if (value != null) {
      for (WebhookEventType eventType : value) {
        String displayName = WebhookEventTypeDisplayUtil.getContextualDisplayName(eventType, isFirewallContext);
        gen.writeString(displayName);
      }
    }
    gen.writeEndArray();
  }
}
