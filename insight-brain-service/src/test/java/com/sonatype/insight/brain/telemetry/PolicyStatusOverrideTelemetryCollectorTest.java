/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyStatusOverrideTelemetryCollectorTest
    extends AbstractComponentTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @Inject
  private PolicyStatusOverrideTelemetryCollector telemetryCollector;

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_STATUS_OVERRIDE);
  }

  @Test
  public void testCollectData_SecurityVulnerabilityOverrideCounts() {
    assertThat(telemetryCollector.collectData().getAttributes())
        .containsEntry(PolicyStatusOverrideTelemetryCollector.SECURITY_VULNERABILITY_OVERRIDE_COUNT, "0");

    tempEntity.newSecurityVulnerabilityOverride("ownerId1", "hash1", "source1", "referenceId1",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    tempEntity.newSecurityVulnerabilityOverride("ownerId2", "hash2", "source2", "referenceId2",
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE);
    tempEntity.newSecurityVulnerabilityOverride("ownerId3", "hash3", "source3", "referenceId3",
        SecurityVulnerabilityOverrideStatus.CONFIRMED);

    assertThat(telemetryCollector.collectData().getAttributes())
        .containsEntry(PolicyStatusOverrideTelemetryCollector.SECURITY_VULNERABILITY_OVERRIDE_COUNT, "3");
  }

  @Test
  public void testCollectData_PolicyWaiverCount() {
    assertThat(telemetryCollector.collectData().getAttributes())
        .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "0");
    Organization organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    Application application = tempEntity
        .newApplication("PolicyWaiverCount-AppName", "PolicyWaiverCount-AppPublicId", organization.getId());
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);
    Policy policy3 = tempEntity.newPolicy(organization);
    tempEntity.newWaiver(policy1.getId(), application.getId());
    tempEntity.newWaiver(policy2.getId(), application.getId());
    tempEntity.newWaiver(policy3.getId(), application.getId());
    assertThat(telemetryCollector.collectData().getAttributes())
        .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "3");
  }
}
