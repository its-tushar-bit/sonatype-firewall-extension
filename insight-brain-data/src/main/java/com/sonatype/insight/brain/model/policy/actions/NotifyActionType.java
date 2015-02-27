/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import javax.mail.internet.InternetAddress;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.security.Role;

import org.apache.commons.lang.StringUtils;

public class NotifyActionType
    implements ActionType
{
  public static final String ID = Action.ID_NOTIFY;

  public static final String TARGET_TYPE_ROLE = "role";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Notify";
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

    String targetType = action.getTargetType();
    String target = action.getTarget();
    if (TARGET_TYPE_ROLE.equals(targetType)) {
      if (StringUtils.isBlank(target)) {
        result.addError("Invalid action '" + getName() + "': A valid role ID is required");
      }
      else {
        Role role = new RoleDAO().getById(target);
        if (role == null) {
          result.addError("Invalid action '" + getName() + "': A valid role ID is required instead of: " + target);
        }
      }
    }
    else if (StringUtils.isBlank(targetType)) {
      if (StringUtils.isBlank(target)) {
        result.addError("Invalid action '" + getName() + "': A valid e-mail address is required");
      }
      else {
        // validate email address
        try {
          new InternetAddress(target);
        }
        catch (Exception e) {
          result.addError("Invalid action '" + getName() + "': A valid e-mail address is required instead of: "
              + target);
        }
      }
    }
    else {
      result.addError("Invalid action '" + getName() + "': Invalid target type: '" + targetType + "'");
    }

    return result;
  }
}
