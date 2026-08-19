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
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public class ProxyRepositoryComponentTelemetry
{
  private static final String USER_NOTIFICATION = "user";

  private static final String ROLE_NOTIFICATION = "role";

  private static final String WEBHOOK_NOTIFICATION = "webhook";

  private static final String JIRA_NOTIFICATION = "jira";

  private final String repositoryManagerId;

  private final String repositoryId;

  private final String repositoryName;

  private final String repositoryType;

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

  // Private constructor for builder
  private ProxyRepositoryComponentTelemetry(Builder builder) {
    this.accountId = builder.accountId;
    this.repositoryManagerId = builder.repositoryManagerId;
    this.repositoryId = builder.repositoryId;
    // Obfuscate repository name if obfuscator is provided (consistent with application ID obfuscation)
    this.repositoryName = builder.repositoryName != null && builder.telemetryDataObfuscator != null
        ? builder.telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(builder.repositoryName)
        : builder.repositoryName;
    this.repositoryType = builder.repositoryType;
    this.componentFormat = builder.componentFormat;
    this.componentHash = builder.componentHash != null ? HdsClientAnalytics.obfuscate(builder.componentHash) : null;
    this.eventType = builder.eventType;
    this.quarantineTime = builder.quarantineTime;
    this.releaseQuarantineTime = builder.releaseQuarantineTime;
    this.releaseQuarantineType = builder.releaseQuarantineType;
    this.releaseReason = builder.releaseReason;
    this.componentIdentifier = builder.componentIdentifier;
    this.componentName = builder.componentName;
    this.componentNamespace = builder.componentNamespace;
    this.componentVersion = builder.componentVersion;

    if (builder.policyNotifications != null) {
      builder.policyNotifications.forEach(policyNotification -> {
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

  /**
   * Creates a new Builder for ProxyRepositoryComponentTelemetry.
   *
   * @return A new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public ProxyRepositoryComponentTelemetry(
      final String accountId,
      final String repositoryManagerId,
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final RepositoryComponentTelemetryEventType eventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final List<PolicyNotification> policyNotifications)
  {
    this(accountId, repositoryManagerId, proxyRepositoryComponent, eventType, releaseQuarantineType, null,
        policyNotifications);
  }

  public ProxyRepositoryComponentTelemetry(
      final String accountId,
      final String repositoryManagerId,
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final RepositoryComponentTelemetryEventType eventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    this.accountId = accountId;
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = proxyRepositoryComponent.getRepositoryId();
    this.repositoryName = null;
    this.repositoryType = null;
    this.componentFormat =
        proxyRepositoryComponent.getComponentIdentifier() == null
            ? null
            : proxyRepositoryComponent.getComponentIdentifier()
                .getFormat();
    this.componentHash =
        proxyRepositoryComponent.getHash() != null
            ? HdsClientAnalytics.obfuscate(proxyRepositoryComponent.getHash())
            : null;
    this.eventType = eventType.getDescription();
    this.quarantineTime =
        proxyRepositoryComponent.getQuarantineTime() == null
            ? null
            : proxyRepositoryComponent.getQuarantineTime()
                .toInstant()
                .toEpochMilli();
    this.releaseQuarantineTime =
        proxyRepositoryComponent.getUnquarantineTime() == null
            ? null
            : proxyRepositoryComponent.getUnquarantineTime()
                .toInstant()
                .toEpochMilli();
    this.releaseQuarantineType = releaseQuarantineType == null ? null : releaseQuarantineType.getDescription();
    this.releaseReason = releaseReason;

    // Extract component identifier details using PackageUrlIdentifier for robust cross-format extraction
    if (proxyRepositoryComponent.getComponentIdentifier() != null) {
      this.componentIdentifier =
          ComponentIdentifierAdapter.toJson(proxyRepositoryComponent.getComponentIdentifier());
      PackageUrlIdentifier purl =
          PackageUrlIdentifier.fromComponentIdentifier(proxyRepositoryComponent.getComponentIdentifier());
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

  public ProxyRepositoryComponentTelemetry(
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

  public ProxyRepositoryComponentTelemetry(
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
    this.accountId = null;
    this.repositoryManagerId = repositoryManagerId;
    this.repositoryId = repositoryId;
    this.repositoryName = null;
    this.repositoryType = null;
    this.componentFormat = componentFormat;
    this.componentHash = componentHash != null ? HdsClientAnalytics.obfuscate(componentHash) : null;
    this.eventType = repositoryComponentTelemetryEventType.getDescription();
    this.quarantineTime = quarantineTime;
    this.releaseQuarantineTime = releaseQuarantineTime;
    this.releaseQuarantineType = releaseQuarantineType;
    this.releaseReason = releaseReason;

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

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRepositoryType() {
    return repositoryType;
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

  /**
   * Builder for ProxyRepositoryComponentTelemetry using Java 17 style.
   * Provides a fluent API for constructing telemetry objects with optional parameters.
   */
  public static class Builder
  {
    private String accountId;

    private String repositoryManagerId;

    private String repositoryId;

    private String repositoryName;

    private String repositoryType;

    private String componentFormat;

    private String componentHash;

    private String eventType;

    private Long quarantineTime;

    private Long releaseQuarantineTime;

    private String releaseQuarantineType;

    private String releaseReason;

    private String componentIdentifier;

    private String componentName;

    private String componentNamespace;

    private String componentVersion;

    private List<PolicyNotification> policyNotifications;

    private TelemetryDataObfuscator telemetryDataObfuscator;

    private Builder() {
    }

    public Builder accountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder repositoryManagerId(String repositoryManagerId) {
      this.repositoryManagerId = repositoryManagerId;
      return this;
    }

    public Builder repositoryId(String repositoryId) {
      this.repositoryId = repositoryId;
      return this;
    }

    public Builder repositoryName(String repositoryName) {
      this.repositoryName = repositoryName;
      return this;
    }

    public Builder repositoryType(String repositoryType) {
      this.repositoryType = repositoryType;
      return this;
    }

    public Builder componentFormat(String componentFormat) {
      this.componentFormat = componentFormat;
      return this;
    }

    public Builder componentHash(String componentHash) {
      this.componentHash = componentHash;
      return this;
    }

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder eventType(RepositoryComponentTelemetryEventType eventType) {
      this.eventType = eventType != null ? eventType.getDescription() : null;
      return this;
    }

    public Builder quarantineTime(Long quarantineTime) {
      this.quarantineTime = quarantineTime;
      return this;
    }

    public Builder releaseQuarantineTime(Long releaseQuarantineTime) {
      this.releaseQuarantineTime = releaseQuarantineTime;
      return this;
    }

    public Builder releaseQuarantineType(String releaseQuarantineType) {
      this.releaseQuarantineType = releaseQuarantineType;
      return this;
    }

    public Builder releaseQuarantineType(ReleaseQuarantineType releaseQuarantineType) {
      this.releaseQuarantineType = releaseQuarantineType != null ? releaseQuarantineType.getDescription() : null;
      return this;
    }

    public Builder releaseReason(String releaseReason) {
      this.releaseReason = releaseReason;
      return this;
    }

    public Builder componentIdentifier(String componentIdentifier) {
      this.componentIdentifier = componentIdentifier;
      return this;
    }

    public Builder componentName(String componentName) {
      this.componentName = componentName;
      return this;
    }

    public Builder componentNamespace(String componentNamespace) {
      this.componentNamespace = componentNamespace;
      return this;
    }

    public Builder componentVersion(String componentVersion) {
      this.componentVersion = componentVersion;
      return this;
    }

    public Builder policyNotifications(List<PolicyNotification> policyNotifications) {
      this.policyNotifications = policyNotifications;
      return this;
    }

    public Builder telemetryDataObfuscator(TelemetryDataObfuscator telemetryDataObfuscator) {
      this.telemetryDataObfuscator = telemetryDataObfuscator;
      return this;
    }

    /**
     * Populates builder fields from a ProxyRepositoryComponent.
     * This is a convenience method for the common case of creating telemetry from a component.
     *
     * @param proxyRepositoryComponent The repository component to extract data from
     * @return This builder instance
     */
    public Builder fromRepositoryComponent(ProxyRepositoryComponent proxyRepositoryComponent) {
      if (proxyRepositoryComponent == null) {
        return this;
      }

      this.repositoryId = proxyRepositoryComponent.getRepositoryId();
      this.componentHash = proxyRepositoryComponent.getHash();

      if (proxyRepositoryComponent.getComponentIdentifier() != null) {
        this.componentFormat = proxyRepositoryComponent.getComponentIdentifier().getFormat();
        this.componentIdentifier = ComponentIdentifierAdapter.toJson(proxyRepositoryComponent.getComponentIdentifier());
        PackageUrlIdentifier purl =
            PackageUrlIdentifier.fromComponentIdentifier(proxyRepositoryComponent.getComponentIdentifier());
        this.componentName = purl.getName();
        this.componentNamespace = purl.getNamespace();
        this.componentVersion = purl.getVersion();
      }

      if (proxyRepositoryComponent.getQuarantineTime() != null) {
        this.quarantineTime = proxyRepositoryComponent.getQuarantineTime().toInstant().toEpochMilli();
      }

      if (proxyRepositoryComponent.getUnquarantineTime() != null) {
        this.releaseQuarantineTime = proxyRepositoryComponent.getUnquarantineTime().toInstant().toEpochMilli();
      }

      return this;
    }

    public ProxyRepositoryComponentTelemetry build() {
      return new ProxyRepositoryComponentTelemetry(this);
    }
  }
}
