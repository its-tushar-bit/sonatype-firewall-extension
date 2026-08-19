/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ComponentFormat;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ComponentFormatValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.136
 */
@Singleton
@Named
public class ComponentFormatConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "ComponentFormat";

  private static List<String> supportedOperators;

  static {
    supportedOperators = Arrays.asList("is", "is not");
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Format";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(Condition condition, MatchFact matchFact) {
    String message = "Component format is '" + matchFact.getComponent().getComponentIdentifier().getFormat() + "'";
    if ("is not".equals(condition.getOperator())) {
      message += ", not '" + condition.getValue() + "'";
    }
    return message;
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getValueTypeId() {
    return ComponentFormatValueType.ID;
  }

  @Override
  protected boolean isApplicable(Component component) {
    return component.getComponentIdentifier() != null;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean match = value.equals(component.getComponentIdentifier().getFormat());
    return "is".equals(operator) ? match : !match;
  }

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    String value = condition.getValue();
    if (StringUtils.isBlank(value)) {
      throw new InvalidConditionException(condition, "Component format is required");
    }

    if (!ComponentFormat.getAllAsStrings().contains(value)) {
      throw new InvalidConditionException(condition, "Unsupported component format: '" + value + "'");
    }

    super.validateCondition(tx, condition, ownerId);
  }
}
