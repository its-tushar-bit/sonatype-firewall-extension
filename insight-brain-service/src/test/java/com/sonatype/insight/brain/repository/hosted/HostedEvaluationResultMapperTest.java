/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedEvaluationResultMapperTest
{
  private static final String EVAL_URL =
      "https://iq.example.com/assets/index.html#/hostedRepos/r1/components?repositoryPublicId=maven-releases";

  private static final String CORRELATION_ID = "nxrm-upload-abc123";

  private static final String COMPONENT_ID = "iq-comp-xyz";

  private HostedEvaluationResultMapper mapper;

  @Before
  public void setUp() {
    mapper = new HostedEvaluationResultMapper();
  }

  @Test
  public void failPolicy_resultsInBlockedTrue_withBlockingViolations() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(
        alert("Critical Security Policy", 9, Action.ID_FAIL,
            constraint("Critical CVSS", "Critical CVSS")));

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isTrue();
    assertThat(result.policyAction()).isEqualTo("FAIL");
    assertThat(result.highestThreatLevel()).isEqualTo(9);
    assertThat(result.blockingViolations()).hasSize(1);
    HostedBlockingViolation v = result.blockingViolations().get(0);
    assertThat(v.policyName()).isEqualTo("Critical Security Policy");
    assertThat(v.constraintName()).isEqualTo("Critical CVSS");
    assertThat(v.reason()).isEqualTo("Critical CVSS"); // operator name — human-readable
    assertThat(v.componentIdentifier()).isNotBlank();
    assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(result.componentId()).isEqualTo(COMPONENT_ID);
    assertThat(result.evaluationUrl()).isEqualTo(EVAL_URL);
  }

  @Test
  public void warnOnlyPolicy_resultsInBlockedFalse_policyActionWarn_noBlockingViolations() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(
        alert("Advisory Policy", 4, Action.ID_WARN, constraint("cvss-advisory", "CVSS >= 4")));

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isFalse();
    assertThat(result.policyAction()).isEqualTo("WARN");
    assertThat(result.highestThreatLevel()).isEqualTo(4);
    assertThat(result.blockingViolations()).isEmpty();
  }

  @Test
  public void noAlerts_allowedResultWithNullPolicyAction() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(); // empty alerts

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isFalse();
    assertThat(result.policyAction()).isNull();
    assertThat(result.highestThreatLevel()).isZero();
    assertThat(result.blockingViolations()).isEmpty();
    assertThat(result.evaluationUrl()).isEqualTo(EVAL_URL); // URL always populated, per API contract
  }

  @Test
  public void mixedFailAndWarn_blockedTrue_policyActionIsFail() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(
        alert("Advisory Policy", 3, Action.ID_WARN, constraint("advisory", "advisory")),
        alert("Critical Security Policy", 9, Action.ID_FAIL, constraint("critical-cvss", "critical")));

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isTrue();
    assertThat(result.policyAction()).isEqualTo("FAIL");
    assertThat(result.highestThreatLevel()).isEqualTo(9);
    assertThat(result.blockingViolations()).hasSize(1);
    assertThat(result.blockingViolations().get(0).policyName()).isEqualTo("Critical Security Policy");
  }

  @Test
  public void notifyActionAloneDoesNotBlock_andDoesNotSetPolicyAction() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(
        alert("Notification Policy", 2, Action.ID_NOTIFY, constraint("notify-rule", "notify")));

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isFalse();
    assertThat(result.policyAction()).isNull(); // NOTIFY not surfaced
    assertThat(result.highestThreatLevel()).isEqualTo(2);
    assertThat(result.blockingViolations()).isEmpty();
  }

  @Test
  public void nullDataList_treatedAsAllow() {
    HostedEvaluationResult result = mapper.map(null, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isFalse();
    assertThat(result.policyAction()).isNull();
    assertThat(result.highestThreatLevel()).isZero();
    assertThat(result.blockingViolations()).isEmpty();
    assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
  }

  @Test
  public void correlationIdAndComponentId_echoedUnchanged() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts();
    HostedEvaluationResult result = mapper.map(data, EVAL_URL, "cid-123", "iq-456");
    assertThat(result.correlationId()).isEqualTo("cid-123");
    assertThat(result.componentId()).isEqualTo("iq-456");
  }

  @Test
  public void multipleFailViolationsOnSameComponent_allAppearInBlockingViolations() {
    RepositoryComponentEvaluationDataList data = resultWithAlerts(
        alert("Security", 9, Action.ID_FAIL,
            constraint("cvss-critical", "critical"),
            constraint("known-malware", "malware match")));

    HostedEvaluationResult result = mapper.map(data, EVAL_URL, CORRELATION_ID, COMPONENT_ID);

    assertThat(result.blocked()).isTrue();
    assertThat(result.blockingViolations()).hasSize(2);
    assertThat(result.blockingViolations())
        .extracting(HostedBlockingViolation::constraintName)
        .containsExactlyInAnyOrder("cvss-critical", "known-malware");
  }

  // ---- test fixture builders ----

  private static RepositoryComponentEvaluationDataList resultWithAlerts(final PolicyAlert... alerts) {
    RepositoryComponentEvaluationDataList list = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData componentData = new RepositoryComponentEvaluationData();
    componentData.policyAlerts = new ArrayList<>(List.of(alerts));
    list.componentEvalResults.add(componentData);
    return list;
  }

  private static PolicyAlert alert(
      final String policyName,
      final int threatLevel,
      final String actionId,
      final ConstraintFact... constraints)
  {
    PolicyFact trigger = new PolicyFact("policy-id-" + policyName.hashCode(), policyName, threatLevel);
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("groupId", "com.acme");
    coords.put("artifactId", "lib");
    coords.put("version", "1.2.3");
    ComponentFact componentFact = new ComponentFact(
        new ComponentIdentifier("maven", coords),
        "hash-abc");
    componentFact.addPathnames(List.of("com/acme/lib/1.2.3/lib-1.2.3.jar"));
    for (ConstraintFact c : constraints) {
      componentFact.addConstraintFact(c);
    }
    trigger.addComponentFact(componentFact);

    Action action = new Action(actionId);
    return new PolicyAlert(trigger, List.of(action));
  }

  private static ConstraintFact constraint(final String name, final String operator) {
    return new ConstraintFact("constraint-id-" + name.hashCode(), name, operator);
  }
}
