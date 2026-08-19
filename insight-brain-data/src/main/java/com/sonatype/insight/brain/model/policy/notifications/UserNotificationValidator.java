/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.internet.InternetAddress;

import com.sonatype.insight.brain.model.ValidationResult;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class UserNotificationValidator
    extends NotificationValidator<UserNotification>
{
  @Override
  protected ValidationResult validate(final UserNotification userNotification) {
    ValidationResult validationResult = new ValidationResult();
    if (StringUtils.isBlank(userNotification.getEmailAddress())) {
      validationResult.addError("Invalid notification: A valid e-mail address is required");
    }
    else {
      try {
        new InternetAddress(userNotification.getEmailAddress());
      }
      catch (Exception e) {
        validationResult.addError("Invalid notification: A valid e-mail address is required instead of: " +
            userNotification.getEmailAddress());
      }
    }
    return validationResult;
  }
}
