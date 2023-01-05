/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;

public class ConditionTest
{
  private final String ownerId = "ownerId";

  @Test
  public void testValidate_ConditionTypeIdNull() {
    Condition condition = new Condition(null /* conditionTypeId */, "present");
    ValidationResult result = condition.validate(null, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: 'null'");
  }

  @Test
  public void testValidate_ConditionTypeIdEmpty() {
    Condition condition = new Condition(" ", "present");
    ValidationResult result = condition.validate(null, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: ' '");
  }

  @Test
  public void testValidate_ConditionTypeIdInvalid() {
    Condition condition = new Condition("abc", "present");
    ValidationResult result = condition.validate(null, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: 'abc'");
  }

  @Test
  public void testValidate_OperatorNull() {
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, null /* operator */);
    ValidationResult result = condition.validate(null, ownerId);
    assertValidationResultHasErrors(result,
        "Invalid condition 'SecurityVulnerabilitySeverity null null', Operator is null");
  }
}
