/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;

public class RoleNotificationDTO
    extends NotificationDTO
{
  public final String roleId;

  public final String roleName;

  public RoleNotificationDTO(RoleNotification roleNotification) {
    super("role", roleNotification.getStageIds());
    roleId = roleNotification.getRoleId();
    roleName = roleNotification.getRoleName();
  }
}
