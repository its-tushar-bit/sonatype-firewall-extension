/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

public class ConditionTrigger
{
  /**
   * The condition index (or number) in the policy constraint. It matches the {@link MatchFact.conditionNumber}.
   */
  private int conditionIndex;

  /**
   * The actual condition trigger. It can be a security vulnerability, license threat group, etc.
   */
  private Object trigger;

  public ConditionTrigger() {
  }

  public ConditionTrigger(int conditionIndex, Object trigger) {
    this.conditionIndex = conditionIndex;
    this.trigger = trigger;
  }

  public int getConditionIndex() {
    return conditionIndex;
  }

  public Object getTrigger() {
    return trigger;
  }

  @Override
  public String toString() {
    return "ConditionTrigger [conditionIndex=" + conditionIndex + ", trigger=" + trigger + "]";
  }
}
