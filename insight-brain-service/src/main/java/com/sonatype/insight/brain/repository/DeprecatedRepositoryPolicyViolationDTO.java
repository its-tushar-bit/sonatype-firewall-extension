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
 * @deprecated Use {@link RepositoryPolicyViolationDTO} instead
 * 
 * @since 1.18.0
 */
@Deprecated
public class DeprecatedRepositoryPolicyViolationDTO
{
  public String policyId;

  public String policyName;

  public int policyThreatLevel;

  public List<PolicyConstraint> constraints;

  public boolean blocksUnquarantine;

  /**
   * @since 1.53
   */
  public String constraintFactsJson;

  // Needed for de-serialization
  public DeprecatedRepositoryPolicyViolationDTO() {
  }

  public DeprecatedRepositoryPolicyViolationDTO(final String policyId,
                                      final String policyName,
                                      final int policyThreatLevel,
                                      final boolean blocksUnquarantine,
                                      final List<PolicyThreats.PolicyConstraint> constraints,
                                      final String constraintFactsJson)
  {
    this.policyId = policyId;
    this.policyName = policyName;
    this.policyThreatLevel = policyThreatLevel;
    this.constraints = constraints;
    this.blocksUnquarantine = blocksUnquarantine;
    this.constraintFactsJson = constraintFactsJson;
  }
}
