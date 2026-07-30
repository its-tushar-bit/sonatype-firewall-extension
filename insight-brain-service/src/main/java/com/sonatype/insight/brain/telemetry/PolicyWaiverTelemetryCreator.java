/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

@Named
public class PolicyWaiverTelemetryCreator
{
  public static final String POLICY_WAIVER_TELEMETRY = "policy_waiver";

  public static final String POLICY_VIOLATION_TELEMETRY = "policy_violations";

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public PolicyWaiverTelemetryCreator(PolicyWaiverReasonDAO policyWaiverReasonDAO, TelemetrySender telemetrySender) {
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.telemetrySender = telemetrySender;
  }

  public void sendRepositoryWaiverTelemetry(
      final PolicyWaiver policyWaiver,
      final ProxyRepositoryPolicyViolation policyViolation)
  {
    final PolicyWaiverTelemetry policyWaiverTelemetry =
        new PolicyWaiverTelemetry(policyWaiver, OwnerType.REPOSITORY.toString(),
            policyViolation.getComponentIdentifier(), StageTypes.PROXY.getId(), policyViolation.getTime());
    final PolicyViolationTelemetry policyViolationTelemetry = new PolicyViolationTelemetry(policyViolation);
    sendWaiverTelemetry(policyWaiver, policyWaiverTelemetry, policyViolationTelemetry);
  }

  public void sendWaiverTelemetryForOwnerType(
      final PolicyWaiver policyWaiver,
      final OwnerType ownerType,
      final AbstractPolicyViolation policyViolation)
  {
    final PolicyWaiverTelemetry policyWaiverTelemetry = new PolicyWaiverTelemetry(policyWaiver, ownerType.toString(),
        policyViolation.getComponentIdentifier(), policyViolation.getStageTypeId(), policyViolation.getOpenTime());
    final PolicyViolationTelemetry policyViolationTelemetry = new PolicyViolationTelemetry(policyViolation);
    sendWaiverTelemetry(policyWaiver, policyWaiverTelemetry, policyViolationTelemetry);
  }

  private void sendWaiverTelemetry(
      PolicyWaiver policyWaiver,
      PolicyWaiverTelemetry policyWaiverTelemetry,
      PolicyViolationTelemetry policyViolationTelemetry)
  {
    policyWaiverTelemetry.withWaiverReason(getWaiverReason(policyWaiver));

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_WAIVER);
    telemetryData.put(POLICY_WAIVER_TELEMETRY, policyWaiverTelemetry);
    telemetryData.put(POLICY_VIOLATION_TELEMETRY, ImmutableList.of(policyViolationTelemetry));
    telemetrySender.send(telemetryData);
  }

  private PolicyWaiverReason getWaiverReason(PolicyWaiver policyWaiver) {
    if (null == policyWaiver || StringUtils.isBlank(policyWaiver.getWaiverReasonId())) {
      return null;
    }
    return policyWaiverReasonDAO.getById(policyWaiver.getWaiverReasonId());
  }
}
