/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConditionTelemetry;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConstraintTelemetry;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator.POLICY_WAIVER_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class PolicyWaiverTelemetryCreatorTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private PolicyWaiverTelemetryCreator telemetryCreator;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Test
  public void testSendRepositoryWaiverTelemetry() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");

    final RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setThreatLevel(5);
    repositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    repositoryPolicyViolation.setActionTypeId("fail");
    repositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    repositoryPolicyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    repositoryPolicyViolation.setTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, repositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, repositoryPolicyViolation);
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

    final RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setThreatLevel(5);
    repositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    repositoryPolicyViolation.setActionTypeId("fail");
    repositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    repositoryPolicyViolation
        .setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "value")));
    repositoryPolicyViolation.setTime(null);

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, repositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, repositoryPolicyViolation);
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

    final RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setThreatLevel(5);
    repositoryPolicyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    repositoryPolicyViolation.setActionTypeId("fail");
    repositoryPolicyViolation.setConstraintFacts(createConstraintFacts());
    repositoryPolicyViolation.setComponentIdentifier(null);
    repositoryPolicyViolation.setTime(new Date());

    // when: telemetry is sent
    telemetryCreator.sendRepositoryWaiverTelemetry(policyWaiver, repositoryPolicyViolation);

    // then: expected telemetry entries are sent
    assertTelemetryForRepository(policyWaiver, repositoryPolicyViolation);
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
    assertTelemetryByOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);
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
    assertTelemetryByOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);
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
    assertTelemetryByOwnerType(policyWaiver, OwnerType.APPLICATION, policyViolation);
  }

  @Test
  public void testSendWaiverTelemetryWithoutViolationInformation() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    policyWaiver.setConstraintFacts(createConstraintFacts());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryWithoutViolationInformation(policyWaiver, OwnerType.APPLICATION);

    // then: expected telemetry entries are sent
    assertTelemetryNoViolationInformation(policyWaiver, OwnerType.APPLICATION);
  }

  @Test
  public void testsendWaiverTelemetryWithoutViolationInformation_TimestampsNull() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(null);
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(null);
    policyWaiver.setHash("HASH");
    policyWaiver.setConstraintFacts(createConstraintFacts());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryWithoutViolationInformation(policyWaiver, OwnerType.APPLICATION);

    // then: expected telemetry entries are sent
    assertTelemetryNoViolationInformation(policyWaiver, OwnerType.APPLICATION);
  }

  @Test
  public void testSendWaiverTelemetryWithoutViolationInformation_EmptyConstraintFacts() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    policyWaiver.setConstraintFacts(Lists.emptyList());

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryWithoutViolationInformation(policyWaiver, OwnerType.APPLICATION);

    // then: expected telemetry entries are sent
    assertTelemetryNoViolationInformation(policyWaiver, OwnerType.APPLICATION);
  }

  @Test
  public void testSendWaiverTelemetryWithoutViolationInformation_NullConstraintFacts() {
    // setup
    final PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setId("ID");
    policyWaiver.setCreateTime(new Date());
    policyWaiver.setOwnerId("APP");
    policyWaiver.setExpiryTime(new Date());
    policyWaiver.setHash("HASH");
    policyWaiver.setConstraintFacts(null);

    // when: telemetry is sent
    telemetryCreator.sendWaiverTelemetryWithoutViolationInformation(policyWaiver, OwnerType.APPLICATION);

    // then: expected telemetry entries are sent
    assertTelemetryNoViolationInformation(policyWaiver, OwnerType.APPLICATION);
  }

  private List<ConstraintFact> createConstraintFacts() {
    final List<ConstraintFact> constraintFacts = new ArrayList<>();

    constraintFacts.add(new ConstraintFact("cons1", "constraint 1", "OR"));
    constraintFacts.get(0).setConditionFacts(null);

    constraintFacts.add(new ConstraintFact("cons2", "constraint 2", "AND"));
    constraintFacts.get(1).setConditionFacts(Lists.emptyList());

    constraintFacts.add(new ConstraintFact("cons3", "constraint 3", "AND"));
    final List<ConditionFact> conditionFacts = new ArrayList<>();
    constraintFacts.get(2).setConditionFacts(conditionFacts);
    conditionFacts.add(new ConditionFact("cond", 0, "SUMMARY", "This is the reason"));

    return constraintFacts;
  }

  private void assertTelemetryNoViolationInformation(
      final PolicyWaiver policyWaiver,
      final OwnerType ownerType)
  {
    final PolicyViolationTelemetry policyViolationTelemetry =
        new PolicyViolationTelemetry(policyWaiver.getConstraintFacts(), null, null, null);
    final PolicyWaiverTelemetry policyWaiverTelemetry =
        new PolicyWaiverTelemetry(policyWaiver.getId(), ownerType.toString(), policyWaiver.getOwnerId(), null,
            policyWaiver.getHash(), null, policyWaiver.getCreateTime(), policyWaiver.getExpiryTime(), null);
    assertTelemetry(policyViolationTelemetry, policyWaiverTelemetry);
  }

  private void assertTelemetryByOwnerType(
      final PolicyWaiver policyWaiver,
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
            policyViolation.getStageTypeId());
    assertTelemetry(policyViolationTelemetry, policyWaiverTelemetry);
  }

  private void assertTelemetryForRepository(
      final PolicyWaiver policyWaiver,
      final RepositoryPolicyViolation policyViolation)
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
            StageTypes.PROXY.getId());
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
    assertThat(actual.getStageId()).isEqualTo(expected.getStageId());
  }
}
