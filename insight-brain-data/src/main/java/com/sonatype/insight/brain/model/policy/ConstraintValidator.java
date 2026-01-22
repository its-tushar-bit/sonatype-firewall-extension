/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class ConstraintValidator
{
  private final ConditionValidator conditionValidator;

  @Inject
  public ConstraintValidator(final ConditionValidator conditionValidator) {
    this.conditionValidator = conditionValidator;
  }

  public ValidationResult validate(TransactionContext tx, Constraint constraint, String ownerId) {
    ValidationResult result = new ValidationResult();
    if (constraint.getName() == null || constraint.getName().trim().isEmpty()) {
      result.addError("The constraint name must not be null or empty");
    }
    if (constraint.getConditions() == null || constraint.getConditions().isEmpty()) {
      result.addError("Constraint '" + constraint.getName() + "' has no conditions");
      return result;
    }

    ValidationResult conditionsResult = new ValidationResult();
    for (Condition condition : constraint.getConditions()) {
      conditionsResult.merge(conditionValidator.validate(tx, condition, ownerId));
    }

    if (!conditionsResult.isValid()) {
      result.addError("Constraint '" + constraint.getName() + "' has invalid conditions:");
      result.merge(conditionsResult);
    }

    return result;
  }
}
