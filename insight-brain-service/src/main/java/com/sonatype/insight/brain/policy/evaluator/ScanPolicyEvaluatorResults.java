/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class ScanPolicyEvaluatorResults
{
  public PolicyEvaluation evaluation;

  public List<PolicyViolation> allViolations;

  public List<PolicyViolation> activeViolations;

  public List<PolicyViolation> notifiableViolations;

  public List<PolicyViolation> waivedViolations;

  public List<PolicyViolation> autoWaivedViolations;

  public List<PolicyViolation> fixedViolations;
}
