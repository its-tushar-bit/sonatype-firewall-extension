/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class PolicyViolationDiff
{
  private final List<PolicyViolation> appeared = new ArrayList<>();

  private final List<PolicyViolation> cleared = new ArrayList<>();

  public List<PolicyViolation> getAppeared() {
    return appeared;
  }

  public void addAppeared(PolicyViolation policyViolation) {
    appeared.add(policyViolation);
  }

  public void addAppeared(List<PolicyViolation> policyViolations) {
    appeared.addAll(policyViolations);
  }

  public List<PolicyViolation> getCleared() {
    return cleared;
  }

  public void addCleared(PolicyViolation policyViolation) {
    cleared.add(policyViolation);
  }
}
