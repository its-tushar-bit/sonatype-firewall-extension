/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Condition types reasoning about a specific license threat group found on a component.
 *
 * @param <T> The type of the condition value.
 */
public abstract class AbstractLicenseThreatGroupConditionType<T>
    extends AbstractConditionType
{
  @Override
  public final String generateDroolsConditionCode(TransactionContext tx, Condition condition) {
    return "ConditionTypes." + getClass().getSimpleName() + ".evaluateCondition($component, this, \""
        + condition.getOperator() + "\", " + generateDroolsConditionValue(tx, condition.getValue()) + ")";
  }

  public abstract boolean evaluateCondition(
      Component component,
      LicenseThreatGroup licenseThreatGroup,
      String operator,
      T value);
}
