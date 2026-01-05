/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryComponentTelemetryCreator
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryComponentTelemetryCreator.class);

  public static final String REPOSITORY_COMPONENT_TELEMETRY = "repository_component";

  public static final String POLICY_VIOLATION_TELEMETRY = "policy_violations";

  private final TelemetrySender telemetrySender;

  private final PendoCache pendoCache;

  private final RepositoryDAO repositoryDAO;

  private final TelemetryDataObfuscator telemetryDataObfuscator;

  @Inject
  public RepositoryComponentTelemetryCreator(
      final TelemetrySender telemetrySender,
      final PendoCache pendoCache,
      final RepositoryDAO repositoryDAO,
      final TelemetryDataObfuscator telemetryDataObfuscator)
  {
    this.telemetrySender = telemetrySender;
    this.pendoCache = pendoCache;
    this.repositoryDAO = repositoryDAO;
    this.telemetryDataObfuscator = telemetryDataObfuscator;
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType)
  {
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, null, null, Collections.emptyList());
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, component);
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId, repositoryName,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, component);
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType)
  {
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, releaseQuarantineType, null, Collections.emptyList());
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId, repositoryName,
        repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(TelemetryData repositoryComponentTelemetry) {
    if (repositoryComponentTelemetry == null ||
        !repositoryComponentTelemetry.getPurpose().equals(TelemetryPurpose.REPOSITORY_COMPONENT)) {
      log.debug("TelemetryData is not for REPOSITORY_COMPONENT purpose. Skipping telemetry send.");
      return;
    }
    telemetrySender.send(repositoryComponentTelemetry);
  }

  private void sendRepositoryComponentTelemetryInternal(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    final String accountId = getAccountId();

    // If repositoryName is not provided but we have repositoryId, look it up
    // This is a fallback - callers should pass repositoryName to avoid database lookup
    if (repositoryName == null && repositoryComponent != null && repositoryComponent.getRepositoryId() != null) {
      log.warn("repositoryName not provided for repositoryId {}. Performing fallback database lookup. " +
          "Caller should pass repositoryName explicitly to avoid performance impact.",
          repositoryComponent.getRepositoryId());
      repositoryName = lookupRepositoryName(repositoryComponent.getRepositoryId());
    }

    // Create policy violation telemetry with Component data to fill missing CVE fields
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream()
            .map(pv -> PolicyViolationTelemetry.createWithComponent(pv, component))
            .collect(Collectors.toList());

    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        RepositoryComponentTelemetry.builder()
            .accountId(accountId)
            .repositoryManagerId(repositoryManagerId)
            .repositoryName(repositoryName)
            .telemetryDataObfuscator(telemetryDataObfuscator)
            .fromRepositoryComponent(repositoryComponent)
            .eventType(repositoryComponentTelemetryEventType)
            .releaseQuarantineType(releaseQuarantineType)
            .releaseReason(releaseReason)
            .policyNotifications(policyNotifications)
            .build();

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);
    telemetryData.getAttributes().put(POLICY_VIOLATION_TELEMETRY, policyViolationTelemetries);
    telemetryData.getAttributes().put(REPOSITORY_COMPONENT_TELEMETRY, repositoryComponentTelemetry);

    telemetrySender.send(telemetryData);
  }

  /**
   * Looks up the repository name (publicId) from the database given a repository ID.
   * This ensures repositoryName is always populated when we have a repositoryId.
   *
   * @param repositoryId The internal repository ID
   * @return The repository's public ID (name), or null if not found
   */
  private String lookupRepositoryName(final String repositoryId) {
    try {
      List<Repository> repositories = repositoryDAO.getByIds(Set.of(repositoryId));
      if (repositories != null && !repositories.isEmpty()) {
        return repositories.get(0).getPublicId();
      }
    }
    catch (Exception e) {
      log.debug("Failed to lookup repository name for repositoryId: {}", repositoryId, e);
    }
    return null;
  }

  /**
   * Retrieves the Salesforce Account ID from HDS telemetry properties.
   * The accountId is tenant-specific and cached per-tenant by PendoCache.
   *
   * @return Salesforce Account ID (e.g., "001QO00000sRp3lYAC") or null if not available
   */
  private String getAccountId() {
    try {
      CustomerTelemetryProperties telemetryProperties = pendoCache.getCustomerTelemetryProperties();
      if (telemetryProperties != null && telemetryProperties.segmentAttributes != null) {
        Object accountIdObj = telemetryProperties.segmentAttributes.get("iq_accountId");
        if (accountIdObj != null && !accountIdObj.toString().startsWith("UNKNOWN-")) {
          return accountIdObj.toString();
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to retrieve Salesforce accountId for telemetry", e);
    }
    return null;
  }
}
