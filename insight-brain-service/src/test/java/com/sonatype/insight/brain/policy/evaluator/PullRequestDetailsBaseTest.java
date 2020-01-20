/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestDetailsBaseTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig config;

  @Before
  public void before() throws IOException, URISyntaxException {
    config.setBaseUrl("http://localhost:1122");
  }

  @Test
  public void testGetConstraintDetailsForConstraints_AllSameId() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    final ConstraintFact constraintFact2 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-456"),
        getSecuritySeverityConditionFact("CVE-456"));

    final ConstraintFact constraintFact3 = getConstraintFact("1", "Constraint 1",
        getLabelConditionFact("Label Reason"),
        getLicenceConditionFact("Licence Reason"));

    final ConstraintFact constraintFact4 = getConstraintFact("1", "Constraint 1",
        getIdentificationSourceConditionFact("Identification Reason"));

    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat((List<String>) (results.get(0).get("conditions"))).hasSize(4);
  }

  @Test
  public void testGetConstraintDetailsForConstraints_DifferentIds() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    final ConstraintFact constraintFact2 = getConstraintFact("2", "Constraint 2",
        getSecurityStatusConditionFact("CVE-456"),
        getSecuritySeverityConditionFact("CVE-456"));

    final ConstraintFact constraintFact3 = getConstraintFact("1", "Constraint 1",
        getLabelConditionFact("Label Reason"),
        getLicenceConditionFact("Licence Reason"));

    final ConstraintFact constraintFact4 = getConstraintFact("2", "Constraint 2",
        getIdentificationSourceConditionFact("Identification Reason"));

    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat((List<String>) (results.get(0).get("conditions"))).hasSize(3);
    assertThat(results.get(1)).containsKeys("constraintName", "conditions");
    assertThat(results.get(1).get("constraintName")).isEqualTo("Constraint 2");
    assertThat((List<String>) (results.get(1).get("conditions"))).hasSize(2);
  }

  @Test
  public void testGetConstraintDetailsForConstraints_SingleConstraint() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat((List<String>) (results.get(0).get("conditions"))).hasSize(1);
  }

  @Test
  public void testGetConstraintDetailsForConstraints_NoConstraint() {
    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(), config.getBaseUrl());

    //Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintDetailsForConstraints_EmptyConditions() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");

    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat((List<String>) (results.get(0).get("conditions"))).isEmpty();
  }

  @Test
  public void testGetConstraintDetailsForConstraints_NullConditions() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");
    constraintFact1.setConditionFacts(null);

    //When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat((List<String>) (results.get(0).get("conditions"))).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_AllSameId() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    final ConstraintFact constraintFact2 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-456"),
        getSecuritySeverityConditionFact("CVE-456"));

    final ConstraintFact constraintFact3 = getConstraintFact("1", "Constraint 1",
        getLabelConditionFact("Label Reason"),
        getLicenceConditionFact("Licence Reason"));

    final ConstraintFact constraintFact4 = getConstraintFact("1", "Constraint 1",
        getIdentificationSourceConditionFact("Identification Reason"));

    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(4);
    assertThat(results).contains("Identification Reason", "Licence Reason", "Label Reason");
    assertThat(results.get(0)).startsWith("Found security vulnerabilities");
  }

  @Test
  public void testGetConstraintConditionSummaries_DifferentIds() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    final ConstraintFact constraintFact2 = getConstraintFact("2", "Constraint 2",
        getSecurityStatusConditionFact("CVE-456"),
        getSecuritySeverityConditionFact("CVE-456"));

    final ConstraintFact constraintFact3 = getConstraintFact("1", "Constraint 1",
        getLabelConditionFact("Label Reason"),
        getLicenceConditionFact("Licence Reason"));

    final ConstraintFact constraintFact4 = getConstraintFact("2", "Constraint 2",
        getIdentificationSourceConditionFact("Identification Reason"));

    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(4);
    assertThat(results).contains("Identification Reason", "Licence Reason", "Label Reason");
    assertThat(results.get(0)).startsWith("Found security vulnerabilities");
  }

  @Test
  public void testGetConstraintConditionSummaries_SingleConstraint() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).startsWith("Found security vulnerability");
  }

  @Test
  public void testGetConstraintConditionSummaries_NoConstraint() {
    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(), config.getBaseUrl());

    //Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_EmptyConditions() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");

    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_NullConditions() {
    //Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");
    constraintFact1.setConditionFacts(null);

    //When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), config.getBaseUrl());

    //Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusOnly() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(statusConditionFact), config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_SeverityOnly() {
    //Setup
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(severityConditionFact), config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusAndSeverity() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(statusConditionFact, severityConditionFact),
            config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusAndBoth() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact statusConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(
            Lists.newArrayList(statusConditionFact, statusConditionFact2, severityConditionFact),
            config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_SeverityAndBoth() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(
            Lists.newArrayList(statusConditionFact, severityConditionFact, severityConditionFact2),
            config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_BothAndBoth() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact statusConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");

    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists
                .newArrayList(statusConditionFact, statusConditionFact2, severityConditionFact, severityConditionFact2),
            config.getBaseUrl());

    //then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_Empty() {
    //when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(), config.getBaseUrl());

    //then
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetSecurityPrefix_Single() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList(""));

    //then
    assertThat(result).isEqualTo("Found security vulnerability:");
  }

  @Test
  public void testGetSecurityPrefix_Multiple() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList("", ""));

    //then
    assertThat(result).isEqualTo("Found security vulnerabilities:");
  }

  @Test
  public void testGetSecurityPrefix_None() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList());

    //then
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testGetViolationSummariesForNonSecurityConditions_Empty() {
    // when
    final List<String> results =
        PullRequestDetailsBase.getViolationSummariesForNonSecurityConditions(Lists.newArrayList());

    // then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetViolationSummariesForNonSecurityConditions_Single() {
    // setup
    final ConditionFact labelConditionFact = getLabelConditionFact("Label Reason");

    // when
    final List<String> results =
        PullRequestDetailsBase.getViolationSummariesForNonSecurityConditions(Lists.newArrayList(labelConditionFact));

    // then
    assertThat(results).hasSize(1);
    assertThat(results).containsOnly("Label Reason");
  }

  @Test
  public void testGetViolationSummariesForNonSecurityConditions_Multiple() {
    // setup
    final ConditionFact labelConditionFact = getLabelConditionFact("Label Reason");
    final ConditionFact liceConditionFact = getLicenceConditionFact("Licence Reason");

    // when
    final List<String> results = PullRequestDetailsBase
        .getViolationSummariesForNonSecurityConditions(Lists.newArrayList(labelConditionFact, liceConditionFact));

    // then
    assertThat(results).hasSize(2);
    assertThat(results).containsOnly("Label Reason", "Licence Reason");
  }

  @Test
  public void testGetViolationSummariesForNonSecurityConditions_Duplicates() {
    // setup
    final ConditionFact labelConditionFact = getLabelConditionFact("Label Reason");
    final ConditionFact liceConditionFact = getLicenceConditionFact("Licence Reason");

    // when
    final List<String> results = PullRequestDetailsBase
        .getViolationSummariesForNonSecurityConditions(
            Lists.newArrayList(labelConditionFact, liceConditionFact, labelConditionFact));

    // then
    assertThat(results).hasSize(2);
    assertThat(results).containsOnly("Label Reason", "Licence Reason");
  }

  @Test
  public void testApplyCVEUrl_CVE() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isEqualTo("[CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testApplyCVEUrl_SonatypeCapitals() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("SONATYPE-123-123");

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isEqualTo("[SONATYPE-123-123](http://localhost:1122/ui/links/vln/SONATYPE-123-123)");
  }

  @Test
  public void testApplyCVEUrl_SonatypeLower() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("sonatype-123-123");

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isEqualTo("[sonatype-123-123](http://localhost:1122/ui/links/vln/sonatype-123-123)");
  }

  @Test
  public void testApplyCVEUrl_NoMatch() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("invalid-cve");

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isEqualTo("invalid-cve");
  }

  @Test
  public void testApplyCVEUrl_NullReference() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact(null);
    statusConditionFact.setReference(null);

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isNull();
  }

  @Test
  public void testApplyCVEUrl_NullReferenceValue() {
    //Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact(null);

    //when
    final String result = PullRequestDetailsBase.applyCVEUrl(statusConditionFact, config.getBaseUrl());

    //then
    assertThat(result).isNull();
  }

  private ConditionFact getSecurityStatusConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilityStatusConditionType.ID, 0, "Security Status Summary",
        "Security Reason Summary", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact getSecuritySeverityConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
        "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact getLicenceConditionFact(final String reason) {
    return new ConditionFact(LicenseConditionType.ID, 1, "Licence Summary",
        reason, null);
  }

  private ConditionFact getLabelConditionFact(final String reason) {
    return new ConditionFact(LabelConditionType.ID, 1, "Label Summary",
        reason, null);
  }

  private ConditionFact getIdentificationSourceConditionFact(final String reason) {
    return new ConditionFact(IdentificationSourceConditionType.ID, 1, "IdentificationSource Summary",
        reason, null);
  }

  private ConstraintFact getConstraintFact(
      final String constraintId,
      final String constraintName,
      final ConditionFact... conditionFacts)
  {
    return new ConstraintFact(constraintId, constraintName, null, conditionFacts);
  }
}
