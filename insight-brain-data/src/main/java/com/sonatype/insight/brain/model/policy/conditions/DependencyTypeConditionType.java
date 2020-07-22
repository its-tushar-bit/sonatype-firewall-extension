/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.DependencyTypeValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.96
 */
public class DependencyTypeConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "DependencyType";

  private static final List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  @Override
  protected boolean internalEvaluateCondition(
      final Component component, final String operator, final String value)
  {
    if (component.getDirectDependency() == null) {
      return false;
    }

    boolean result = value.equals(
        component.getDirectDependency() ? DependencyType.DIRECT.getId() : DependencyType.TRANSITIVE.getId());
    return "is".equals(operator) ? result : !result;
  }

  @Override
  protected String generateDroolsConditionValue(
      final TransactionContext tx, final String value)
  {
    return asDroolsString(value);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Dependency Type";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return DependencyTypeValueType.ID;
  }

  @Override
  public String explainMatch(
      final Condition condition, final MatchFact matchFact)
  {
    return "Dependency type was " +
        (matchFact.getComponent().getDirectDependency() ? DependencyType.DIRECT.getName() : DependencyType.TRANSITIVE
            .getName()) +
        ("is not".equals(condition.getOperator()) ?
            ", not " + DependencyType.getById(condition.getValue()).getName() : "");
  }

  @Override
  public void validateCondition(final TransactionContext tx, final Condition condition, final String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    if (DependencyType.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }
}
