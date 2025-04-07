/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiPolicyWaiverRequestReviewDTO
{
  // Can be APPROVED or REJECTED
  public String status;

  public String rejectionReason;

  public String comment;

  public ComponentMatcherStrategyForWaiver matcherStrategy;

  @ApiDateFormat
  public Date expiryTime;

  @JsonInclude(Include.NON_EMPTY)
  public String waiverReasonId;

  public boolean expireWhenRemediationAvailable;

  public ApiPolicyWaiverRequestReviewDTO() {
  }

  public ApiPolicyWaiverRequestReviewDTO(
      String comment,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      Date expiryTime,
      String waiverReasonId,
      boolean expireWhenRemediationAvailable,
      String status)
  {
    this.comment = comment;
    this.matcherStrategy = matcherStrategy;
    this.expiryTime = expiryTime;
    this.waiverReasonId = waiverReasonId;
    this.expireWhenRemediationAvailable = expireWhenRemediationAvailable;
    this.status = status;
  }
}
