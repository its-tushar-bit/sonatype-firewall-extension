/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IntegerValueType;
import com.sonatype.insight.dataaccess.TransactionContext;

public class LicenseThreatGroupLevelConditionType
    extends AbstractConditionType
{
  public static final String ID = "License Threat Group Level";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("<=");
    supportedOperators.add(">=");
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return IntegerValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    try {
      int value = Integer.parseInt(condition.getValue());
      if (value < 0 || value > 10) {
        throw new InvalidConditionException(condition, "The license threat group level must be between 0 and 10");
      }
    }
    catch (NumberFormatException e) {
      throw new InvalidConditionException(condition, "Invalid license threat group level: " + condition.getValue());
    }
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "License Threat Group Level";
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsInteger(value);
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    final StringBuilder buf = new StringBuilder();
    final Set<LicenseThreatGroup> licenseThreatGroups = getLicenseThreatGroupsByLevel(component,
        Integer.valueOf(condition.getValue()), condition.getOperator());
    if (licenseThreatGroups.isEmpty()) {
      buf.append("no");
    }
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if (buf.length() > 0) {
        buf.append(" and ");
      }
      buf.append('\'').append(licenseThreatGroup.getName()).append('\'');
    }
    return "Found " + buf + " License Threat " + (licenseThreatGroups.size() != 1 ? "Groups" : "Group")
        + " with Level " + condition.getOperator() + " " + condition.getValue();
  }

  public boolean evaluateCondition(LicenseThreatGroup licenseThreatGroup,
                                   String operator,
                                   Integer licenseThreatGroupLevel)
  {
    if (">=".equals(operator)) {
      return licenseThreatGroup.getThreatLevel() >= licenseThreatGroupLevel;
    }
    if ("<=".equals(operator)) {
      return licenseThreatGroup.getThreatLevel() <= licenseThreatGroupLevel;
    }
    throw new IllegalArgumentException("Unsupported condition operator:" + operator);
  }

  private Set<LicenseThreatGroup> getLicenseThreatGroupsByLevel(Component component, int threatLevel, String operator) {
    Set<LicenseThreatGroup> licenseThreatGroups = component.getLicenseThreatGroups();
    if (licenseThreatGroups.isEmpty()) {
      return licenseThreatGroups;
    }

    Set<LicenseThreatGroup> result = new LinkedHashSet<>();
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if (">=".equals(operator) && (licenseThreatGroup.getThreatLevel() >= threatLevel)) {
        result.add(licenseThreatGroup);
      }
      else if ("<=".equals(operator) && (licenseThreatGroup.getThreatLevel() <= threatLevel)) {
        result.add(licenseThreatGroup);
      }
    }
    return result;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }

  @Override
  public String generateDroolsConditionCode(TransactionContext tx, Condition condition) {
    return "$licenseThreatGroup : (LicenseThreatGroup (ConditionTypes." + getClass().getSimpleName()
        + ".evaluateCondition(this, \"" + condition.getOperator() + "\", "
        + generateDroolsConditionValue(tx, condition.getValue()) + ")) from $component.licenseThreatGroups)";
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex
        + ", new TriggerLicenseThreatGroup($licenseThreatGroup)));";
  }
}
