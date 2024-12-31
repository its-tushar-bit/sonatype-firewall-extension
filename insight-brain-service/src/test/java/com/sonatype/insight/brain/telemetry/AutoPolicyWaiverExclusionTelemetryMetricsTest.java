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
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverExclusionTelemetryMetricsTest
    extends AbstractComponentTest
{
  @Inject
  private AutoPolicyWaiverExclusionTelemetryMetrics metrics;

  @Test
  public void testComputeStatsAndReset_shouldReturnWhatWasCollected() {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = createAutoPolicyWaiverExclusion(
        "revocationId",
        "ownerId",
        "autoPolicyWaiverId");

    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion2 = createAutoPolicyWaiverExclusion(
        "revocationId2",
        "ownerId",
        "autoPolicyWaiverId2");

    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);
    metrics.collect(autoPolicyWaiverExclusion2, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion2, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);

    List<TelemetryData> stats = metrics.computeStatsAndReset();

    assertThat(stats).hasSize(4);
    assertThat(stats).satisfiesExactlyInAnyOrder(
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverExclusion,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverExclusionAction.CREATE,
            2),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverExclusion,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverExclusionAction.DELETE,
            2),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverExclusion2,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverExclusionAction.CREATE,
            1),
        data -> assertTelemetryData(data,
            TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS,
            autoPolicyWaiverExclusion2,
            5,
            OwnerType.APPLICATION,
            "ownerId",
            null,
            AutoPolicyWaiverExclusionAction.DELETE,
            1)
    );
  }

  @Test
  public void testComputeStatsAndReset_shouldResetReliably() {
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = createAutoPolicyWaiverExclusion(
        "revocationId",
        "ownerId",
        "autoPolicyWaiverId");

    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion2 = createAutoPolicyWaiverExclusion(
        "revocationId2",
        "ownerId",
        "autoPolicyWaiverId2");

    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);
    metrics.collect(autoPolicyWaiverExclusion2, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.CREATE);
    metrics.collect(autoPolicyWaiverExclusion2, OwnerType.APPLICATION, AutoPolicyWaiverExclusionAction.DELETE);

    metrics.computeStatsAndReset();

    // Nothing left before collecting again
    assertThat(metrics.computeStatsAndReset()).isEmpty();
  }

  private AutoPolicyWaiverExclusion createAutoPolicyWaiverExclusion(
      String id,
      String ownerId,
      String autoPolicyWaiverId)
  {
    AutoPolicyWaiverExclusion exclusion = new AutoPolicyWaiverExclusion();
    exclusion.setId(id);
    exclusion.setOwnerId(ownerId);
    exclusion.setCreateTime(Date.from(Instant.now()));
    exclusion.setThreatLevel(5);
    exclusion.setAutoPolicyWaiverId(autoPolicyWaiverId);
    return exclusion;
  }

  private void assertTelemetryData(TelemetryData data,
                                   TelemetryPurpose purpose,
                                   AutoPolicyWaiverExclusion autoPolicyWaiverExclusion,
                                   int autoPolicyWaiverRevocationThreatLevel,
                                   OwnerType ownerType,
                                   String applicationId,
                                   String organizationId,
                                   AutoPolicyWaiverExclusionAction autoPolicyWaiverExclusionAction,
                                   long autoPolicyWaiverCountForSameAction)
  {
    assertThat(data.getPurpose()).isEqualTo(purpose);
    assertThat(data.getAttributes().get(AutoPolicyWaiverExclusionTelemetryMetrics.AUTO_POLICY_WAIVER_EXCLUSION_ID))
        .isEqualTo(autoPolicyWaiverExclusion.getId());
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverExclusionTelemetryMetrics.AUTO_POLICY_WAIVER_EXCLUSION_ACTION))
        .isEqualTo(autoPolicyWaiverExclusionAction);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverExclusionTelemetryMetrics.AUTO_POLICY_WAIVER_EXCLUSION_THREAD_LEVEL))
        .isEqualTo(autoPolicyWaiverRevocationThreatLevel);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverExclusionTelemetryMetrics.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID))
        .isEqualTo(autoPolicyWaiverExclusion.getAutoPolicyWaiverId());
    assertThat(data.getAttributes().get(AutoPolicyWaiverExclusionTelemetryMetrics.OWNER_TYPE))
        .isEqualTo(ownerType);
    assertThat(data.getAttributes().get(AutoPolicyWaiverExclusionTelemetryMetrics.APPLICATION_ID))
        .isEqualTo(applicationId != null ? HdsClientAnalytics.obfuscate(applicationId) : null);
    assertThat(data.getAttributes().get(AutoPolicyWaiverExclusionTelemetryMetrics.ORGANIZATION_ID))
        .isEqualTo(organizationId != null ? HdsClientAnalytics.obfuscate(organizationId) : null);
    assertThat(data.getAttributes()
        .get(AutoPolicyWaiverExclusionTelemetryMetrics.AUTO_POLICY_WAIVER_EXCLUSION_COUNT_FOR_SAME_ACTION))
        .isEqualTo(autoPolicyWaiverCountForSameAction);
  }
}
