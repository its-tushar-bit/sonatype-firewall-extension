/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.dataaccess.TransactionContext;

public abstract class AbstractConditionType
    implements ConditionType
{
  private final TenantReference<Boolean> enabled = new TenantReference<>(() -> true);

  @Override
  public void validateCondition(
      TransactionContext tx,
      Condition condition,
      String ownerId) throws InvalidConditionException
  {
    if (condition.getOperator() == null) {
      throw new InvalidConditionException(condition, "Operator is null");
    }
    if (!getSupportedOperators().contains(condition.getOperator())) {
      throw new InvalidConditionException(condition, "Operator is not supported");
    }
    if (getValueTypeId() != null && condition.getValue() == null) {
      throw new InvalidConditionException(condition, "Value is null");
    }
  }

  @Override
  public String getValueHint() {
    return null;
  }

  @Override
  public String explainCondition(Condition condition) {
    return getName() + ' ' + condition.getOperator() + (condition.getValue() != null ? ' ' + condition.getValue() : "");
  }

  protected abstract String generateDroolsConditionValue(TransactionContext tx, String value);

  protected static String asDroolsComment(String text) {
    return " /* " + text.replace("*/", "").replaceAll("[\r\n]+", " ") + " */";
  }

  protected static String asDroolsString(String value) {
    if (value == null) {
      value = "null";
    }
    else {
      value = value.replace("\\", "\\\\");
      value = value.replace("\n", "\\n");
      value = value.replace("\r", "\\r");
      value = value.replace("\"", "\\\"");
      value = '"' + value + '"';
    }
    return value;
  }

  protected static String asDroolsInteger(String value) {
    // We've seen issues similar to https://issues.jboss.org/browse/JBRULES-3628 so we use explicit boxing
    return "Integer.valueOf( " + value + " )";
  }

  protected static String asDroolsFloat(String value) {
    return "Float.valueOf( (float) " + value + " )";
  }

  protected static String asDroolsDouble(String value) {
    return "Double.valueOf( (double) " + value + " )";
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.OTHER;
  }

  /**
   * @since 1.32.0
   */
  @Override
  public String convertIfNeeded(final String value) {
    return value;
  }

  @Override
  public TriggerReference getTriggerReference(Condition condition, MatchFact matchFact) {
    return null;
  }

  /**
   * @since 1.87
   */
  @Override
  public boolean isEnabled() {
    return enabled.get();
  }

  /**
   * @since 1.87
   */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled.set(enabled);
  }
}
