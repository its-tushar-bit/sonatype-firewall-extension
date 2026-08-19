/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;

/**
 * @since 1.21
 */
public class RoleNotification
    extends Notification
{
  private String roleId;

  private String roleName;

  public RoleNotification() {
    // primarily supports deserialization
  }

  public RoleNotification(String roleId, String roleName, String... stageIds) {
    super(stageIds);
    setRoleId(roleId);
    setRoleName(roleName);
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public Action toAction() {
    return Action.newNotifyAction(roleId, NotifyActionType.TARGET_TYPE_ROLE);
  }

  @Override
  protected void addToNotifications(Notifications notifications) {
    notifications.getRoleNotifications().add(this);
  }

  @Override
  public String toString() {
    return "RoleNotification [roleId=" + roleId + ", getStageIds()=" + getStageIds() + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId);
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
    RoleNotification other = (RoleNotification) obj;
    return Objects.equals(roleId, other.roleId);
  }
}
