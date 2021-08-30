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
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
public class RepositoryComponentTelemetryCreator
{
  public static final String REPOSITORY_COMPONENT_TELEMETRY = "repository_component";

  public static final String POLICY_VIOLATION_TELEMETRY = "policy_violations";

  private final TelemetrySender telemetrySender;

  @Inject
  public RepositoryComponentTelemetryCreator(final TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType)
  {
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, null, Collections.emptyList());
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, null, policyNotifications);
  }

  public void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType)
  {
    sendRepositoryComponentTelemetry(repositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, releaseQuarantineType, Collections.emptyList());
  }

  private void sendRepositoryComponentTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final List<PolicyNotification> policyNotifications)
  {
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream().map(PolicyViolationTelemetry::new).collect(Collectors.toList());
    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(repositoryManagerId, repositoryComponent,
            repositoryComponentTelemetryEventType, releaseQuarantineType, policyNotifications);

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);
    telemetryData.getAttributes().put(POLICY_VIOLATION_TELEMETRY, policyViolationTelemetries);
    telemetryData.getAttributes().put(REPOSITORY_COMPONENT_TELEMETRY, repositoryComponentTelemetry);
    telemetrySender.send(telemetryData);
  }
}
