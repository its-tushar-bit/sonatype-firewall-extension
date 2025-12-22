/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.purl.PackageUrlIdentifier;

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

  private final String releaseReason;

  private final String componentIdentifier;

  private final String componentName;

  private final String componentNamespace;

  private final String componentVersion;

  private final String accountId;

  public RepositoryComponentTelemetry(
      final String accountId,
      final String repositoryManagerId,
      final RepositoryComponent repositoryComponent,
      final RepositoryComponentTelemetryEventType eventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final List<PolicyNotification> policyNotifications)
  {
    this(accountId, repositoryManagerId, repositoryComponent, eventType, releaseQuarantineType, null,
        policyNotifications);
  }

  public RepositoryComponentTelemetry(
      final String accountId,
      final String repositoryManagerId,
      final RepositoryComponent repositoryComponent,
      final RepositoryComponentTelemetryEventType eventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    this.accountId = accountId;
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = repositoryComponent.getRepositoryId();
    this.componentFormat =
        repositoryComponent.getComponentIdentifier() == null ? null : repositoryComponent.getComponentIdentifier()
            .getFormat();
    this.componentHash = HdsClientAnalytics.obfuscate(repositoryComponent.getHash());
    this.eventType = eventType.getDescription();
    this.quarantineTime =
        repositoryComponent.getQuarantineTime() == null ? null : repositoryComponent.getQuarantineTime().toInstant()
            .toEpochMilli();
    this.releaseQuarantineTime =
        repositoryComponent.getUnquarantineTime() == null ? null : repositoryComponent.getUnquarantineTime().toInstant()
            .toEpochMilli();
    this.releaseQuarantineType = releaseQuarantineType == null ? null : releaseQuarantineType.getDescription();
    this.releaseReason = releaseReason;

    // Extract component identifier details using PackageUrlIdentifier for robust cross-format extraction
    if (repositoryComponent.getComponentIdentifier() != null) {
      this.componentIdentifier =
          ComponentIdentifierAdapter.toJson(repositoryComponent.getComponentIdentifier());
      PackageUrlIdentifier purl =
          PackageUrlIdentifier.fromComponentIdentifier(repositoryComponent.getComponentIdentifier());
      this.componentName = purl.getName();
      this.componentNamespace = purl.getNamespace();
      this.componentVersion = purl.getVersion();
    }
    else {
      this.componentIdentifier = null;
      this.componentName = null;
      this.componentNamespace = null;
      this.componentVersion = null;
    }

    if (policyNotifications != null) {
      policyNotifications.forEach(policyNotification -> {
        if (!policyNotification.getNotifications().getUserNotifications().isEmpty()) {
          notifications.add(USER_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getRoleNotifications().isEmpty()) {
          notifications.add(ROLE_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getJiraNotifications().isEmpty()) {
          notifications.add(JIRA_NOTIFICATION);
        }
        if (!policyNotification.getNotifications().getWebhookNotifications().isEmpty()) {
          notifications.add(WEBHOOK_NOTIFICATION);
        }
      });
    }
  }

  public RepositoryComponentTelemetry(
      final String repositoryManagerId,
      final String repositoryId,
      final String componentFormat,
      final String componentHash,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final Long quarantineTime,
      final Long releaseQuarantineTime,
      final String releaseQuarantineType)
  {
    this(repositoryManagerId, repositoryId, componentFormat, componentHash, repositoryComponentTelemetryEventType,
        quarantineTime, releaseQuarantineTime, releaseQuarantineType, null);
  }

  public RepositoryComponentTelemetry(
      final String repositoryManagerId,
      final String repositoryId,
      final String componentFormat,
      final String componentHash,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final Long quarantineTime,
      final Long releaseQuarantineTime,
      final String releaseQuarantineType,
      final String releaseReason)
  {
    this.accountId = null; // Not available in this legacy constructor
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = repositoryId;
    this.componentFormat = componentFormat;
    this.componentHash =  HdsClientAnalytics.obfuscate(componentHash);
    this.eventType = repositoryComponentTelemetryEventType.getDescription();
    this.quarantineTime = quarantineTime;
    this.releaseQuarantineTime = releaseQuarantineTime;
    this.releaseQuarantineType = releaseQuarantineType;
    this.releaseReason = releaseReason;

    // Component identifier fields not available in this constructor
    this.componentIdentifier = null;
    this.componentName = null;
    this.componentNamespace = null;
    this.componentVersion = null;
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

  public String getReleaseReason() {
    return releaseReason;
  }

  public String getComponentIdentifier() {
    return componentIdentifier;
  }

  public String getComponentName() {
    return componentName;
  }

  public String getComponentNamespace() {
    return componentNamespace;
  }

  public String getComponentVersion() {
    return componentVersion;
  }

  public String getAccountId() {
    return accountId;
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

  public enum ReleaseReason
  {
    WAIVED("Waived"),
    AUTO_RELEASED("Auto-Released"),
    DELETED("Deleted"),
    POLICY_CHANGE("Policy-Change"),
    MONITORING_ENABLED("Monitoring-Enabled");

    private final String description;

    ReleaseReason(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
