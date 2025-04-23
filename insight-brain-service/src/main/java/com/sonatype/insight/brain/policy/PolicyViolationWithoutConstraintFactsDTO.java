/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

public class PolicyViolationWithoutConstraintFactsDTO
{
  public String applicationId;

  public String applicationName;

  public ComponentIdentifier componentIdentifier;

  public String hash;

  public String id;

  public String policyId;

  public String policyName;

  public PolicyThreatCategory threatCategory;

  public int threatLevel;

  public long time;

  public String filename;
}
