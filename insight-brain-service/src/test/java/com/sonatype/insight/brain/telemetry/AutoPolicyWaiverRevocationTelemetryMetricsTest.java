/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverRevocationTelemetry.AutoPolicyWaiverRevocationAction;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverRevocationTelemetryMetricsTest
    extends AbstractComponentTest
{
  @Inject
  private AutoPolicyWaiverRevocationTelemetryMetrics metrics;

  @Test
  public void testComputeStatsAndReset_shouldReturnWhatWasCollected() {
    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation = createAutoPolicyWaiverRevocation(
        "revocationId",
        "ownerId",
        "autoPolicyWaiverId");

    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation2 = createAutoPolicyWaiverRevocation(
        "revocationId2",
        "ownerId",
        "autoPolicyWaiverId2");

    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);
    metrics.collect(autoPolicyWaiverRevocation2, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation2, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);

    List<TelemetryData> stats = metrics.computeStatsAndReset();

    assertThat(stats).hasSize(4);
    assertThat(stats).satisfiesExactlyInAnyOrder(
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverRevocation,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverRevocationAction.CREATE,
            2),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverRevocation,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverRevocationAction.DELETE,
            2),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverRevocation2,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverRevocationAction.CREATE,
            1),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverRevocation2,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverRevocationAction.DELETE,
            1)
    );
  }

  @Test
  public void testComputeStatsAndReset_shouldResetReliably() {
    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation = createAutoPolicyWaiverRevocation(
        "revocationId",
        "ownerId",
        "autoPolicyWaiverId");

    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation2 = createAutoPolicyWaiverRevocation(
        "revocationId2",
        "ownerId",
        "autoPolicyWaiverId2");

    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);
    metrics.collect(autoPolicyWaiverRevocation2, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.CREATE);
    metrics.collect(autoPolicyWaiverRevocation2, OwnerType.APPLICATION, AutoPolicyWaiverRevocationAction.DELETE);

    metrics.computeStatsAndReset();

    // Nothing left before collecting again
    assertThat(metrics.computeStatsAndReset()).isEmpty();
  }

  private AutoPolicyWaiverRevocation createAutoPolicyWaiverRevocation(
      String id,
      String ownerId,
      String autoPolicyWaiverId)
  {
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation();
    revocation.setId(id);
    revocation.setOwnerId(ownerId);
    revocation.setCreateTime(Date.from(Instant.now()));
    revocation.setThreatLevel(5);
    revocation.setAutoPolicyWaiverId(autoPolicyWaiverId);
    return revocation;
  }

  private void assertTelemetryData(TelemetryData data,
                                   TelemetryPurpose purpose,
                                   AutoPolicyWaiverRevocation autoPolicyWaiverRevocation,
                                   int autoPolicyWaiverRevocationThreatLevel,
                                   OwnerType ownerType,
                                   String applicationId,
                                   String organizationId,
                                   AutoPolicyWaiverRevocationAction autoPolicyWaiverRevocationAction,
                                   long autoPolicyWaiverCountForSameAction)
  {
    assertThat(data.getPurpose()).isEqualTo(purpose);
    assertThat(data.getAttributes().get(AutoPolicyWaiverRevocationTelemetryMetrics.AUTO_POLICY_WAIVER_REVOCATION_ID))
        .isEqualTo(autoPolicyWaiverRevocation.getId());
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverRevocationTelemetryMetrics.AUTO_POLICY_WAIVER_REVOCATION_ACTION))
        .isEqualTo(autoPolicyWaiverRevocationAction);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverRevocationTelemetryMetrics.AUTO_POLICY_WAIVER_REVOCATION_THREAD_LEVEL))
        .isEqualTo(autoPolicyWaiverRevocationThreatLevel);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverRevocationTelemetryMetrics.AUTO_POLICY_WAIVER_REVOCATION_AUTO_POLICY_WAIVER_ID))
        .isEqualTo(autoPolicyWaiverRevocation.getAutoPolicyWaiverId());
    assertThat(data.getAttributes().get(AutoPolicyWaiverRevocationTelemetryMetrics.OWNER_TYPE))
        .isEqualTo(ownerType);
    assertThat(data.getAttributes().get(AutoPolicyWaiverRevocationTelemetryMetrics.APPLICATION_ID))
        .isEqualTo(applicationId != null ? HdsClientAnalytics.obfuscate(applicationId) : null);
    assertThat(data.getAttributes().get(AutoPolicyWaiverRevocationTelemetryMetrics.ORGANIZATION_ID))
        .isEqualTo(organizationId != null ? HdsClientAnalytics.obfuscate(organizationId) : null);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverRevocationTelemetryMetrics.AUTO_POLICY_WAIVER_REVOCATION_COUNT_FOR_SAME_ACTION))
        .isEqualTo(autoPolicyWaiverCountForSameAction);
  }
}
