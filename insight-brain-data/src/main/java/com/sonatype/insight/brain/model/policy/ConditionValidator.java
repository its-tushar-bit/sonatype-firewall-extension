/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

@Singleton
@Named
public class ConditionValidator
{
  public ValidationResult validate(final TransactionContext tx, final Condition condition, final String ownerId) {
    try {
      ConditionType conditionType = ConditionTypes.getById(condition.getConditionTypeId());
      conditionType.validateCondition(tx, condition, ownerId);
    }
    catch (InvalidConditionException | IllegalArgumentException e) {
      return new ValidationResult(e);
    }

    return ValidationResult.noErrors();
  }
}
