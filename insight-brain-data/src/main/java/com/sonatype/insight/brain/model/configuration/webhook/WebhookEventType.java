/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.webhook;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.25.0
 */
public enum WebhookEventType
{
  POLICY_MANAGEMENT("Policy Management"),
  APPLICATION_EVALUATION("Application Evaluation"),
  POLICY_ALERT("Violation Alert"),
  LICENSE_OVERRIDE_MANAGEMENT("License Override Management"),
  SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT("Security Vulnerability Override Management");

  private String displayName;

  WebhookEventType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return this.displayName;
  }
}
