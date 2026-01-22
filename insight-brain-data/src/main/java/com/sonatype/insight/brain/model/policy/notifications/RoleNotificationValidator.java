/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.security.Role;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class RoleNotificationValidator
    extends NotificationValidator<RoleNotification>
{
  private final Provider<RoleDAO> roleDAOProvider;

  @Inject
  public RoleNotificationValidator(final Provider<RoleDAO> roleDAOProvider) {
    this.roleDAOProvider = roleDAOProvider;
  }

  @Override
  protected ValidationResult validate(final RoleNotification roleNotification) {
    ValidationResult validationResult = new ValidationResult();
    if (StringUtils.isBlank(roleNotification.getRoleId())) {
      validationResult.addError("Invalid notification: A valid role ID is required");
    }
    else {
      Role role = roleDAOProvider.get().getById(roleNotification.getRoleId());
      if (role == null) {
        validationResult.addError("Invalid notification: '" + roleNotification.getRoleId() + "' is not a valid role");
      }
    }
    return validationResult;
  }
}
