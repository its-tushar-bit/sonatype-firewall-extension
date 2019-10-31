/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.Objects;

import javax.mail.internet.InternetAddress;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.ValidationResult;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.21
 */
public class UserNotification
    extends Notification
{
  private String emailAddress;

  public UserNotification() {
    // primarily supports deserialization
  }

  public UserNotification(String emailAddress, String... stageIds) {
    super(stageIds);
    setEmailAddress(emailAddress);
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  @Override
  protected void validate(ValidationResult validationResult) {
    if (StringUtils.isBlank(emailAddress)) {
      validationResult.addError("Invalid notification: A valid e-mail address is required");
    }
    else {
      try {
        new InternetAddress(emailAddress);
      }
      catch (Exception e) {
        validationResult
            .addError("Invalid notification: A valid e-mail address is required instead of: " + emailAddress);
      }
    }
  }

  @Override
  public Action toAction() {
    return Action.newNotifyAction(emailAddress, null);
  }

  @Override
  protected void addToNotifications(Notifications notifications) {
    notifications.getUserNotifications().add(this);
  }

  @Override
  public String toString() {
    return "UserNotification [emailAddress=" + emailAddress + ", getStageIds()=" + getStageIds() + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    UserNotification other = (UserNotification) obj;
    return Objects.equals(emailAddress, other.emailAddress);
  }
}
