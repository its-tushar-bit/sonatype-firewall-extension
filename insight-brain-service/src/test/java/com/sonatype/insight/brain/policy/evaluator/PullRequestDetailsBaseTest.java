/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestDetailsBaseTest
    extends AbstractComponentTest
{
  // All SCM features do the markdown CVE URL save for Bitbucket. So we want to always convert URLs for this class.
  static final Boolean CONVERT_URLS = true;

  // The majority of tests will default to use the full security data, not reduced. For readability.
  private static final boolean FULL_DATA = false;

  @Before
  public void before() {
    setBaseUrl("http://localhost:1122");
  }

  @Test
  public void testGetConstraintDetailsForConstraints_AllSameId() {
    // Setup
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

    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), getBaseUrl(),
        CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat(getConditions(results.get(0))).hasSize(4);
  }

  @SuppressWarnings("unchecked")
  private List<String> getConditions(Map<String, Object> result) {
    return (List<String>) result.get("conditions");
  }

  @Test
  public void testGetConstraintDetailsForConstraints_DifferentIds() {
    // Setup
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

    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), getBaseUrl(),
        CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat(getConditions(results.get(0))).hasSize(3);
    assertThat(results.get(1)).containsKeys("constraintName", "conditions");
    assertThat(results.get(1).get("constraintName")).isEqualTo("Constraint 2");
    assertThat(getConditions(results.get(1))).hasSize(2);
  }

  @Test
  public void testGetConstraintDetailsForConstraints_SingleConstraint() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat(getConditions(results.get(0))).hasSize(1);
  }

  @Test
  public void testGetConstraintDetailsForConstraints_NoConstraint() {
    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintDetailsForConstraints_EmptyConditions() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");

    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat(getConditions(results.get(0))).isEmpty();
  }

  @Test
  public void testGetConstraintDetailsForConstraints_NullConditions() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");
    constraintFact1.setConditionFacts(null);

    // When
    final List<Map<String, Object>> results = PullRequestDetailsBase.getConstraintDetailsForConstraints(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("constraintName", "conditions");
    assertThat(results.get(0).get("constraintName")).isEqualTo("Constraint 1");
    assertThat(getConditions(results.get(0))).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_AllSameId() {
    // Setup
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

    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), getBaseUrl(),
        CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).hasSize(4);
    assertThat(results).contains("Identification Reason", "Licence Reason", "Label Reason");
    assertThat(results.get(0)).startsWith("Found 2 security vulnerabilities");
  }

  @Test
  public void testGetConstraintConditionSummaries_DifferentIds() {
    // Setup
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

    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1, constraintFact2, constraintFact3, constraintFact4), getBaseUrl(),
        CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).hasSize(4);
    assertThat(results).contains("Identification Reason", "Licence Reason", "Label Reason");
    assertThat(results.get(0)).startsWith("Found 2 security vulnerabilities");
  }

  @Test
  public void testGetConstraintConditionSummaries_SingleConstraint() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).startsWith("Found 1 security vulnerability");
  }

  @Test
  public void testGetConstraintConditionSummaries_NoConstraint() {
    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(Lists.newArrayList(),
        getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_EmptyConditions() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");

    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetConstraintConditionSummaries_NullConditions() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1");
    constraintFact1.setConditionFacts(null);

    // When
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */,
        null /* unused */);

    // Then
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusOnly() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(statusConditionFact), getBaseUrl(), CONVERT_URLS,
            FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found 1 security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_SeverityOnly() {
    // Setup
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(severityConditionFact), getBaseUrl(), CONVERT_URLS,
            FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found 1 security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusAndSeverity() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists.newArrayList(statusConditionFact, severityConditionFact),
            getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get())
        .isEqualTo("Found 1 security vulnerability: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_StatusAndBoth() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact statusConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(
            Lists.newArrayList(statusConditionFact, statusConditionFact2, severityConditionFact), getBaseUrl(),
            CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found 2 security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_SeverityAndBoth() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(
            Lists.newArrayList(statusConditionFact, severityConditionFact, severityConditionFact2), getBaseUrl(),
            CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found 2 security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_BothAndBoth() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");
    final ConditionFact statusConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");
    final ConditionFact severityConditionFact = getSecuritySeverityConditionFact("CVE-123-123");
    final ConditionFact severityConditionFact2 = getSecurityStatusConditionFact("CVE-456-456");

    // when
    final Optional<String> result = PullRequestDetailsBase
        .getViolationSummaryForSecurityConditions(Lists
            .newArrayList(statusConditionFact, statusConditionFact2, severityConditionFact, severityConditionFact2),
            getBaseUrl(), CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get()).isEqualTo(
        "Found 2 security vulnerabilities: [CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123), " +
            "[CVE-456-456](http://localhost:1122/ui/links/vln/CVE-456-456)");
  }

  @Test
  public void testGetViolationSummaryForSecurityConditions_Empty() {
    // when
    final Optional<String> result =
        PullRequestDetailsBase.getViolationSummaryForSecurityConditions(Lists.newArrayList(), getBaseUrl(),
            CONVERT_URLS, FULL_DATA, null /* unused */, null /* unused */);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetSecurityPrefix_Single() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList(""));

    // then
    assertThat(result).isEqualTo("Found 1 security vulnerability:");
  }

  @Test
  public void testGetSecurityPrefix_Multiple() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList("", ""));

    // then
    assertThat(result).isEqualTo("Found 2 security vulnerabilities:");
  }

  @Test
  public void testGetSecurityPrefix_None() {
    // when
    final String result = PullRequestDetailsBase.getSecurityPrefix(Lists.newArrayList());

    // then
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
  public void testMaybeApplyCVEUrl_CVE() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");

    // when
    final String result = PullRequestDetailsBase
        .maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isEqualTo("[CVE-123-123](http://localhost:1122/ui/links/vln/CVE-123-123)");
  }

  @Test
  public void testMaybeApplyCVEUrl_CVE_DoNotConvert() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("CVE-123-123");

    // when - we pass in false for the URL conversion
    final String result = PullRequestDetailsBase.maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), false);

    // then - URL conversion does not occur
    assertThat(result).isEqualTo("CVE-123-123");
  }

  @Test
  public void testMaybeApplyCVEUrl_SonatypeCapitals() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("SONATYPE-123-123");

    // when
    final String result = PullRequestDetailsBase
        .maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isEqualTo("[SONATYPE-123-123](http://localhost:1122/ui/links/vln/SONATYPE-123-123)");
  }

  @Test
  public void testMaybeApplyCVEUrl_SonatypeLower() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("sonatype-123-123");

    // when
    final String result =
        PullRequestDetailsBase.maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isEqualTo("[sonatype-123-123](http://localhost:1122/ui/links/vln/sonatype-123-123)");
  }

  @Test
  public void testMaybeApplyCVEUrl_NoMatch() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact("invalid-cve");

    // when
    final String result =
        PullRequestDetailsBase.maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isEqualTo("invalid-cve");
  }

  @Test
  public void testMaybeApplyCVEUrl_NullReference() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact(null);
    statusConditionFact.setReference(null);

    // when
    final String result =
        PullRequestDetailsBase.maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isNull();
  }

  @Test
  public void testMaybeApplyCVEUrl_NullReferenceValue() {
    // Setup
    final ConditionFact statusConditionFact = getSecurityStatusConditionFact(null);

    // when
    final String result =
        PullRequestDetailsBase.maybeApplyCVEUrl(statusConditionFact, getBaseUrl(), CONVERT_URLS);

    // then
    assertThat(result).isNull();
  }

  @Test
  public void testGetConstraintConditionSummaries_ReducedSecurityData_SingleCVE() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    // When - pass in true for reduced security data
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1), getBaseUrl(), CONVERT_URLS, true, "public_id", "scan_id");

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).isEqualTo("Found 1 security vulnerability: " +
        "<a href=\"http://localhost:1122/ui/links/application/public_id/report/scan_id\">View Details</a>.");
  }

  @Test
  public void testGetConstraintConditionSummaries_ReducedSecurityData_MultipleCVE() {
    // Setup
    final ConstraintFact constraintFact1 = getConstraintFact("1", "Constraint 1",
        getSecurityStatusConditionFact("CVE-123"),
        getSecuritySeverityConditionFact("CVE-123"));

    final ConstraintFact constraintFact2 = getConstraintFact("2", "Constraint 2",
        getSecurityStatusConditionFact("CVE-456"),
        getSecuritySeverityConditionFact("CVE-456"));

    // When - pass in true for reduced security data
    final List<String> results = PullRequestDetailsBase.getConstraintConditionSummaries(
        Lists.newArrayList(constraintFact1, constraintFact2), getBaseUrl(), CONVERT_URLS, true, "public_id", "scan_id");

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).isEqualTo("Found 2 security vulnerabilities: " +
        "<a href=\"http://localhost:1122/ui/links/application/public_id/report/scan_id\">View Details</a>.");
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
