/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PolicyStatusOverrideTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyStatusOverrideTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose(), is(TelemetryPurpose.POLICY_STATUS_OVERRIDE));
  }

  @Test
  public void testCollectData_SecurityVulnerabilityOverrideCounts() throws Exception {
    assertThat(
        telemetryCollector.collectData().getAttributes()
            .get(PolicyStatusOverrideTelemetryCollector.SECURITY_VULNERABILITY_OVERRIDE_COUNT),
        is("0"));

    tempEntity.newSecurityVulnerabilityOverride("ownerId1", "hash1", "source1", "referenceId1",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    tempEntity.newSecurityVulnerabilityOverride("ownerId2", "hash2", "source2", "referenceId2",
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE);
    tempEntity.newSecurityVulnerabilityOverride("ownerId3", "hash3", "source3", "referenceId3",
        SecurityVulnerabilityOverrideStatus.CONFIRMED);

    assertThat(
        telemetryCollector.collectData().getAttributes()
            .get(PolicyStatusOverrideTelemetryCollector.SECURITY_VULNERABILITY_OVERRIDE_COUNT),
        is("3"));
  }
}
