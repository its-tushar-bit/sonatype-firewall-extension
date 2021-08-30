/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ImmutableList;

@Named
public class PolicyWaiverTelemetryCreator
{
  public static final String POLICY_WAIVER_TELEMETRY = "policy_waiver";

  public static final String POLICY_VIOLATION_TELEMETRY = "policy_violations";

  private final TelemetrySender telemetrySender;

  @Inject
  public PolicyWaiverTelemetryCreator(final TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void sendRepositoryWaiverTelemetry(
      final PolicyWaiver policyWaiver,
      final RepositoryPolicyViolation policyViolation)
  {
    final PolicyWaiverTelemetry policyWaiverTelemetry =
        new PolicyWaiverTelemetry(policyWaiver, OwnerType.REPOSITORY.toString(),
            policyViolation.getComponentIdentifier(), StageTypes.PROXY.getId(), policyViolation.getTime());
    final PolicyViolationTelemetry policyViolationTelemetry = new PolicyViolationTelemetry(policyViolation);
    sendWaiverTelemetry(policyWaiverTelemetry, policyViolationTelemetry);
  }

  public void sendWaiverTelemetryForOwnerType(
      final PolicyWaiver policyWaiver,
      final OwnerType ownerType,
      final PolicyViolation policyViolation)
  {
    final PolicyWaiverTelemetry policyWaiverTelemetry = new PolicyWaiverTelemetry(policyWaiver, ownerType.toString(),
        policyViolation.getComponentIdentifier(), policyViolation.getStageTypeId(), policyViolation.getOpenTime());
    final PolicyViolationTelemetry policyViolationTelemetry = new PolicyViolationTelemetry(policyViolation);
    sendWaiverTelemetry(policyWaiverTelemetry, policyViolationTelemetry);
  }

  public void sendWaiverTelemetryWithoutViolationInformation(
      final PolicyWaiver policyWaiver,
      final OwnerType ownerType)
  {
    final PolicyWaiverTelemetry policyWaiverTelemetry = new PolicyWaiverTelemetry(policyWaiver, ownerType.toString());
    final PolicyViolationTelemetry policyViolationTelemetry =
        new PolicyViolationTelemetry(policyWaiver.getConstraintFacts());
    sendWaiverTelemetry(policyWaiverTelemetry, policyViolationTelemetry);
  }

  private void sendWaiverTelemetry(
      final PolicyWaiverTelemetry policyWaiverTelemetry,
      PolicyViolationTelemetry policyViolationTelemetry)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_WAIVER);
    telemetryData.getAttributes().put(POLICY_WAIVER_TELEMETRY, policyWaiverTelemetry);
    telemetryData.getAttributes().put(POLICY_VIOLATION_TELEMETRY, ImmutableList.of(policyViolationTelemetry));
    telemetrySender.send(telemetryData);
  }
}
