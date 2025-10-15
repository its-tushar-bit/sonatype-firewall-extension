/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;

/**
 * Internal data structure for aggregating user activity metrics from audit events.
 * Used by UserActivityService to track login counts, API calls, scans, and configuration activities.
 */
class UserActivityData
{
  private final String username;

  private int loginCount = 0;

  private String lastActive;

  public UserActivityData(String username) {
    this.username = username;
  }

  public void addEvent(AuditDTO auditEvent) {
    // Update last active time
    if (lastActive == null || (auditEvent.timestamp != null && auditEvent.timestamp.compareTo(lastActive) > 0)) {
      lastActive = auditEvent.timestamp;
    }

    // Count different types of activities
    if (AuditEvent.LOGIN.getType().equals(auditEvent.type)) {
      loginCount++;
    }
  }

  public String getUsername() {
    return username;
  }

  public Integer getLoginCount() {
    return loginCount;
  }

  public String getLastActive() {
    return lastActive;
  }
}
