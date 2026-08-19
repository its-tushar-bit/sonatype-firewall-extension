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
 * @since 1.21.0
 */
public class JiraNotification
    extends Notification
{
  private String projectKey;

  private Long issueTypeId;

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(final String projectKey) {
    this.projectKey = projectKey;
  }

  public Long getIssueTypeId() {
    return issueTypeId;
  }

  public void setIssueTypeId(final long issueTypeId) {
    this.issueTypeId = issueTypeId;
  }

  public JiraNotification() {
    // primarily supports deserialization
  }

  public JiraNotification(String projectKey, long issueTypeId, String... stageIds) {
    super(stageIds);
    this.projectKey = projectKey;
    this.issueTypeId = issueTypeId;
  }

  @Override
  public Action toAction() {
    return Action.newNotifyAction(projectKey, NotifyActionType.TARGET_TYPE_JIRA);
  }

  @Override
  protected void addToNotifications(final Notifications notifications) {
    notifications.getJiraNotifications().add(this);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof JiraNotification) {
      JiraNotification other = (JiraNotification) obj;
      return Objects.equals(projectKey, other.getProjectKey()) && Objects.equals(issueTypeId, other.getIssueTypeId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectKey, issueTypeId);
  }

  @Override
  public String toString() {
    return "JiraNotification [projectKey=" + projectKey + ", issueTypeId=" + issueTypeId + ", getStageIds()="
        + getStageIds() + "]";
  }
}
