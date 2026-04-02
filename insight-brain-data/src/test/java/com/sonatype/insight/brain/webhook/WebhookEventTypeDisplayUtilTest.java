/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link WebhookEventTypeDisplayUtil}.
 */
public class WebhookEventTypeDisplayUtilTest
{
  @Test
  public void testGetContextualDisplayName_LifecycleContext() {
    // Lifecycle context should return standard display names
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.APPLICATION_EVALUATION, false), is("Application Evaluation"));
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.ORG_APP_MANAGEMENT, false), is("Organization and Application Management"));
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.WAIVER_EXPIRATION, false), is("Waiver Expiration"));
  }

  @Test
  public void testGetContextualDisplayName_FirewallContext() {
    // Firewall context should return context-specific display names
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.APPLICATION_EVALUATION, true), is("Container Evaluation"));
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.ORG_APP_MANAGEMENT, true), is("Organization and Repository Management"));
    // Waiver expiration is the same in both contexts
    assertThat(WebhookEventTypeDisplayUtil.getContextualDisplayName(
        WebhookEventType.WAIVER_EXPIRATION, true), is("Waiver Expiration"));
  }
}
