/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IntegerValueType;

public class LicenseThreatGroupLevelConditionType
    extends AbstractConditionType<Integer>
{
  public static final String ID = "License Threat Group Level";

  private static List<String> supportedOperators = new ArrayList<String>();

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
  public void validateCondition(Condition condition, String ownerId) throws InvalidConditionException {
    super.validateCondition(condition, ownerId);

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
  public String generateDroolsConditionValue(String value) {
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

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, Integer value) {
    return !getLicenseThreatGroupsByLevel(component, value, operator).isEmpty();
  }

  private Set<LicenseThreatGroup> getLicenseThreatGroupsByLevel(Component component, int threatLevel, String operator) {
    Set<LicenseThreatGroup> licenseThreatGroups = component.getLicenseThreatGroups();
    if (licenseThreatGroups.isEmpty()) {
      return licenseThreatGroups;
    }

    Set<LicenseThreatGroup> result = new LinkedHashSet<LicenseThreatGroup>();
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
}
