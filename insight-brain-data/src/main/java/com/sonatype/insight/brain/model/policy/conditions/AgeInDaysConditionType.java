/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.AgeInDaysValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class AgeInDaysConditionType
    extends AbstractComponentConditionType<Integer>
{
  public static final String ID = "AgeInDays";

  public static final long DAY_IN_MILLISECONDS = 24L * 3600L * 1000L;

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("older than");
    supportedOperators.add("younger than");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Age";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsInteger(value) + asDroolsComment("days");
  }

  @Override
  public String explainCondition(Condition condition) {
    return getName() + ' ' + condition.getOperator() + ' ' + condition.getValue() + " days";
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    final int days = Integer.parseInt(condition.getValue());
    final String conditionAge = days % 365 == 0 ?
        days / 365 + " years" :
        days % 30 == 0 ? days / 30 + " months" : days % 7 == 0 ? days / 7 + " weeks" : days + " days";
    return "Found component " + condition.getOperator() + " " + conditionAge;
  }

  @Override
  public String getValueTypeId() {
    return AgeInDaysValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    try {
      Integer.parseInt(condition.getValue());
    }
    catch (NumberFormatException e) {
      throw new InvalidConditionException(condition, "Invalid age (in days): " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return true;
  }

  @Override
  public String getValueHint() {
    return "Enter term";
  }

  @Override
  public boolean internalEvaluateCondition(Component component, String operator, Integer value) {
    if (component.getCatalogDate() == null || component.getCatalogDate() == 0L) {
      return false;
    }
    int ageInDays = (int) ((System.currentTimeMillis() - component.getCatalogDate()) / DAY_IN_MILLISECONDS);
    if ("older than".equals(operator)) {
      return ageInDays > value;
    }
    else {
      return ageInDays <= value;
    }
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.QUALITY;
  }
}
