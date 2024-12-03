/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.json.store.ApiDateFormat;

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

  @ApiDateFormat
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
}
