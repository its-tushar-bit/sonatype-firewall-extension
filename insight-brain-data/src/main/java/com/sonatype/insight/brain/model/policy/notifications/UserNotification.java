/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Action;

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
