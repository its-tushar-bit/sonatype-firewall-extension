/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.json.store.ISODateSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Options for creating policy waivers")
public class ApiWaiverOptionsDTO
{
  @Schema(
      description = "Reason for waiving the violation(s). Must be non-blank.",
      required = true,
      example = "False positive - internal tool approved by security team")
  public String comment;

  /**
   * This field is being deprecated in favor of "matcherStrategy"
   *
   * @deprecated use {@link #matcherStrategy}
   */
  @Deprecated
  @Schema(hidden = true)
  public boolean applyToAllComponents;

  /**
   * @since 1.140
   */
  @Schema(
      description = "Component matching strategy. For Firewall bulk waivers, only EXACT_COMPONENT and ALL_VERSIONS are supported.",
      required = true,
      allowableValues = {"EXACT_COMPONENT", "ALL_VERSIONS"},
      example = "EXACT_COMPONENT")
  public ComponentMatcherStrategyForWaiver matcherStrategy;

  @JsonSerialize(using = ISODateSerializer.class)
  @Schema(
      description = "Optional expiration date/time for the waiver in ISO 8601 format. Must be in the future if provided.",
      required = false)
  public Date expiryTime;

  @Schema(
      description = "Optional reference to a pre-defined waiver reason ID",
      required = false,
      example = "waiver-reason-id-123")
  public String waiverReasonId;

  /**
   * @since 1.185
   */
  @Schema(
      description = "If true, the waiver will automatically expire when a remediation becomes available. " +
          "Can only be set to true when matcherStrategy is EXACT_COMPONENT.",
      required = false,
      example = "false")
  public boolean expireWhenRemediationAvailable;

  public ApiWaiverOptionsDTO() {
  }

  public ApiWaiverOptionsDTO(
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String waiverReasonId,
      boolean expireWhenRemediationAvailable)
  {
    this.comment = comment;
    this.matcherStrategy = matcherStrategy;
    this.expiryTime = expiryTime;
    this.waiverReasonId = waiverReasonId;
    this.expireWhenRemediationAvailable = expireWhenRemediationAvailable;
  }

  public ApiWaiverOptionsDTO(PolicyWaiver policyWaiver) {
    this.comment = policyWaiver.getComment();
    this.matcherStrategy = policyWaiver.getComponentMatchStrategy();
    this.expiryTime = policyWaiver.getExpiryTime();
    this.waiverReasonId = policyWaiver.getWaiverReasonId();
    this.expireWhenRemediationAvailable = policyWaiver.isExpireWhenRemediationAvailable();
  }
}
