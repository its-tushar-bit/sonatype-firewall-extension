/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import jakarta.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverTelemetryCollectorTest extends AbstractComponentTest
{
  @Inject
  private TelemetryUtils telemetryUtils;

  private AutoPolicyWaiverTelemetryCollector telemetryCollector;

  private AutoPolicyWaiver autoPolicyWaiver1;

  private AutoPolicyWaiver autoPolicyWaiver2;

  private AutoPolicyWaiver autoPolicyWaiver3;

  private PolicyViolation policyViolation1;

  private PolicyViolation policyViolation2;

  private PolicyViolation policyViolation3;

  private Owner owner1;

  private Owner owner2;

  @Before
  public void before() {
    telemetryCollector = new AutoPolicyWaiverTelemetryCollector(telemetryUtils);

    autoPolicyWaiver1 = createAutoPolicyWaiver("autoPolicyWaiver1", "owner1", 1);
    autoPolicyWaiver2 = createAutoPolicyWaiver("autoPolicyWaiver2", "owner2", 2);
    autoPolicyWaiver3 = createAutoPolicyWaiver("autoPolicyWaiver3", "owner3", 3);

    policyViolation1 = createPolicyViolation("policyViolation1", "autoPolicyWaiver1", "application1");
    policyViolation2 = createPolicyViolation("policyViolation2", "autoPolicyWaiver2", "application2");
    policyViolation3 = createPolicyViolation("policyViolation3", "autoPolicyWaiver3", "application3");

    owner1 = new Organization("orgName");
    owner1.setId(UUID.randomUUID().toString());
    owner2 = new Application("publicId", "test-app", owner1.getId());
    owner2.setId(UUID.randomUUID().toString());
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Apply() {
    telemetryCollector.addTelemetryForApplyAutoWaiver(autoPolicyWaiver1, policyViolation1, owner1);
    telemetryCollector.addTelemetryForApplyAutoWaiver(autoPolicyWaiver2, policyViolation2, owner1);
    telemetryCollector.addTelemetryForApplyAutoWaiver(autoPolicyWaiver3, policyViolation3, owner2);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver1.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.APPLY.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver1.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver1.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver1.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID,
                      policyViolation1.getHash());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.APPLY.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver2.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver2.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver2.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID,
                      policyViolation2.getHash());
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver3.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.APPLY.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver3.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver3.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver3.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID,
                      policyViolation3.getHash());
            }
        );
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Create() {
    telemetryCollector.addTelemetryForCreateAutoWaiver(autoPolicyWaiver1, owner1);
    telemetryCollector.addTelemetryForCreateAutoWaiver(autoPolicyWaiver2, owner2);
    telemetryCollector.addTelemetryForCreateAutoWaiver(autoPolicyWaiver3, owner1);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver1.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver1.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver1.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver1.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver2.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver2.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver2.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver3.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.CREATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver3.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver3.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver3.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            }
        );
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Update() {
    telemetryCollector.addTelemetryForUpdateAutoWaiver(autoPolicyWaiver1, owner2);
    telemetryCollector.addTelemetryForUpdateAutoWaiver(autoPolicyWaiver2, owner2);
    telemetryCollector.addTelemetryForUpdateAutoWaiver(autoPolicyWaiver3, owner1);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver1.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.UPDATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver1.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver1.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver1.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.UPDATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver2.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver2.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver2.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver3.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.UPDATE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver3.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver3.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver3.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            }
        );
  }

  @Test
  public void testGetTelemetryData_ReturnCollectedTelemetryData_Delete() {
    telemetryCollector.addTelemetryForDeleteAutoWaiver(autoPolicyWaiver1, owner2);
    telemetryCollector.addTelemetryForDeleteAutoWaiver(autoPolicyWaiver2, owner1);
    telemetryCollector.addTelemetryForDeleteAutoWaiver(autoPolicyWaiver3, owner1);

    assertThat(telemetryCollector.getTelemetryData())
        .hasSize(3)
        .satisfiesExactlyInAnyOrder(
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(TelemetryUtils.REAL_APPLICATION_ID, owner2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner2.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver1.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver1.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver1.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver1.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver2.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver2.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver2.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver2.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            },
            telemetryData -> {
              final Map<String, Object> attributes = telemetryData.getAttributes();
              assertThat(attributes)
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.ORGANIZATION_ID,
                      HdsClientAnalytics.obfuscate(owner1.getId()))
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.OWNER_TYPE, owner1.getType())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ID, autoPolicyWaiver3.getId())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_ACTION,
                      AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.DELETE.name())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_REACHABILITY,
                      autoPolicyWaiver3.hasReachability())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_PATH_FORWARD,
                      autoPolicyWaiver3.hasPathForward())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_THREAT_LEVEL,
                      autoPolicyWaiver3.getThreatLevel())
                  .containsEntry(AutoPolicyWaiverTelemetryCollector.AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, null);
            }
        );
  }

  private AutoPolicyWaiver createAutoPolicyWaiver(
      final String autoPolicyWaiverId,
      final String ownerId,
      final int threatLevel)
  {
    final AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setId(autoPolicyWaiverId);
    autoPolicyWaiver.setOwnerId(ownerId);
    autoPolicyWaiver.setThreatLevel(threatLevel);
    autoPolicyWaiver.setCreatorId("creatorId");
    autoPolicyWaiver.setCreateTime(new Date());
    autoPolicyWaiver.setPathForward(true);
    autoPolicyWaiver.setReachability(true);

    return autoPolicyWaiver;
  }

  private PolicyViolation createPolicyViolation(
      final String policyViolationId,
      final String autoPolicyWaiverId,
      final String applicationId)
  {
    final PolicyViolation policyViolation = new PolicyViolation();
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
    policyViolation.setHash(UUID.randomUUID().toString());

    return policyViolation;
  }
}
