/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.dataaccess.TransactionContext;

public class Constraint
{
  private String id;

  private String name;

  private boolean enabled = true;

  private LogicalOperator operator = LogicalOperator.AND;

  private List<Condition> conditions;

  public Constraint() {
  }

  public Constraint(final String id, final String name, final LogicalOperator operator) {
    this.id = id;
    this.name = name;
    if (operator != null) {
      this.operator = operator;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(final List<Condition> conditions) {
    if (conditions != null) {
      for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
        conditions.get(conditionIndex).setConditionIndex(conditionIndex);
      }
    }
    this.conditions = conditions;
  }

  public void addCondition(final Condition condition) {
    if (conditions == null) {
      conditions = new ArrayList<>();
    }
    condition.setConditionIndex(conditions.size());
    conditions.add(condition);
  }

  public ValidationResult validate(TransactionContext tx, String ownerId) {
    ValidationResult result = new ValidationResult();
    if (name == null || name.trim().isEmpty()) {
      result.addError("The constraint name must not be null or empty");
    }
    if (conditions == null || conditions.isEmpty()) {
      result.addError("Constraint '" + name + "' has no conditions");
      return result;
    }

    ValidationResult conditionsResult = new ValidationResult();
    for (Condition condition : conditions) {
      conditionsResult.merge(condition.validate(tx, ownerId));
    }

    if (!conditionsResult.isValid()) {
      result.addError("Constraint '" + name + "' has invalid conditions:");
      result.merge(conditionsResult);
    }

    return result;
  }

  @Override
  public String toString() {
    return "Constraint [id=" + id + ", name=" + name + ", enabled=" + enabled + ", operator=" + operator + "]";
  }
}
