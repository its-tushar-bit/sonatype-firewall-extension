/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.HistoricalTelemetryStateDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.HistoricalTelemetryService.Status;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@Category(SlowTest.class)
public class HistoricalPolicyViolationTelemetryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private HistoricalTelemetryStateDAO historicalTelemetryStateDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private TelemetrySender mockTelemetrySender;

  @Inject
  private HistoricalPolicyViolationTelemetryService testSubject;

  @Test
  public void testCollectAndSendPolicyViolationTelemetry_invalidStatus_h2() {
    testCollectAndSendPolicyViolationTelemetry_invalidStatus();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCollectAndSendPolicyViolationTelemetry_invalidStatus_postgres() {
    testCollectAndSendPolicyViolationTelemetry_invalidStatus();
  }

  private void testCollectAndSendPolicyViolationTelemetry_invalidStatus() {
    // given: a persisted policy violation
    var app = tempEntity.newApplicationWithParent();
    var eval = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "testScanId",
        DateUtils.addDays(new Date(), -2));
    var component = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "", "jar");
    var policy = tempEntity.newPolicy();
    tempEntity.newPolicyViolation(eval, policy, component, "hash", "reason");

    // and given: historical telemetry setup for batches of 2, a violation to push telemetry for and an invalid
    // starting status
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = 0;
    var state = tempEntity.newHistoricalTelemetryState(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION.name(),
        cutoffDate, batchSize, minFreeMemoryMb, Status.IN_PROGRESS.name());
    // Set lastUpdated to a recent time to avoid triggering stale state detection (EI-440)
    state.setLastUpdated(new Date());
    historicalTelemetryStateDAO.update(state);

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: no telemetry processed
    assertThat(count).isZero();

    // when: we update the state to a valid status
    state.setStatus(Status.PENDING.name());
    historicalTelemetryStateDAO.update(state);
    count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: the telemetry was processed (note: we add a terminating record to the telemetry)
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testCollectAndSendPolicyViolationTelemetry_insufficientMemory() {
    // given: a persisted policy violation
    var app = tempEntity.newApplicationWithParent();
    var eval = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "testScanId",
        DateUtils.addDays(new Date(), -2));
    var component = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "", "jar");
    var policy = tempEntity.newPolicy();
    tempEntity.newPolicyViolation(eval, policy, component, "hash", "reason");

    // and given: historical telemetry setup to fail on the memory check
    final var cutoffDate = new Date();
    final var batchSize = 2;
    final var minFreeMemoryMb = Integer.MAX_VALUE;

    var state = tempEntity.newHistoricalTelemetryState(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION.name(),
        cutoffDate, batchSize, minFreeMemoryMb, Status.PENDING.name());

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: processing was skipped
    assertThat(count).isZero();
    state = historicalTelemetryStateDAO.getById(state.getId());
    assertThat(state.getStatus()).isEqualTo(Status.SKIPPED.name());

    // when: reset status and remove the memory trigger
    state.setStatus(Status.PENDING.name());
    state.setMinFreeMemoryMb(0);
    historicalTelemetryStateDAO.update(state);
    count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: the telemetry was processed (note: we have a terminating record in the telemetry)
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testCollectAndSendPolicyViolationTelemetry_h2() {
    testCollectAndSendPolicyViolationTelemetry();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCollectAndSendPolicyViolationTelemetry_postgres() {
    testCollectAndSendPolicyViolationTelemetry();
  }

  private void testCollectAndSendPolicyViolationTelemetry() {
    // given: a persisted security policy violation and a non-security policy violation
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        "testScanId", DateUtils.addDays(new Date(), -2));
    Policy securityPolicy = tempEntity.newPolicy(app.getId(), "TestSecurityPolicy");
    PolicyViolation securityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, securityPolicy);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact("testConstraintId", "testConstraintName", "testOperatorName");
    TriggerReference triggerReference =
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "CVE-2013-7285");
    String triggerJson = "{\"conditionIndex\":1,\"trigger\":{\"refId\":\"CVE-2013-7285\",\"severity\":7}}";
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "testSummary",
        "testReason", triggerReference);
    conditionFact.setTriggerJson(triggerJson);
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    securityPolicyViolation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(securityPolicyViolation);
    Policy nonSecurityPolicy = tempEntity.newPolicy();
    PolicyViolation nonSecurityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, nonSecurityPolicy);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

    // when: we try to collect and send telemetry
    long count = testSubject.collectAndSendPolicyViolationTelemetry();

    // then: two policy violations were processed and the telemetry data has the expected attributes
    assertThat(count).isEqualTo(3);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(3);

    TelemetryData telemetryData = telemetryDataList.get(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION);
    assertThat(telemetryData.getAttributes()).containsEntry("count", 1);
    assertThat(telemetryData.getAttributes()).containsEntry("application_id",
        HdsClientAnalytics.obfuscate(securityPolicyViolation.getOwnerId()));
    assertThat(telemetryData.getAttributes()).containsEntry("real_application_id",
        securityPolicyViolation.getOwnerId());
    assertThat(telemetryData.getAttributes()).containsEntry("component_identifier",
        "maven: {artifactId=Artifact1, groupId=Group1, version=Version1}");
    assertThat(telemetryData.getAttributes()).containsEntry("ecosystem", "maven");
    assertThat(telemetryData.getAttributes()).containsEntry("component_namespace", "Group1");
    assertThat(telemetryData.getAttributes()).containsEntry("component_name", "Artifact1");
    assertThat(telemetryData.getAttributes()).containsEntry("component_version", "Version1");
    assertThat(telemetryData.getAttributes()).containsEntry("open_time",
        securityPolicyViolation.getOpenTime().getTime());
    assertThat(telemetryData.getAttributes()).containsEntry("fix_time", securityPolicyViolation.getFixTime());
    assertThat(telemetryData.getAttributes()).containsEntry("waive_time", securityPolicyViolation.getWaiveTime());
    assertThat(telemetryData.getAttributes()).containsEntry("legacy_violation_time",
        securityPolicyViolation.getLegacyViolationTime());
    assertThat(telemetryData.getAttributes()).containsEntry("policy_name", securityPolicyViolation.getPolicyName());
    assertThat(telemetryData.getAttributes()).containsEntry("policy_violation_id", securityPolicyViolation.getId());
    assertThat(telemetryData.getAttributes()).containsEntry("stage_id", securityPolicyViolation.getStageTypeId());
    assertThat(telemetryData.getAttributes()).containsEntry("threat_category",
        securityPolicyViolation.getThreatCategory().getName());
    assertThat(telemetryData.getAttributes()).containsEntry("threat_level", securityPolicyViolation.getThreatLevel());
    assertThat(telemetryData.getAttributes()).containsEntry("cve_number", "CVE-2013-7285");
    assertThat(telemetryData.getAttributes()).containsEntry("cvss_score", 7);
    assertThat(telemetryData.getAttributes()).doesNotContainEntry("transmission", "complete");
    assertThat(telemetryData.getAttributes()).hasSize(20);

    telemetryData = telemetryDataList.get(1);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION);
    assertThat(telemetryData.getAttributes()).containsEntry("count", 1);
    assertThat(telemetryData.getAttributes()).containsEntry("application_id",
        HdsClientAnalytics.obfuscate(nonSecurityPolicyViolation.getOwnerId()));
    assertThat(telemetryData.getAttributes()).containsEntry("real_application_id",
        nonSecurityPolicyViolation.getOwnerId());
    assertThat(telemetryData.getAttributes()).containsEntry("component_identifier",
        "maven: {artifactId=Artifact1, groupId=Group1, version=Version1}");
    assertThat(telemetryData.getAttributes()).containsEntry("ecosystem", "maven");
    assertThat(telemetryData.getAttributes()).containsEntry("component_namespace", "Group1");
    assertThat(telemetryData.getAttributes()).containsEntry("component_name", "Artifact1");
    assertThat(telemetryData.getAttributes()).containsEntry("component_version", "Version1");
    assertThat(telemetryData.getAttributes()).containsEntry("open_time",
        nonSecurityPolicyViolation.getOpenTime().getTime());
    assertThat(telemetryData.getAttributes()).containsEntry("fix_time", nonSecurityPolicyViolation.getFixTime());
    assertThat(telemetryData.getAttributes()).containsEntry("waive_time", nonSecurityPolicyViolation.getWaiveTime());
    assertThat(telemetryData.getAttributes()).containsEntry("legacy_violation_time",
        nonSecurityPolicyViolation.getLegacyViolationTime());
    assertThat(telemetryData.getAttributes()).containsEntry("policy_name", nonSecurityPolicyViolation.getPolicyName());
    assertThat(telemetryData.getAttributes()).containsEntry("policy_violation_id", nonSecurityPolicyViolation.getId());
    assertThat(telemetryData.getAttributes()).containsEntry("stage_id", nonSecurityPolicyViolation.getStageTypeId());
    assertThat(telemetryData.getAttributes()).containsEntry("threat_category",
        nonSecurityPolicyViolation.getThreatCategory().getName());
    assertThat(telemetryData.getAttributes()).containsEntry("threat_level",
        nonSecurityPolicyViolation.getThreatLevel());
    assertThat(telemetryData.getAttributes()).doesNotContainEntry("transmission", "complete");
    assertThat(telemetryData.getAttributes()).hasSize(18);

    telemetryData = telemetryDataList.get(2);
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.HISTORICAL_POLICY_VIOLATION);
    assertThat(telemetryData.getAttributes()).containsEntry("transmission", "complete");
    assertThat(telemetryData.getAttributes()).hasSize(1);
  }
}
