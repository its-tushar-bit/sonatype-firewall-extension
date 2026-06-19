/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.25.0
 */
public enum WebhookEventType
{
  POLICY_MANAGEMENT("Policy Management", "iq:policyManagement"),
  APPLICATION_EVALUATION("Application Evaluation", "iq:applicationEvaluation"),
  POLICY_ALERT("Violation Alert", "iq:policyAlert"),
  FIREWALL_POLICY_ALERT("Firewall Violation Alert", "iq:firewallPolicyAlert"),
  LICENSE_OVERRIDE_MANAGEMENT("License Override Management", "iq:licenseOverrideManagement"),
  SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT("Security Vulnerability Override Management",
      "iq:securityVulnerabilityOverrideManagement"),
  WAIVER_REQUEST("Waiver Request", "iq:waiverRequest"),
  FIREWALL_WAIVER_REQUEST("Firewall Waiver Request", "iq:firewallWaiverRequest"),
  WAIVER_EXPIRATION("Waiver Expiration", "iq:waiverExpiration"),
  ORG_APP_MANAGEMENT("Organization and Application Management", "iq:orgAppManagement");

  private final String displayName;

  private final String id;

  WebhookEventType(String displayName, String id) {
    this.displayName = displayName;
    this.id = id;
  }

  @JsonValue
  public String getDisplayName() {
    return this.displayName;
  }

  public String getId() {
    return this.id;
  }

  /**
   * Custom deserializer to handle both original and contextual display names.
   * Firewall context uses contextual names like "Container Evaluation" instead of "Application Evaluation"
   * and "Organization and Repository Management" instead of "Organization and Application Management".
   *
   * Returns null for unknown display names to support upgrade/downgrade paths gracefully.
   * This prevents deserialization failures when older versions encounter webhook event types
   * added in newer versions.
   *
   * @param displayName the display name from JSON (original or contextual)
   * @return the corresponding WebhookEventType enum value, or null if not recognized
   */
  @JsonCreator
  public static WebhookEventType fromDisplayName(String displayName) {
    // Handle null gracefully for upgrade/downgrade compatibility
    if (displayName == null) {
      return null;
    }

    // Try to find by original display name first
    for (WebhookEventType type : values()) {
      if (type.displayName.equals(displayName)) {
        return type;
      }
    }

    // Handle contextual Firewall display names
    switch (displayName) {
      case "Container Evaluation":
        return APPLICATION_EVALUATION;
      case "Organization and Repository Management":
        return ORG_APP_MANAGEMENT;
      default:
        // Return null for unknown values to support graceful degradation during version downgrades
        return null;
    }
  }
}
