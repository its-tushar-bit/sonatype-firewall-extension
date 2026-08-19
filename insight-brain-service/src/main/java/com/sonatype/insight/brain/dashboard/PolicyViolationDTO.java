/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;

/**
 * @since 1.11.0
 */
public class PolicyViolationDTO
{
  public String applicationId;

  public String applicationName;

  /**
   * @since 1.13.0
   */
  public ComponentIdentifier componentIdentifier;

  public String hash;

  public String id;

  public String policyId;

  public String policyName;

  public PolicyThreatCategory threatCategory;

  public int threatLevel;

  public long time;

  public String filename;

  public List<ConstraintFact> constraintFacts;

  public String computeUniqueAppPolicyConstraintId() {
    return PolicyViolationComparator.computeUniqueAppPolicyConstraintId(applicationId, policyId, constraintFacts);
  }
}
