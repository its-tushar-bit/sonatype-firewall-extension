/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.autowaivers;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.83
 */
public class ApiAutoPolicyWaiverExclusionResponseDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String autoPolicyWaiverExclusionId;

  @JsonInclude(Include.NON_EMPTY)
  public String ownerId;

  public String creatorId;

  public String creatorName;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date createTime;

  public String autoPolicyWaiverId;

  public String ownerName;

  public String ownerPublicId;

  public String ownerType;

  public String hash;

  public String scanId;

  public ComponentMatcherStrategyForExclusion componentMatchStrategy;

  public String policyViolationId;

  public Integer threatLevel;

  public String policyName;

  public String componentDisplayName;

  public ComponentIdentifier componentIdentifier;

  public String vulnerabilityIdentifiers;

  public String policyId;

  public List<ConstraintFact> constraintFacts;
}
