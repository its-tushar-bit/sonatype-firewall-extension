/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.insight.brain.model.component.Component;

public class MatchFact
{
  private final Component component;

  private final String policyId;

  private final String constraintId;

  private final int conditionNumber;

  public MatchFact(final Component component, final String policyId, final String constraintId) {
    this(component, policyId, constraintId, -1 /* indicates all conditions */);
  }

  public MatchFact(final Component component, final String policyId, final String constraintId,
      final int conditionNumber)
  {
    this.component = component;
    this.policyId = policyId;
    this.constraintId = constraintId;
    this.conditionNumber = conditionNumber;
  }

  public Component getComponent() {
    return component;
  }

  public String getPolicyId() {
    return policyId;
  }

  public String getConstraintId() {
    return constraintId;
  }

  public int getConditionNumber() {
    return conditionNumber;
  }

  @Override
  public String toString() {
    return "(" + policyId + ", " + constraintId + ", " + conditionNumber + ") @ " + component;
  }
}
