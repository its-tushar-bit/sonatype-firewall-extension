/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

public class Condition
{
  private String conditionTypeId;

  private String operator;

  private String value;

  /**
   * The condition index in the policy constraint.
   *
   * @since 1.50
   */
  private int conditionIndex;

  public Condition() {
  }

  public Condition(final String conditionTypeId, final String operator) {
    this.conditionTypeId = conditionTypeId;
    this.operator = operator;
  }

  public Condition(final String conditionTypeId, final String operator, final String value) {
    this.conditionTypeId = conditionTypeId;
    this.operator = operator;
    this.value = value;
  }

  public String getConditionTypeId() {
    return conditionTypeId;
  }

  public String getOperator() {
    return operator;
  }

  public String getValue() {
    return ConditionTypes.getById(conditionTypeId).convertIfNeeded(value);
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public String toMessageString() {
    return conditionTypeId + ' ' + operator + ' ' + value;
  }

  @Override
  public String toString() {
    return "Condition [conditionTypeId=" + conditionTypeId + ", operator=" + operator + ", value=" + value + ", index="
        + conditionIndex + "]";
  }

  public int getConditionIndex() {
    return conditionIndex;
  }

  public void setConditionIndex(int conditionIndex) {
    this.conditionIndex = conditionIndex;
  }
}
