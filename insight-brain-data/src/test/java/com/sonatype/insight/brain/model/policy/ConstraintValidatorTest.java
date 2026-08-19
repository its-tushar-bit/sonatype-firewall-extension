/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.ValidationAssert.assertValidationResultHasErrors;
import static org.assertj.core.api.Assertions.assertThat;

public class ConstraintValidatorTest
    extends AbstractDataTest
{
  private final String ownerId = "ownerId";

  private final ConstraintValidator constraintValidator = new ConstraintValidator(new ConditionValidator());

  @Test
  public void testValidate_NameNull() {
    Constraint constraint = new Constraint("Constraint Id", null /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    ValidationResult result = constraintValidator.validate(null, constraint, ownerId);
    assertValidationResultHasErrors(result, "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_NameEmpty() {
    Constraint constraint = new Constraint("Constraint Id", " " /* name */, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    ValidationResult result = constraintValidator.validate(null, constraint, ownerId);
    assertValidationResultHasErrors(result, "The constraint name must not be null or empty");
  }

  @Test
  public void testValidate_NoConditions() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    ValidationResult result = constraintValidator.validate(null, constraint, ownerId);
    assertValidationResultHasErrors(result, "Constraint 'Constraint Name' has no conditions");
  }

  @Test
  public void testValidate_InvalidCondition() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint.addCondition(new Condition(null /* conditionTypeId */, "present"));
    ValidationResult result = constraintValidator.validate(null, constraint, ownerId);
    assertValidationResultHasErrors(result, "Constraint 'Constraint Name' has invalid conditions:",
        "Invalid condition type id: 'null'");
  }

  @Test
  public void testAddCondition() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    Condition condition1 = new Condition(null /* conditionTypeId */, "present");
    Condition condition2 = new Condition(null /* conditionTypeId */, "is");

    constraint.addCondition(condition1);
    constraint.addCondition(condition2);

    assertThat(condition1).isEqualTo(constraint.getConditions().get(0));
    assertThat(condition1.getConditionIndex()).isEqualTo(0);
    assertThat(condition2).isEqualTo(constraint.getConditions().get(1));
    assertThat(condition2.getConditionIndex()).isEqualTo(1);
  }

  @Test
  public void testSetConditions() {
    Constraint constraint = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    Condition condition1 = new Condition(null /* conditionTypeId */, "present");
    Condition condition2 = new Condition(null /* conditionTypeId */, "is");

    constraint.setConditions(Arrays.asList(condition1, condition2));

    assertThat(condition1).isEqualTo(constraint.getConditions().get(0));
    assertThat(condition1.getConditionIndex()).isEqualTo(0);
    assertThat(condition2).isEqualTo(constraint.getConditions().get(1));
    assertThat(condition2.getConditionIndex()).isEqualTo(1);
  }
}
