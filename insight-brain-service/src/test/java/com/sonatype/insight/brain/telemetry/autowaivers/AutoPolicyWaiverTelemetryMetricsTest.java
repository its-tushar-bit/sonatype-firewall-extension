/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.autowaivers.AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverTelemetryMetricsTest extends AbstractComponentTest
{
  @Inject
  private AutoPolicyWaiverTelemetryMetrics metrics;

  @Test
  public void testComputeStatsAndReset_shouldReturnWhatWasCollected() {
    AutoPolicyWaiver autoPolicyWaiver1 = createAutoPolicyWaiver("autoPolicyWaiver1", "owner1",
        1);
    AutoPolicyWaiver autoPolicyWaiver2 = createAutoPolicyWaiver("autoPolicyWaiver2", "owner2", 2);
    AutoPolicyWaiver autoPolicyWaiver3 = createAutoPolicyWaiver("autoPolicyWaiver3", "owner3", 3);

    PolicyViolation policyViolation1 = createPolicyViolation("policyViolation1", "autoPolicyWaiver1", "application1");
    PolicyViolation policyViolation2 = createPolicyViolation("policyViolation2", "autoPolicyWaiver2", "application2");
    PolicyViolation policyViolation3 = createPolicyViolation("policyViolation3", "autoPolicyWaiver3", "application3");

    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.CREATE, null);
    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.UPDATE, null);
    metrics.collect(autoPolicyWaiver1, OwnerType.ORGANIZATION, AutoPolicyWaiverAction.UPDATE,
        null);
    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.DELETE, null);
    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation1);
    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation1);
    metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation1);
    metrics.collect(autoPolicyWaiver2, OwnerType.APPLICATION, AutoPolicyWaiverAction.CREATE, null);
    metrics.collect(autoPolicyWaiver2, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation2);
    metrics.collect(autoPolicyWaiver3, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation3);
    metrics.collect(autoPolicyWaiver3, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
        policyViolation3);
    metrics.collect(autoPolicyWaiver3, OwnerType.ORGANIZATION, AutoPolicyWaiverAction.APPLY,
        policyViolation3);

    List<TelemetryData> stats = metrics.computeStatsAndReset();

    assertThat(stats).hasSize(9);
    assertThat(stats).satisfiesExactlyInAnyOrder(
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver1,
            true,
            true,
            1,
            OwnerType.APPLICATION,
            "owner1",
            null,
            AutoPolicyWaiverAction.CREATE,
            null,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver1,
            true,
            true,
            1,
            OwnerType.APPLICATION,
            "owner1",
            null,
            AutoPolicyWaiverAction.UPDATE,
            null,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver1,
            true,
            true,
            1,
            OwnerType.ORGANIZATION,
            null,
            "owner1",
            AutoPolicyWaiverAction.UPDATE,
            null,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver1,
            true,
            true,
            1,
            OwnerType.APPLICATION,
            "owner1",
            null,
            AutoPolicyWaiverAction.DELETE,
            null,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver1,
            true,
            true,
            1,
            OwnerType.APPLICATION,
            "owner1",
            null,
            AutoPolicyWaiverAction.APPLY,
            policyViolation1,
            3L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver2,
            true,
            true,
            2,
            OwnerType.APPLICATION,
            "owner2",
            null,
            AutoPolicyWaiverAction.CREATE,
            null,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver2,
            true,
            true,
            2,
            OwnerType.APPLICATION,
            "owner2",
            null,
            AutoPolicyWaiverAction.APPLY,
            policyViolation2,
            1L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver3,
            true,
            true,
            3,
            OwnerType.APPLICATION,
            "owner3",
            null,
            AutoPolicyWaiverAction.APPLY,
            policyViolation3,
            2L),
        data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
            autoPolicyWaiver3,
            true,
            true,
            3,
            OwnerType.ORGANIZATION,
            null,
            "owner3",
            AutoPolicyWaiverAction.APPLY,
            policyViolation3,
            1L)
    );
  }

  @Test
  public void testComputeStatsAndReset_shouldResetReliably() {
    AutoPolicyWaiver autoPolicyWaiver1 = createAutoPolicyWaiver("autoPolicyWaiver1", "owner1", 1);
    AutoPolicyWaiver autoPolicyWaiver2 = createAutoPolicyWaiver("autoPolicyWaiver2", "owner2", 2);
    AutoPolicyWaiver autoPolicyWaiver3 = createAutoPolicyWaiver("autoPolicyWaiver3", "owner3", 3);

    PolicyViolation policyViolation2 = createPolicyViolation("policyViolation2", "autoPolicyWaiver2", "application2");
    PolicyViolation policyViolation3 = createPolicyViolation("policyViolation3", "autoPolicyWaiver3", "application3");

    for (int i = 0; i < 10; i++) {
      metrics.collect(autoPolicyWaiver1, OwnerType.APPLICATION, AutoPolicyWaiverAction.CREATE, null);
      metrics.collect(autoPolicyWaiver2, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
          policyViolation2);
      metrics.collect(autoPolicyWaiver2, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
          policyViolation2);
      metrics.collect(autoPolicyWaiver3, OwnerType.APPLICATION, AutoPolicyWaiverAction.APPLY,
          policyViolation3);

      List<TelemetryData> stats = metrics.computeStatsAndReset();
      assertThat(stats).hasSize(3);
      assertThat(stats).satisfiesExactlyInAnyOrder(
          data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
              autoPolicyWaiver1,
              true,
              true,
              1,
              OwnerType.APPLICATION,
              "owner1",
              null,
              AutoPolicyWaiverAction.CREATE,
              null,
              1L),
          data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
              autoPolicyWaiver2,
              true,
              true,
              2,
              OwnerType.APPLICATION,
              "owner2",
              null,
              AutoPolicyWaiverAction.APPLY,
              policyViolation2,
              2L),
          data -> assertTelemetryData(data, TelemetryPurpose.AUTO_POLICY_WAIVER,
              autoPolicyWaiver3,
              true,
              true,
              3,
              OwnerType.APPLICATION,
              "owner3",
              null,
              AutoPolicyWaiverAction.APPLY,
              policyViolation3,
              1L)
      );

      // Nothing left before collecting again
      assertThat(metrics.computeStatsAndReset()).isEmpty();
    }
  }

  private AutoPolicyWaiver createAutoPolicyWaiver(String autoPolicyWaiverId, String ownerId,
                                                  int threatLevel)
  {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setId(autoPolicyWaiverId);
    autoPolicyWaiver.setOwnerId(ownerId);
    autoPolicyWaiver.setThreatLevel(threatLevel);
    autoPolicyWaiver.setCreatorId("creatorId");
    autoPolicyWaiver.setCreateTime(new Date());
    autoPolicyWaiver.setPathForward(true);
    autoPolicyWaiver.setReachability(true);

    return autoPolicyWaiver;
  }

  private PolicyViolation createPolicyViolation(String policyViolationId,
                                                String autoPolicyWaiverId,
                                                String applicationId)
  {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setAutoPolicyWaiverId(autoPolicyWaiverId);
    policyViolation.setId(policyViolationId);
    policyViolation.setFilename("filename");
    policyViolation.setApplicationId(applicationId);
    policyViolation.setLegacyViolationApplied(false);
    policyViolation.setFixTime(null);
    policyViolation.setLegacyViolationTime(null);
    policyViolation.setOpenTime(new Date());
    policyViolation.setReachabilityStatus(ReachabilityStatus.REACHABLE);
    policyViolation.setStageTypeId("build");

    return policyViolation;
  }

  private void assertTelemetryData(TelemetryData data,
                                   TelemetryPurpose purpose,
                                   AutoPolicyWaiver autoPolicyWaiver,
                                   boolean autoPolicyWaiverReachability,
                                   boolean autoPolicyWaiverPathForward,
                                   int autoPolicyWaiverThreatLevel,
                                   OwnerType ownerType,
                                   String applicationId,
                                   String organizationId,
                                   AutoPolicyWaiverAction autoPolicyWaiverAction,
                                   PolicyViolation policyViolation,
                                   long autoPolicyWaiverCountForSameAction)
  {
    assertThat(data.getPurpose()).isEqualTo(purpose);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_ID))
        .isEqualTo(autoPolicyWaiver.getId());
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_REACHABILITY))
        .isEqualTo(autoPolicyWaiverReachability);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_PATH_FORWARD))
        .isEqualTo(autoPolicyWaiverPathForward);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_THREAT_LEVEL))
        .isEqualTo(autoPolicyWaiverThreatLevel);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.OWNER_TYPE)).isEqualTo(ownerType);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.APPLICATION_ID))
        .isEqualTo(applicationId != null ? HdsClientAnalytics.obfuscate(applicationId) : null);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.ORGANIZATION_ID))
        .isEqualTo(organizationId != null ? HdsClientAnalytics.obfuscate(organizationId) : null);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_ACTION))
        .isEqualTo(autoPolicyWaiverAction.toString());
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID))
        .isEqualTo(policyViolation != null ? policyViolation.getHash() : null);
    assertThat(data.getAttributes().get(AutoPolicyWaiverTelemetryMetrics.AUTO_POLICY_WAIVER_COUNT_FOR_SAME_ACTION))
        .isEqualTo(autoPolicyWaiverCountForSameAction);
  }
}
