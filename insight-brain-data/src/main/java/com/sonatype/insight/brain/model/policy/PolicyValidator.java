/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.notifications.NotificationsValidator;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PolicyValidator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyValidator.class);

  private final ConstraintValidator constraintValidator;

  private final NotificationsValidator notificationsValidator;

  @Inject
  public PolicyValidator(
      final ConstraintValidator constraintValidator,
      final NotificationsValidator notificationsValidator)
  {
    this.constraintValidator = constraintValidator;
    this.notificationsValidator = notificationsValidator;
  }

  public ValidationResult validate(final TransactionContext tx, final Policy policy, final String ownerId) {
    log.debug("Validating {}", policy);

    ValidationResult validationResult = new ValidationResult();
    try {
      NameHelper.validate("The policy name", policy.getName());
    }
    catch (InvalidNameException e) {
      validationResult.addError(e.getMessage());
    }

    if (policy.getConstraints() == null || policy.getConstraints().isEmpty()) {
      validationResult.addError("Policy '" + policy.getName() + "' has no constraints");
    }
    else {
      ValidationResult constraintResult = new ValidationResult();
      Set<String> constraintNames = new LinkedHashSet<>();
      for (Constraint constraint : policy.getConstraints()) {
        String constraintName = constraint.getName();
        if (constraintName != null && !constraintName.trim().isEmpty()) {
          if (constraintNames.contains(constraintName)) {
            constraintResult.addError("Duplicate constraint name '" + constraintName + "'");
          }
          else {
            constraintNames.add(constraintName);
          }
        }
        constraintResult.merge(constraintValidator.validate(tx, constraint, ownerId));
      }
      if (!constraintResult.isValid()) {
        validationResult.addError("Policy '" + policy.getName() + "' has invalid constraints:");
        validationResult.merge(constraintResult);
      }
    }

    ValidationResult actionResult = new ValidationResult();
    for (String stageId : policy.getActions().keySet()) {
      if (StageTypes.getById(stageId) == null) {
        actionResult.addError("Invalid stage: '" + stageId + "'");
      }

      String actionId = policy.getActions().get(stageId);
      if (!Action.ID_FAIL.equals(actionId) && !Action.ID_WARN.equals(actionId)) {
        actionResult.addError("Invalid action for stage '" + stageId + "': '" + actionId + "'");
      }
    }
    if (!actionResult.isValid()) {
      validationResult.addError("Policy '" + policy.getName() + "' has invalid actions:");
      validationResult.merge(actionResult);
    }

    ValidationResult notificationsResult = notificationsValidator.validate(policy.getNotifications());
    if (!notificationsResult.isValid()) {
      validationResult.addError("Policy '" + policy.getName() + "' has invalid notifications:");
      validationResult.merge(notificationsResult);
    }

    if (policy.getThreatLevel() < 0 || policy.getThreatLevel() > 10) {
      validationResult.addError(
          "Policy '" + policy.getName() + "' has threat level outside of valid range 0-10: " + policy.getThreatLevel());
    }

    if (!validationResult.isValid()) {
      log.debug("Validation result: {}", validationResult.toMessageString());
    }

    return validationResult;
  }
}
