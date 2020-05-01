/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.insight.brain.model.component.ComponentDataSource;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType.HAS_SUPPORT_FOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DataSourceConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
  private static final ComponentDataSource LICENSE = ComponentDataSource.getById("license");

  private Constraint createConstraint(String operator, String value) {
    return createConstraint("ConstraintId1", "Constraint Name 1", DataSourceConditionType.ID, operator, value);
  }

  @Test
  public void testValidateCondition_InvalidValue() {
    Condition condition = new Condition(DataSourceConditionType.ID, HAS_SUPPORT_FOR, "abc");
    assertThatThrownBy(() -> {
      new DataSourceConditionType().validateCondition(null, condition, null /* applicationId */);
    }).isInstanceOf(InvalidConditionException.class).hasMessageEndingWith("Value not supported: abc");
  }

  @Test
  public void testValidateCondition_ValidValue() {
    Condition condition = new Condition(DataSourceConditionType.ID, HAS_SUPPORT_FOR, LICENSE.getName());
    assertThat(condition).isNotNull();
  }
}
