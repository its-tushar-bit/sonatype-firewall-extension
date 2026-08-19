/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.ValidationResult;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class JiraNotificationValidator
    extends NotificationValidator<JiraNotification>
{
  @Override
  protected ValidationResult validate(final JiraNotification jiraNotification) {
    ValidationResult validationResult = new ValidationResult();
    if (StringUtils.isBlank(jiraNotification.getProjectKey())) {
      validationResult.addError("Invalid JIRA notification: A valid project key is required");
    }
    if (jiraNotification.getIssueTypeId() == null) {
      validationResult.addError("Invalid JIRA notification: A valid issue type id is required");
    }
    return validationResult;
  }
}
