/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.List;

public class Constraint
{
  private String id;

  private String name;

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

  @Override
  public String toString() {
    return "Constraint [id=" + id + ", name=" + name + ", operator=" + operator + "]";
  }
}
