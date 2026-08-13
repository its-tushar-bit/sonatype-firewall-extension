/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverExclusionTelemetryCollectorTest
    extends AbstractComponentH2Test
{
  @Inject
  private TelemetryUtils telemetryUtils;

  private AutoPolicyWaiverExclusionTelemetryCollector telemetryCollector;

  private AutoPolicyWaiverExclusion autoWaiverExclusion1;

  private AutoPolicyWaiverExclusion autoWaiverExclusion2;

  private AutoPolicyWaiverExclusion autoWaiverExclusion3;

  private Owner owner1;

  private Owner owner2;

  @BeforeEach
  public void before() {
    telemetryCollector = new AutoPolicyWaiverExclusionTelemetryCollector(telemetryUtils);

    autoWaiverExclusion1 = createAutoPolicyWaiverExclusion("exclusionId1", "ownerId", "autoPolicyWaiverId1");
    autoWaiverExclusion2 = createAutoPolicyWaiverExclusion("exclusionId2", "ownerId", "autoPolicyWaiverId2");
    autoWaiverExclusion3 = createAutoPolicyWaiverExclusion("exclusionId3", "ownerId", "autoPolicyWaiverId3");

    owner1 = new Organization("orgName");
    owner1.setId(UUID.randomUUID().toString());
    owner2 = new Application("publicId", "test-app", owner1.getId());
    owner2.setId(UUID.randomUUID().toString());
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Create() {
    telemetryCollector.addTelemetryForCreateAutoWaiverExclusion(autoWaiverExclusion1, owner1);
    telemetryCollector.addTelemetryForCreateAutoWaiverExclusion(autoWaiverExclusion2, owner2);
    telemetryCollector.addTelemetryForCreateAutoWaiverExclusion(autoWaiverExclusion3, owner1);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion1.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion1.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion1.getAutoPolicyWaiverId());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion2.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion2.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion2.getAutoPolicyWaiverId());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion3.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion3.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion3.getAutoPolicyWaiverId());
            });
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Delete() {
    telemetryCollector.addTelemetryForDeleteAutoWaiverExclusion(autoWaiverExclusion1, owner1);
    telemetryCollector.addTelemetryForDeleteAutoWaiverExclusion(autoWaiverExclusion2, owner1);
    telemetryCollector.addTelemetryForDeleteAutoWaiverExclusion(autoWaiverExclusion3, owner2);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion1.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion1.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion1.getAutoPolicyWaiverId());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion2.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion2.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion2.getAutoPolicyWaiverId());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ID,
                      autoWaiverExclusion3.getId())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_ACTION,
                      AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL,
                      autoWaiverExclusion3.getThreatLevel())
                  .containsEntry(
                      AutoPolicyWaiverExclusionTelemetryCollector.AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
                      autoWaiverExclusion3.getAutoPolicyWaiverId());
            });
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
}
