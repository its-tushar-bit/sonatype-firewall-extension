/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.PercentageValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class RelativePopularityConditionType
    extends AbstractComponentConditionType<Integer>
{
  public static final String ID = "RelativePopularity";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Relative Popularity (Percentage)";
  }

  @Override
  public List<String> getSupportedOperators() {
    return NumericOperators.LIST;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsInteger(value);
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    boolean isEqualsOperator = "=".equals(condition.getOperator());
    return "Relative popularity was " + (!isEqualsOperator ? condition.getOperator() + " " : "") +
        condition.getValue() + "%" +
        (!isEqualsOperator ? " (relative popularity = " + matchFact.getComponent().getRelativePopularity() + "%)" : "");
  }

  @Override
  public String getValueTypeId() {
    return PercentageValueType.ID;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    try {
      int value = Integer.parseInt(condition.getValue());
      if (value < 0 || value > 100) {
        throw new InvalidConditionException(condition, "Relative popularity must be between 0 and 100");
      }
    }
    catch (NumberFormatException e) {
      throw new InvalidConditionException(condition, "Invalid relative popularity: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getValueHint() {
    return "Enter percent value, 1 to 100";
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, Integer value) {
    // Component without relative popularity should always pass this condition. See
    // https://issues.sonatype.org/browse/CLM-3979 and https://issues.sonatype.org/browse/CLM-4403.
    if (component.getRelativePopularity() == null) {
      return false;
    }

    if ("=".equals(operator)) {
      return component.getRelativePopularity() == value;
    }
    if ("<".equals(operator)) {
      return component.getRelativePopularity() < value;
    }
    if ("<=".equals(operator)) {
      return component.getRelativePopularity() <= value;
    }
    if (">".equals(operator)) {
      return component.getRelativePopularity() > value;
    }
    return component.getRelativePopularity() >= value;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }
}
