/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;

public class JiraNotificationDTO
    extends NotificationDTO
{
  public final String jiraProjectKey;

  public final String jiraIssueTypeId;

  public JiraNotificationDTO(JiraNotification jiraNotification) {
    super("jira", jiraNotification.getStageIds());
    jiraProjectKey = jiraNotification.getProjectKey();
    jiraIssueTypeId = String.valueOf(jiraNotification.getIssueTypeId());
  }
}
