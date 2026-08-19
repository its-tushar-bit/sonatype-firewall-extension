/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Map;

import com.sonatype.insight.brain.model.policy.notifications.Notifications;

public class PolicyOverridesDTO
{
  public Map<String, String> actions;

  public Notifications notifications;

  public PolicyOverridesDTO() {
    // for Jackson
  }

  public PolicyOverridesDTO(Map<String, String> actions) {
    this.actions = actions;
  }

  public PolicyOverridesDTO(Notifications notifications) {
    this.notifications = notifications;
  }
}
