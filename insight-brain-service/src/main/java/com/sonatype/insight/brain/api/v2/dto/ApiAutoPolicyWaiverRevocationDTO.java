/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.83
 */
public class ApiAutoPolicyWaiverRevocationDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String autoPolicyWaiverRevocationId;

  @JsonInclude(Include.NON_EMPTY)
  public String ownerId;

  public String creatorId;

  public String creatorName;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date createTime;

  public String autoPolicyWaiverId;

  public String hash;

  public String associatedPackageUrl;

  public String scanId;

  public ComponentMatcherStrategyForRevocation componentMatchStrategy;

  public String policyViolationId;

  public Integer threatLevel;
  
  public String policyName;

  public String componentDisplayName;

  public String vulnerabilityIdentifiers;
}
