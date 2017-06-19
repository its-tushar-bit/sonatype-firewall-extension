/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

public class Condition
{
  private String conditionTypeId;

  private String operator;

  private String value;

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

  public ValidationResult validate(TransactionContext tx, String ownerId) {
    ConditionType conditionType = ConditionTypes.getById(conditionTypeId);
    if (conditionType == null) {
      return new ValidationResult("Invalid condition type id: '" + conditionTypeId + "'");
    }

    try {
      conditionType.validateCondition(tx, this, ownerId);
    }
    catch (InvalidConditionException e) {
      return new ValidationResult(e);
    }

    return ValidationResult.noErrors();
  }

  public String toMessageString() {
    return conditionTypeId + ' ' + operator + ' ' + value;
  }

  @Override
  public String toString() {
    return "Condition [conditionTypeId=" + conditionTypeId + ", operator=" + operator + ", value=" + value + "]";
  }
}
