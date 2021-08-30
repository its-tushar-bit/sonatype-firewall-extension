/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

public class RepositoryComponentTelemetry
{
  private static final String USER_NOTIFICATION = "user";

  private static final String ROLE_NOTIFICATION = "role";

  private static final String WEBHOOK_NOTIFICATION = "webhook";

  private static final String JIRA_NOTIFICATION = "jira";

  private final String repositoryManagerId;

  private final String repositoryId;

  private final String componentFormat;

  private final String componentHash;

  private final String eventType;

  private final Long quarantineTime;

  private final Long releaseQuarantineTime;

  private final Set<String> notifications = new HashSet<>();

  private final String releaseQuarantineType;

  public RepositoryComponentTelemetry(
      final String repositoryManagerId,
      final RepositoryComponent repositoryComponent,
      final RepositoryComponentTelemetryEventType eventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final List<PolicyNotification> policyNotifications)
  {
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = repositoryComponent.getRepositoryId();
    this.componentFormat =
        repositoryComponent.getComponentIdentifier() == null ? null : repositoryComponent.getComponentIdentifier()
            .getFormat();
    this.componentHash = repositoryComponent.getHash();
    this.eventType = eventType.getDescription();
    this.quarantineTime =
        repositoryComponent.getQuarantineTime() == null ? null : repositoryComponent.getQuarantineTime().toInstant()
            .toEpochMilli();
    this.releaseQuarantineTime =
        repositoryComponent.getUnquarantineTime() == null ? null : repositoryComponent.getUnquarantineTime().toInstant()
            .toEpochMilli();
    this.releaseQuarantineType = releaseQuarantineType == null ? null : releaseQuarantineType.getDescription();
    if (policyNotifications != null) {
      policyNotifications.forEach(policyNotification -> {
        if (!policyNotification.getNotifications().getUserNotifications().isEmpty()) {
          notifications.add(USER_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getRoleNotifications().isEmpty()) {
          notifications.add(ROLE_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getJiraNotifications().isEmpty()) {
          notifications.add(WEBHOOK_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getWebhookNotifications().isEmpty()) {
          notifications.add(JIRA_NOTIFICATION);
        }
      });
    }
  }

  public String getRepositoryManagerId() {
    return repositoryManagerId;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getComponentFormat() {
    return componentFormat;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public String getEventType() {
    return eventType;
  }

  public Long getQuarantineTime() {
    return quarantineTime;
  }

  public Long getReleaseQuarantineTime() {
    return releaseQuarantineTime;
  }

  public Set<String> getNotifications() {
    return notifications;
  }

  public String getReleaseQuarantineType() {
    return releaseQuarantineType;
  }

  public enum RepositoryComponentTelemetryEventType
  {
    AUDIT("audit"),
    QUARANTINE("quarantine"),
    RELEASE_QUARANTINE("release_quarantine"),
    DELETE("delete");

    private final String description;

    RepositoryComponentTelemetryEventType(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  public enum ReleaseQuarantineType
  {
    AUTO("auto"),
    MANUAL("manual");

    private final String description;

    ReleaseQuarantineType(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
