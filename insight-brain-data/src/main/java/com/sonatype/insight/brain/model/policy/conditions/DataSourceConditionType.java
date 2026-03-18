/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentDataSource;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.DataSourceValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.ImmutableList;

@Singleton
@Named
public class DataSourceConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "DataSource";

  public static final String HAS_SUPPORT_FOR = "has support for";

  public static final String HAS_NO_SUPPORT_FOR = "has no support for";

  private static List<String> supportedOperators = ImmutableList.of(HAS_SUPPORT_FOR, HAS_NO_SUPPORT_FOR);

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Data Source";
  }

  @Override
  protected String generateDroolsConditionValue(final TransactionContext tx, final String value) {
    return asDroolsString(value);
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String explainCondition(final Condition condition) {
    return getName() + ' ' + condition.getOperator() + ' ' +
        ComponentDataSource.getById(condition.getValue()).getId();
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    return getName() + ' ' + condition.getOperator() + ' '
        + ComponentDataSource.getById(condition.getValue()).getName();
  }

  @Override
  public String getValueTypeId() {
    return DataSourceValueType.ID;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId) {
    super.validateCondition(tx, condition, ownerId);

    if (ComponentDataSource.getById(condition.getValue()) == null) {
      throw new InvalidConditionException(condition, "Value not supported: " + condition.getValue());
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  protected boolean isApplicable(Component component) {
    return true;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    AnalyzerFeatures analyzerFeatures = component.getAnalyzerFeatures();
    if (analyzerFeatures != null) {
      boolean result = ComponentDataSource.IDENTITY.getId().contentEquals(value)
          ? analyzerFeatures.isHasIdentity()
          : analyzerFeatures.isHasLicense();
      return HAS_SUPPORT_FOR.equals(operator) ? result : !result;
    }
    // If Metadata is not present we are returning false so the condition is not triggered
    return false;
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.OTHER;
  }
}
