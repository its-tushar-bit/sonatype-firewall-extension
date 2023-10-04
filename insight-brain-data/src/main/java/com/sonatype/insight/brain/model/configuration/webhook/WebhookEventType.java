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
  POLICY_MANAGEMENT("Policy Management", "iq:policyManagement"),
  APPLICATION_EVALUATION("Application Evaluation", "iq:applicationEvaluation"),
  POLICY_ALERT("Violation Alert", "iq:policyAlert"),
  LICENSE_OVERRIDE_MANAGEMENT("License Override Management", "iq:licenseOverrideManagement"),
  SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT("Security Vulnerability Override Management",
      "iq:securityVulnerabilityOverrideManagement"),
  WAIVER_REQUEST("Waiver Request", "iq:waiverRequest"),
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
}
