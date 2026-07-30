/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator.POLICY_WAIVER_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConditionTelemetry;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConstraintTelemetry;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class PolicyWaiverTelemetryCreatorTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private PolicyWaiverTelemetryCreator telemetryCreator;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Test
  public void testSendRepositoryWaiverTelemetry() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation = new ProxyRepositoryPolicyViolation();
    proxyRepositoryPolicyViolation.setThreatLevel(5);
    proxyRepositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    proxyRepositoryPolicyViolation.setActionTypeId("fail");
    proxyRepositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    proxyRepositoryPolicyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    proxyRepositoryPolicyViolation.setTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, proxyRepositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, policyWaiverReason, proxyRepositoryPolicyViolation);
  }

  @Test
  public void testSendRepositoryWaiverTelemetry_withReason() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");

    var policyWaiverReason = policyWaiverReasonDAO.getAll().get(0);
    policyWaiver.setWaiverReasonId(policyWaiverReason.getId());

    final ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation = new ProxyRepositoryPolicyViolation();
    proxyRepositoryPolicyViolation.setThreatLevel(5);
    proxyRepositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    proxyRepositoryPolicyViolation.setActionTypeId("fail");
    proxyRepositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    proxyRepositoryPolicyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    proxyRepositoryPolicyViolation.setTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, proxyRepositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, policyWaiverReason, proxyRepositoryPolicyViolation);
  }

  @Test
  public void testSendRepositoryWaiverTelemetry_TimesNull() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(null);
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(null);
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation = new ProxyRepositoryPolicyViolation();
    proxyRepositoryPolicyViolation.setThreatLevel(5);
    proxyRepositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    proxyRepositoryPolicyViolation.setActionTypeId("fail");
    proxyRepositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    proxyRepositoryPolicyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    proxyRepositoryPolicyViolation.setTime(null);

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, proxyRepositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, policyWaiverReason, proxyRepositoryPolicyViolation);
  }

  @Test
  public void testSendRepositoryWaiverTelemetry_NullComponentIdentifier() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation = new ProxyRepositoryPolicyViolation();
    proxyRepositoryPolicyViolation.setThreatLevel(5);
    proxyRepositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    proxyRepositoryPolicyViolation.setActionTypeId("fail");
    proxyRepositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    proxyRepositoryPolicyViolation.setComponentIdentifier(null);
    proxyRepositoryPolicyViolation.setTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, proxyRepositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, policyWaiverReason, proxyRepositoryPolicyViolation);
  }

  @Test
  public void testSendWaiverTelemetryForOwnerType() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatLevel(5);
    policyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    policyViolation.setActionTypeId("fail");
    policyViolation.setConstraintFacts(createConstraintFacts());
    policyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    policyViolation.setOpenTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryByOwnerType(policyWaiver, policyWaiverReason, OwnerType.APPLICATION, policyViolation);
  }

  @Test
  public void testSendWaiverTelemetryForOwnerType_withReason() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    final var policyWaiverReason = policyWaiverReasonDAO.getAll().get(0);
    policyWaiver.setWaiverReasonId(policyWaiverReason.getId());

    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatLevel(5);
    policyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    policyViolation.setActionTypeId("fail");
    policyViolation.setConstraintFacts(createConstraintFacts());
    policyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    policyViolation.setOpenTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryByOwnerType(policyWaiver, policyWaiverReason, OwnerType.APPLICATION, policyViolation);
  }

  @Test
  public void testSendWaiverTelemetryForOwnerType_TimestampsNull() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(null);
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(null);
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatLevel(5);
    policyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    policyViolation.setActionTypeId("fail");
    policyViolation.setConstraintFacts(createConstraintFacts());
    policyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    policyViolation.setOpenTime(null);

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryByOwnerType(policyWaiver, policyWaiverReason, OwnerType.APPLICATION, policyViolation);
  }

  @Test
  public void testSendWaiverTelemetryForOwnerType_NullComponentIdentifier() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    final PolicyWaiverReason policyWaiverReason = null;

    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatLevel(5);
    policyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    policyViolation.setActionTypeId("fail");
    policyViolation.setConstraintFacts(createConstraintFacts());
    policyViolation
        .setComponentIdentifier(null);
    policyViolation.setOpenTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryForOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryByOwnerType(policyWaiver, policyWaiverReason, OwnerType.APPLICATION, policyViolation);
  }

  private List<ConstraintFact> createConstraintFacts() {
    final List<ConstraintFact> constraintFacts = new ArrayList<>();

    constraintFacts.add(new ConstraintFact("cons1", "constraint 1", "OR"));
    constraintFacts.get(0).setConditionFacts(null);

    constraintFacts.add(new ConstraintFact("cons2", "constraint 2", "AND"));
    constraintFacts.get(1).setConditionFacts(Collections.emptyList());

    constraintFacts.add(new ConstraintFact("cons3", "constraint 3", "AND"));
    final List<ConditionFact> conditionFacts = new ArrayList<>();
    constraintFacts.get(2).setConditionFacts(conditionFacts);
    conditionFacts.add(new ConditionFact("cond", 0, "SUMMARY", "This is the reason"));

    return constraintFacts;
  }

  private void assertTelemetryByOwnerType(
      final PolicyWaiver policyWaiver,
      final PolicyWaiverReason policyWaiverReason,
      final OwnerType ownerType,
      final PolicyViolation policyViolation)
  {
    final PolicyViolationTelemetry policyViolationTelemetry =
        new PolicyViolationTelemetry(policyViolation.getConstraintFacts(), policyViolation.getActionTypeId(),
            policyViolation.getThreatCategory().getName(), policyViolation.getThreatLevel());
    final PolicyWaiverTelemetry policyWaiverTelemetry =
        new PolicyWaiverTelemetry(policyWaiver.getId(),
            ownerType.toString(),
            policyWaiver.getOwnerId(),
            policyViolation.getComponentIdentifier(),
            policyWaiver.getHash(),
            policyViolation.getOpenTime(),
            policyWaiver.getCreateTime(),
            policyWaiver.getExpiryTime(),
            policyViolation.getStageTypeId(),
            policyWaiver.isForContainerImage(),
            policyWaiver.isForContainerImageComponent())
                .withWaiverReason(policyWaiverReason);
    assertTelemetry(policyViolationTelemetry, policyWaiverTelemetry);
  }

  private void assertTelemetryForRepository(
      final PolicyWaiver policyWaiver,
      final PolicyWaiverReason policyWaiverReason,
      final ProxyRepositoryPolicyViolation policyViolation)
  {
    final PolicyViolationTelemetry policyViolationTelemetry =
        new PolicyViolationTelemetry(policyViolation.getConstraintFacts(), policyViolation.getActionTypeId(),
            policyViolation.getThreatCategory().getName(), policyViolation.getThreatLevel());
    final PolicyWaiverTelemetry policyWaiverTelemetry =
        new PolicyWaiverTelemetry(policyWaiver.getId(),
            OwnerType.REPOSITORY.toString(),
            policyWaiver.getOwnerId(),
            policyViolation.getComponentIdentifier(),
            policyWaiver.getHash(),
            policyViolation.getTime(),
            policyWaiver.getCreateTime(),
            policyWaiver.getExpiryTime(),
            StageTypes.PROXY.getId(),
            policyWaiver.isForContainerImage(),
            policyWaiver.isForContainerImageComponent())
                .withWaiverReason(policyWaiverReason);
    assertTelemetry(policyViolationTelemetry, policyWaiverTelemetry);
  }

  private void assertTelemetry(
      final PolicyViolationTelemetry policyViolationTelemetry,
      final PolicyWaiverTelemetry policyWaiverTelemetry)
  {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(POLICY_VIOLATION_TELEMETRY, ImmutableList.of(policyViolationTelemetry));
    expectedAttributes.put(POLICY_WAIVER_TELEMETRY, policyWaiverTelemetry);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_WAIVER);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertAttributes(expectedAttributes, telemetryData.getAttributes());
  }

  private void assertAttributes(
      final Map<String, Object> expectedAttributes,
      final Map<String, Object> actualAttributes)
  {
    assertThat(actualAttributes).containsKey(POLICY_VIOLATION_TELEMETRY);
    assertThat(actualAttributes).containsKey(POLICY_WAIVER_TELEMETRY);

    assertPolicyViolationTelemetry((List<PolicyViolationTelemetry>) expectedAttributes.get(POLICY_VIOLATION_TELEMETRY),
        (List<PolicyViolationTelemetry>) actualAttributes.get(POLICY_VIOLATION_TELEMETRY));

    assertPolicyWaiverTelemetry((PolicyWaiverTelemetry) expectedAttributes.get(POLICY_WAIVER_TELEMETRY),
        (PolicyWaiverTelemetry) actualAttributes.get(POLICY_WAIVER_TELEMETRY));
  }

  private void assertPolicyViolationTelemetry(
      final List<PolicyViolationTelemetry> expected,
      final List<PolicyViolationTelemetry> actual)
  {
    assertThat(actual).isNotNull().hasSize(1);
    assertThat(actual.get(0).getActionTypeId()).isEqualTo(expected.get(0).getActionTypeId());
    assertThat(actual.get(0).getThreatLevel()).isEqualTo(expected.get(0).getThreatLevel());
    assertThat(actual.get(0).getThreatCategory()).isEqualTo(expected.get(0).getThreatCategory());
    assertConstraints(expected.get(0).getConstraints(), actual.get(0).getConstraints());
  }

  private void assertConstraints(final List<ConstraintTelemetry> expected, final List<ConstraintTelemetry> actual) {
    assertThat(actual).isNotNull().hasSize(expected.size());
    for (int i = 0; i < expected.size(); i++) {
      ConstraintTelemetry expectedConstraint = expected.get(0);
      ConstraintTelemetry actualConstraint = actual.get(0);
      assertThat(actualConstraint.getConstraintOperator()).isEqualTo(expectedConstraint.getConstraintOperator());
      assertConditions(expectedConstraint.getConditions(), actualConstraint.getConditions());
    }
  }

  private void assertConditions(final List<ConditionTelemetry> expected, final List<ConditionTelemetry> actual) {
    assertThat(actual).isNotNull().hasSize(expected.size());
    for (int i = 0; i < expected.size(); i++) {
      ConditionTelemetry expectedCondition = expected.get(0);
      ConditionTelemetry actualCondition = actual.get(0);
      assertThat(actualCondition.getConditionSummary()).isEqualTo(expectedCondition.getConditionSummary());
    }
  }

  private void assertPolicyWaiverTelemetry(final PolicyWaiverTelemetry expected, final PolicyWaiverTelemetry actual) {
    assertThat(actual.getWaiverExpiration()).isEqualTo(expected.getWaiverExpiration());
    assertThat(actual.getPolicyWaiverId()).isEqualTo(expected.getPolicyWaiverId());
    assertThat(actual.getWaiverTime()).isEqualTo(expected.getWaiverTime());
    assertThat(actual.getComponentHash()).isEqualTo(expected.getComponentHash());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getComponentFormat()).isEqualTo(expected.getComponentFormat());
    assertThat(actual.getOwnerType()).isEqualTo(expected.getOwnerType());
    assertThat(actual.getViolationTime()).isEqualTo(expected.getViolationTime());
    assertThat(actual.getWaiverReason()).isEqualTo(expected.getWaiverReason());
    assertThat(actual.getStageId()).isEqualTo(expected.getStageId());
  }
}
