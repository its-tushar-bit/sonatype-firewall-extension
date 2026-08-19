/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.actions;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.ActionType;

public class NotifyActionType
    implements ActionType
{
  public static final String ID = Action.ID_NOTIFY;

  public static final String TARGET_TYPE_ROLE = "role";

  /**
   * @since 1.21.0
   */
  public static final String TARGET_TYPE_JIRA = "jira";

  /**
   * @since 1.64.0
   */
  public static final String TARGET_TYPE_WEBHOOK = "webhook";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Notify";
  }

  @Override
  public String getSummary(String stageTypeId) {
    return "Notification Sent";
  }
}
