/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

public abstract class NotificationValidator<R extends Notification>
{
  public ValidationResult validate(@SuppressWarnings("unused") final TransactionContext tx, final R notification) {
    ValidationResult validationResult = new ValidationResult();
    for (String stageId : notification.getStageIds()) {
      if (!Notification.CONTINUOUS_MONITORING.equals(stageId) && StageTypes.getById(stageId) == null) {
        validationResult.addError("Invalid stage type id: '" + stageId + "'");
      }
    }

    ValidationResult innerValidationResult = validate(notification);
    if (!innerValidationResult.isValid()) {
      validationResult.merge(innerValidationResult);
    }

    return validationResult;
  }

  protected abstract ValidationResult validate(final R notification);
}
