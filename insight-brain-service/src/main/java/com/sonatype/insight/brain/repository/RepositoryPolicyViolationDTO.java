/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;

/**
 * @since 1.18.0
 */
public class RepositoryPolicyViolationDTO
{
  public String policyId;

  public String policyName;

  public int policyThreatLevel;

  public List<PolicyConstraint> constraints;

  // Needed for de-serialization
  public RepositoryPolicyViolationDTO() {
  }

  public RepositoryPolicyViolationDTO(final String policyId, final String policyName, final int policyThreatLevel,
      final List<PolicyThreats.PolicyConstraint> constraints) {
    this.policyId = policyId;
    this.policyName = policyName;
    this.policyThreatLevel = policyThreatLevel;
    this.constraints = constraints;
  }
}
