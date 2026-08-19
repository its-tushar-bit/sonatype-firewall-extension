/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.autowaivers;

import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;

public class ApiAutoPolicyWaiverExclusionRequestDTO
{
  public String applicationPublicId;

  public String ownerId;

  public String scanId;

  public String policyViolationId;

  public String autoPolicyWaiverId;

  public ComponentMatcherStrategyForExclusion matchStrategy;
}
