/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;

public class MatchFact
{
  private final Component component;

  private final String policyId;

  private final String constraintId;

  private final int conditionIndex;

  /**
   * The policy waiver that waives this fact.
   * 
   * @since 1.12
   */
  private PolicyWaiver policyWaiver;

  public MatchFact(final Component component, final String policyId, final String constraintId) {
    this(component, policyId, constraintId, -1 /* indicates all conditions */);
  }

  public MatchFact(final Component component,
                   final String policyId,
                   final String constraintId,
                   final int conditionIndex)
  {
    this.component = component;
    this.policyId = policyId;
    this.constraintId = constraintId;
    this.conditionIndex = conditionIndex;
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

  public int getConditionIndex() {
    return conditionIndex;
  }

  @Override
  public String toString() {
    return "(Policy id:" + policyId + ", Constraint id:" + constraintId + ", Condition index:" + conditionIndex
        + ") @ " + component;
  }

  public PolicyWaiver getPolicyWaiver() {
    return policyWaiver;
  }

  public void setPolicyWaiver(PolicyWaiver policyWaiver) {
    this.policyWaiver = policyWaiver;
  }
}
