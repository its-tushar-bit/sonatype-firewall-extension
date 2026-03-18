/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IacControl;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IacControlValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class IacControlConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "IacControlConditionType";

  private static List<String> supportedOperators = new ArrayList<>();

  static {
    supportedOperators.add("is");
    supportedOperators.add("is not");
  }

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  @Inject
  public IacControlConditionType(final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO) {
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "IaC Compliance Family";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String getValueTypeId() {
    return IacControlValueType.ID;
  }

  @Override
  public String explainMatch(Condition condition, MatchFact matchFact) {
    return "IaC Compliance Family was " + ("is not".equals(condition.getOperator()) ? "not " : "")
        + IacControl.getById(condition.getValue()).getName();
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  private final Pattern controlPattern = Pattern.compile("`(.*?)`");

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean anyMatch = false;

    for (SecurityVulnerability securityVulnerability : component.getSecurityVulnerabilities()) {
      ThirdPartyVulnerability thirdPartyVulnerability =
          thirdPartyVulnerabilityDAO.getByRefId(securityVulnerability.getRefId());
      if (thirdPartyVulnerability == null) {
        continue;
      }

      String description = Optional.ofNullable(thirdPartyVulnerability.getDescription()).orElse("");

      Matcher matcher = controlPattern.matcher(description);
      while (matcher.find()) {
        if (matcher.group(1).startsWith(value)) {
          anyMatch = true;
          break;
        }
      }
    }

    return "is".equals(operator) == anyMatch;
  }

  @Override
  protected String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.SECURITY;
  }
}
