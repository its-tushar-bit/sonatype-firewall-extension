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

public class ApiWaiverOptionsDTO
{
  public String comment;

  /**
   * This field is being deprecated in favor of "matcherStrategy"
   *
   * @deprecated use {@link #matcherStrategy}
   */
  @Deprecated
  public boolean applyToAllComponents;

  /**
   * @since 1.140
   */
  public ComponentMatcherStrategyForWaiver matcherStrategy;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date expiryTime;

  public String waiverReasonId;

  /**
   * @since 1.185
   */
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
