/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import com.sonatype.clm.dto.model.policy.PolicyFact;

/**
 * @since 1.21.0
 */
public class PolicyNotification
{
  private PolicyFact policyFact;

  private Notifications notifications;

  public PolicyFact getPolicyFact() {
    return policyFact;
  }

  public Notifications getNotifications() {
    return notifications;
  }

  public PolicyNotification() {
  }

  public PolicyNotification(PolicyFact policyFact, Notifications notifications) {
    this.policyFact = policyFact;
    this.notifications = notifications;
  }
}
