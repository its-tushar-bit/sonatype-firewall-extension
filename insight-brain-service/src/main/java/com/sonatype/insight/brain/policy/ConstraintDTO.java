/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import static java.util.stream.Collectors.toList;

@JsonInclude(Include.NON_NULL)
public class ConstraintDTO
{
  public final String constraintName;

  public final String conditionQuantifier;

  public List<ConditionDTO> conditions;

  public ConstraintDTO(Constraint constraint) {
    this.constraintName = constraint.getName();
    this.conditionQuantifier = constraint.getOperator() == LogicalOperator.AND ? "all" : "any";
    List<Condition> conditions = constraint.getConditions();
    if (conditions != null) {
      this.conditions = conditions.stream().map(ConditionDTO::new).collect(Collectors.toList());
    }
  }

  public static List<ConstraintDTO> transcribe(List<Constraint> constraints) {
    if (constraints != null) {
      return constraints.stream().map(ConstraintDTO::new).collect(toList());
    }
    return null;
  }
}
