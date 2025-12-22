/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
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

  @Inject
  public RepositoryComponentTelemetryCreator(
      final TelemetrySender telemetrySender,
      final PendoCache pendoCache)
  {
    this.telemetrySender = telemetrySender;
    this.pendoCache = pendoCache;
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
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, null, null, policyNotifications);
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
    sendRepositoryComponentTelemetryInternal(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications);
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
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    final String accountId = getAccountId();
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream().map(PolicyViolationTelemetry::new).collect(Collectors.toList());
    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(accountId, repositoryManagerId, repositoryComponent,
            repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications);

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);
    telemetryData.getAttributes().put(POLICY_VIOLATION_TELEMETRY, policyViolationTelemetries);
    telemetryData.getAttributes().put(REPOSITORY_COMPONENT_TELEMETRY, repositoryComponentTelemetry);

    telemetrySender.send(telemetryData);
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
