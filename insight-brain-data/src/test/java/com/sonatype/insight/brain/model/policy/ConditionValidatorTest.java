/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;

public class ConditionValidatorTest
    extends AbstractDataTest
{
  private final String ownerId = "ownerId";

  private final ConditionValidator conditionValidator = new ConditionValidator();

  @Test
  public void testValidate_ConditionTypeIdNull() {
    Condition condition = new Condition(null /* conditionTypeId */, "present");
    ValidationResult result = conditionValidator.validate(null, condition, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: 'null'");
  }

  @Test
  public void testValidate_ConditionTypeIdEmpty() {
    Condition condition = new Condition(" ", "present");
    ValidationResult result = conditionValidator.validate(null, condition, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: ' '");
  }

  @Test
  public void testValidate_ConditionTypeIdInvalid() {
    Condition condition = new Condition("abc", "present");
    ValidationResult result = conditionValidator.validate(null, condition, ownerId);
    assertValidationResultHasErrors(result, "Invalid condition type id: 'abc'");
  }

  @Test
  public void testValidate_OperatorNull() {
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, null /* operator */);
    ValidationResult result = conditionValidator.validate(null, condition, ownerId);
    assertValidationResultHasErrors(result,
        "Invalid condition 'SecurityVulnerabilitySeverity null null', Operator is null");
  }
}
