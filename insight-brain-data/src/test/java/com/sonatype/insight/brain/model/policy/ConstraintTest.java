/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;

public class ConstraintTest
{
  private String ownerId = "ownerId";

  @Test
  public void testValidate_NameNull() {
    Constraint constraint = new Constraint("Constraint Id", null /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    ValidationResult result = constraint.validate(null, ownerId);
    assertValidationResultHasErrors(result, "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_NameEmpty() {
    Constraint constraint = new Constraint("Constraint Id", " " /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    ValidationResult result = constraint.validate(null, ownerId);
    assertValidationResultHasErrors(result, "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_NoConditions() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    ValidationResult result = constraint.validate(null, ownerId);
    assertValidationResultHasErrors(result, "Constraint 'Constraint Name' has no conditions");
  }

  @Test
  public void testValidate_InvalidCondition() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(null /* conditionTypeId */, "present"));
    ValidationResult result = constraint.validate(null, ownerId);
    assertValidationResultHasErrors(result, "Constraint 'Constraint Name' has invalid conditions:",
        "Invalid condition type id: 'null'");
  }
}
