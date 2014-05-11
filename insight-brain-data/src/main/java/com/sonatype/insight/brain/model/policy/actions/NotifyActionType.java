/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import java.util.List;

import javax.mail.internet.InternetAddress;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.ValidationResult;

public class NotifyActionType
    implements ActionType
{
  public static final String ID = Action.ID_NOTIFY;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Notify";
  }

  @Override
  public List<String> getAvailableTargets() {
    return null;
  }

  @Override
  public boolean isRequiresTarget() {
    return true;
  }

  @Override
  public String getSummary() {
    return "Notification Sent";
  }

  @Override
  public ValidationResult validateAction(Action action) {
    ValidationResult result = new ValidationResult();
    String target = action.getTarget();
    
    if (target == null || target.trim().isEmpty()) {
      result.addError("Invalid action '" + getName() + "': A valid e-mail address is required");
    }
    else {
      // validate email address
      try {
        new InternetAddress(target);
      }
      catch (Exception e) {
        result.addError("Invalid action '" + getName() + "': A valid e-mail address is required instead of: " + target);
      }
    }

    return result;
  }
}
