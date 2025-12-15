/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConditionTelemetry;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetry.ConstraintTelemetry;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.REPOSITORY_COMPONENT_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class RepositoryComponentTelemetryCreatorTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySenderMock;

  @Inject
  private RepositoryComponentTelemetryCreator telemetryCreator;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_NoViolationsOrReleaseType() {
    final RepositoryComponent repositoryComponent = createComponent();
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.DELETE);

    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
        "repoManId", RepositoryComponentTelemetryEventType.DELETE);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithNotifications() {
    final RepositoryComponent repositoryComponent = createComponent();
    final List<PolicyNotification> policyNotifications =
        ImmutableList.of(createPolicyNotification(), createPolicyNotification());
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.QUARANTINE, policyNotifications);

    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
        "repoManId", RepositoryComponentTelemetryEventType.QUARANTINE, policyNotifications);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithNullNotifications() {
    final RepositoryComponent repositoryComponent = createComponent();
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.QUARANTINE, (List<PolicyNotification>) null);

    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
        "repoManId", RepositoryComponentTelemetryEventType.QUARANTINE, (List<PolicyNotification>) null);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithTelemetryData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);

    telemetryCreator.sendRepositoryComponentTelemetry(telemetryData);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryCaptor.capture());
    assertThat(telemetryCaptor.getValue()).isSameAs(telemetryData);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithNullTelemetryData() {
    telemetryCreator.sendRepositoryComponentTelemetry(null);

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithWrongPurpose() {
    TelemetryData wrongPurposeTelemetry = new TelemetryData(TelemetryPurpose.ADMIN_PASSWORD_CHANGE);

    telemetryCreator.sendRepositoryComponentTelemetry(wrongPurposeTelemetry);

    verifyNoInteractions(telemetrySenderMock);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_WithReleaseType() {
    final RepositoryComponent repositoryComponent = createComponent();
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE, ReleaseQuarantineType.AUTO);

    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
        "repoManId", RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE, ReleaseQuarantineType.AUTO);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_NullComponentIdentifier() {
    final RepositoryComponent repositoryComponent = createComponent();
    repositoryComponent.setComponentIdentifier(null);
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.DELETE);
    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
        "repoManId", RepositoryComponentTelemetryEventType.DELETE);
  }

  @Test
  public void testSendRepositoryComponentTelemetry_NullDates() {
    final RepositoryComponent repositoryComponent = createComponent();
    repositoryComponent.setQuarantineTime(null);
    repositoryComponent.setUnquarantineTimeForMonitoring(null);
    telemetryCreator
        .sendRepositoryComponentTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()),
            "repoManId", RepositoryComponentTelemetryEventType.DELETE);
    assertTelemetry(repositoryComponent, ImmutableList.of(createViolation(), createViolation()), "repoManId",
        RepositoryComponentTelemetryEventType.DELETE);
  }

  private PolicyNotification createPolicyNotification() {
    final Notifications notifications = new Notifications();
    final PolicyNotification policyNotification = new PolicyNotification(null, notifications);
    notifications.add(new JiraNotification());
    notifications.add(new UserNotification());
    notifications.add(new RoleNotification());
    notifications.add(new WebhookNotification());
    return policyNotification;
  }

  private RepositoryPolicyViolation createViolation() {
    final RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation();
    policyViolation.setThreatLevel(5);
    policyViolation.setThreatCategory(PolicyThreatCategory.LICENSE);
    policyViolation.setActionTypeId("fail");
    policyViolation.setConstraintFacts(createConstraintFacts());
    policyViolation
        .setComponentIdentifier(null);
    policyViolation.setTime(new Date());
    policyViolation.setPolicyName("Security-Malicious");
    return policyViolation;
  }

  private RepositoryComponent createComponent() {
    final RepositoryComponent repositoryComponent = new RepositoryComponent();
    repositoryComponent.setRepositoryId("repoid");
    repositoryComponent.setHash("hash");
    repositoryComponent.setComponentIdentifier(new ComponentIdentifier("npm", ImmutableMap.of("packageId", "package")));
    repositoryComponent.setQuarantineTime(new Date());
    repositoryComponent.setUnquarantineTimeForMonitoring(new Date());
    return repositoryComponent;
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

  private void assertTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType)
  {
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream().map(PolicyViolationTelemetry::new).collect(Collectors.toList());
    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(repositoryManagerId, repositoryComponent,
            repositoryComponentTelemetryEventType, null, Collections.emptyList());
    assertThat(repositoryComponentTelemetry.getComponentHash()).isEqualTo(
        HdsClientAnalytics.obfuscate(repositoryComponent.getHash()));
    assertTelemetry(policyViolationTelemetries, repositoryComponentTelemetry);
  }

  private void assertTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications)
  {
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream().map(PolicyViolationTelemetry::new).collect(Collectors.toList());
    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(repositoryManagerId, repositoryComponent,
            repositoryComponentTelemetryEventType, null, policyNotifications);
    assertThat(repositoryComponentTelemetry.getComponentHash()).isEqualTo(
        HdsClientAnalytics.obfuscate(repositoryComponent.getHash()));
    assertTelemetry(policyViolationTelemetries, repositoryComponentTelemetry);
  }

  private void assertTelemetry(
      final RepositoryComponent repositoryComponent,
      final List<RepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType)
  {
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream().map(PolicyViolationTelemetry::new).collect(Collectors.toList());
    final RepositoryComponentTelemetry repositoryComponentTelemetry =
        new RepositoryComponentTelemetry(repositoryManagerId, repositoryComponent,
            repositoryComponentTelemetryEventType, releaseQuarantineType, Collections.emptyList());
    assertThat(repositoryComponentTelemetry.getComponentHash()).isEqualTo(
        HdsClientAnalytics.obfuscate(repositoryComponent.getHash()));
    assertTelemetry(policyViolationTelemetries, repositoryComponentTelemetry);
  }

  private void assertTelemetry(
      final List<PolicyViolationTelemetry> policyViolationTelemetries,
      final RepositoryComponentTelemetry repositoryComponentTelemetry)
  {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(POLICY_VIOLATION_TELEMETRY, policyViolationTelemetries);
    expectedAttributes.put(REPOSITORY_COMPONENT_TELEMETRY, repositoryComponentTelemetry);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_COMPONENT);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertAttributes(expectedAttributes, telemetryData.getAttributes());
  }

  private void assertAttributes(
      final Map<String, Object> expectedAttributes,
      final Map<String, Object> actualAttributes)
  {
    assertThat(actualAttributes).containsKey(POLICY_VIOLATION_TELEMETRY);
    assertThat(actualAttributes).containsKey(REPOSITORY_COMPONENT_TELEMETRY);

    assertPolicyViolationTelemetry((List<PolicyViolationTelemetry>) expectedAttributes.get(POLICY_VIOLATION_TELEMETRY),
        (List<PolicyViolationTelemetry>) actualAttributes.get(POLICY_VIOLATION_TELEMETRY));

    assertRepositoryComponentTelemetry(
        (RepositoryComponentTelemetry) expectedAttributes.get(REPOSITORY_COMPONENT_TELEMETRY),
        (RepositoryComponentTelemetry) actualAttributes.get(REPOSITORY_COMPONENT_TELEMETRY));
  }

  private void assertPolicyViolationTelemetry(
      final List<PolicyViolationTelemetry> expected,
      final List<PolicyViolationTelemetry> actual)
  {
    assertThat(actual).isNotNull().hasSize(expected.size());
    for (int i = 0; i < expected.size(); i++) {
      assertThat(actual.get(i).getActionTypeId()).isEqualTo(expected.get(i).getActionTypeId());
      assertThat(actual.get(i).getThreatLevel()).isEqualTo(expected.get(i).getThreatLevel());
      assertThat(actual.get(i).getThreatCategory()).isEqualTo(expected.get(i).getThreatCategory());
      assertThat(actual.get(i).getPolicyName()).isEqualTo(expected.get(i).getPolicyName());
      assertConstraints(expected.get(i).getConstraints(), actual.get(i).getConstraints());
    }
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

  private void assertRepositoryComponentTelemetry(
      final RepositoryComponentTelemetry expected,
      final RepositoryComponentTelemetry actual)
  {
    assertThat(actual.getComponentFormat()).isEqualTo(expected.getComponentFormat());
    assertThat(actual.getComponentHash()).isEqualTo(expected.getComponentHash());
    assertThat(actual.getRepositoryId()).isEqualTo(expected.getRepositoryId());
    assertThat(actual.getRepositoryManagerId()).isEqualTo(expected.getRepositoryManagerId());
    assertThat(actual.getEventType()).isEqualTo(expected.getEventType());
    assertThat(actual.getQuarantineTime()).isEqualTo(expected.getQuarantineTime());
    assertThat(actual.getReleaseQuarantineType()).isEqualTo(expected.getReleaseQuarantineType());
    assertThat(actual.getReleaseQuarantineTime()).isEqualTo(expected.getReleaseQuarantineTime());
    assertNotifications(expected.getNotifications(), actual.getNotifications());
  }

  private void assertNotifications(final Set<String> expected, final Set<String> actual) {
    assertThat(actual).isNotNull().hasSize(expected.size());
    assertThat(actual).containsAll(expected);
  }
}
