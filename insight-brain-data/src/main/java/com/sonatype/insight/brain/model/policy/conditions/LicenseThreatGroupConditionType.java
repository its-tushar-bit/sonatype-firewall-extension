/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;

public class LicenseThreatGroupConditionType
    extends AbstractConditionType<String>
{
  public static final String ID = "License Threat Group";

  private static List<String> supportedOperators = new ArrayList<String>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return LicenseThreatGroupValueType.ID;
  }

  @Override
  public void validateCondition(Condition condition, String ownerId) throws InvalidConditionException {
    super.validateCondition(condition, ownerId);

    String licenseThreatGroupId = condition.getValue();
    LicenseThreatGroupValueType licenseThreatGroupValueType = new LicenseThreatGroupValueType(ownerId);
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupValueType.getAvailableValues()) {
      if (licenseThreatGroup.getId().equals(licenseThreatGroupId)) {
        return;
      }
    }
    throw new InvalidConditionException(condition, "Invalid license threat group id: " + licenseThreatGroupId);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "License Threat Group";
  }

  @Override
  public String generateDroolsConditionValue(String value) {
    return "\"" + value + "\"" + asDroolsComment("License threat group name: " + getLicenseThreatGroupName(value));
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + " '" + getLicenseThreatGroupName(condition.getValue()) + '\'';
  }

  private String getLicenseThreatGroupName(String licenseThreatGroupId) {
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(licenseThreatGroupId)) {
      return LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_NAME;
    }

    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getById(licenseThreatGroupId);
    return licenseThreatGroup.getName();
  }

  @Override
  public String explainMatch(final Condition condition, final Component component) {
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(condition.getValue())) {
      if ("is".equals(condition.getOperator())) {
        return "Found a License that is not assigned to any License Threat Group";
      }
      else {
        return "Did not find a License that is not assigned to any License Threat Group";
      }
    }

    final StringBuilder buf = new StringBuilder();
    final Set<LicenseThreatGroup> licenseThreatGroups = component.getLicenseThreatGroups();
    if ("is".equals(condition.getOperator())) {
      final String groupId = condition.getValue();
      for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
        if (groupId.equals(licenseThreatGroup.getId())) {
          return "Found a License in the '" + licenseThreatGroup.getName() + "' License Threat Group";
        }
      }
      throw new IllegalStateException("Cannot explainMatch when there was no match");
    }
    if (licenseThreatGroups.isEmpty()) {
      buf.append("no");
    }
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if (buf.length() > 0) {
        buf.append(" and ");
      }
      buf.append('\'').append(licenseThreatGroup.getName()).append('\'');
    }
    return "Found " + buf + " License Threat " + (licenseThreatGroups.size() != 1 ? "Groups" : "Group");
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result;
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(value)) {
      result = !component.getUnassignedLicenseIds().isEmpty();
    }
    else {
      result = component.hasLicenseInLicenseThreatGroup(value);
    }
    return "is".equals(operator) ? result : !result;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }
}
