/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerLicenseThreatGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class LicenseThreatGroupConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "License Threat Group";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseDAO licenseDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public LicenseThreatGroupConditionType(
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseDAO licenseDAO,
      final OwnerDAO ownerDAO)
  {
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseDAO = licenseDAO;
    this.ownerDAO = ownerDAO;
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return LicenseThreatGroupValueType.ID;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    super.validateCondition(tx, condition, ownerId);

    String licenseThreatGroupId = condition.getValue();
    LicenseThreatGroupValueType licenseThreatGroupValueType =
        new LicenseThreatGroupValueType(tx, ownerId, ownerDAO, licenseThreatGroupDAO);
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
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value)
        + asDroolsComment("License threat group name: " + getLicenseThreatGroupName(tx, value));
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + " '" + getLicenseThreatGroupName(null, condition.getValue())
        + '\'';
  }

  private String getLicenseThreatGroupName(TransactionContext tx, String licenseThreatGroupId) {
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(licenseThreatGroupId)) {
      return LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_NAME;
    }

    LicenseThreatGroup licenseThreatGroup = tx != null
        ? licenseThreatGroupDAO.getById(tx, licenseThreatGroupId)
        : licenseThreatGroupDAO.getById(licenseThreatGroupId);
    // NOTE: Due to CLM-8176, it's possible to reference a missing LTG, unfortunate but not appropriate to crash here
    return licenseThreatGroup != null ? licenseThreatGroup.getName() : "[deleted]";
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(condition.getValue())) {
      if ("is".equals(condition.getOperator())) {
        String licenseNames = licenseIdsToLicenseNamesCsv(matchFact.getComponent().getUnassignedLicenseIds());
        return "Found licenses that are not assigned to any license threat group (" + licenseNames + ")";
      }
      else {
        return "Did not find a license that is not assigned to any license threat group";
      }
    }

    TriggerLicenseThreatGroup conditionTrigger = (TriggerLicenseThreatGroup) matchFact
        .getConditionTriggerByConditionIndex(condition.getConditionIndex())
        .getTrigger();
    String licenseThreatGroupId = conditionTrigger.id;
    String licenseThreatGroupName = getLicenseThreatGroupName(null, licenseThreatGroupId);

    if ("is".equals(condition.getOperator())) {
      String licenseNames =
          licenseIdsToLicenseNamesCsv(matchFact.getComponent().getLicenseIdsInLicenseThreatGroup(licenseThreatGroupId));
      return "Found licenses in the '" + licenseThreatGroupName + "' license threat group (" + licenseNames + ")";
    }
    else {
      return "Did not find a license in the '" + licenseThreatGroupName + "' license threat group";
    }
  }

  private String licenseIdsToLicenseNamesCsv(Set<String> licenseIds) {
    return licenseIds.stream()
        .map(licenseId -> "'" + licenseDAO.getByIdNotNull(licenseId).getShortDisplayName() + "'")
        .collect(Collectors.joining(", "));
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean result;
    if (LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(value)) {
      result = !component.getUnassignedLicenseIds().isEmpty();
    }
    else {
      result = !component.getLicenseIdsInLicenseThreatGroup(value).isEmpty();
    }
    return "is".equals(operator) ? result : !result;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.LICENSE;
  }

  @Override
  public String generateDroolsTriggerCode(Condition condition, int conditionIndex) {
    return "$conditionTriggers.add(new ConditionTrigger(" + conditionIndex + ", new TriggerLicenseThreatGroup("
        + asDroolsString(condition.getValue()) + ")));";
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return true;
  }
}
