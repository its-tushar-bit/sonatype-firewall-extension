/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;

public class MatchFact
{
  private final Component component;

  private final String policyId;

  private final String constraintId;

  private final int conditionIndex;

  /**
   * Data that triggered the condition(s) for this MatchFact. These can be security vulnerabilities, license thread
   * groups, licenses, etc. (When a condition is evaluated to true, it can return some data that explains why the
   * condition was triggered.)
   */
  private final List<ConditionTrigger> conditionTriggers;

  public MatchFact(
      final Component component,
      final String policyId,
      final String constraintId,
      final List<ConditionTrigger> conditionTriggers)
  {
    this(component, policyId, constraintId, -1 /* indicates all conditions */, conditionTriggers);
  }

  public MatchFact(
      final Component component,
      final String policyId,
      final String constraintId,
      final int conditionIndex,
      final List<ConditionTrigger> conditionTriggers)
  {
    this.component = component;
    this.policyId = policyId;
    this.constraintId = constraintId;
    this.conditionIndex = conditionIndex;
    this.conditionTriggers = conditionTriggers;
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
        + ", Condition triggers: " + conditionTriggers + ") @ " + component;
  }

  public List<ConditionTrigger> getConditionTriggers() {
    return conditionTriggers;
  }

  public ConditionTrigger getConditionTriggerByConditionIndex(int conditionIndex) {
    return getConditionTriggers().stream()
        .filter(x -> x.getConditionIndex() == conditionIndex)
        .findFirst()
        .orElse(null);
  }
}
