/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.TEST_COMPONENT_IDENTIFIER;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generateConditionFact;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generateConstraintFact;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePV;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePVWithManyConditionFacts;
import static com.sonatype.insight.brain.git.render.ComponentFeedbackHelper.generatePVWithSingleConditionFact;
import static com.sonatype.insight.brain.git.render.ReferenceIdParser.parseReferenceIds;
import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceIdParserTest
{
  private static final String PV_ID = "pv1";

  private static final String[] REF_IDS = {
    "CVE-123-00",
    "SONATYPE-123-01",
    "sonatype-123-02",
    "CVE-123-03",
    "SONATYPE-123-04",
    "sonatype-123-05"
  };

  @Test
  public void testParseReferenceIds_parseMultipleFacts() {
    final PolicyViolation policyViolation = generatePV(
        PV_ID, TEST_COMPONENT_IDENTIFIER,
        generateConstraintFact(
            generateConditionFact(SecurityVulnerabilitySeverityConditionType.ID, REF_IDS[0]),
            generateConditionFact(SecurityVulnerabilityStatusConditionType.ID, REF_IDS[1]),
            generateConditionFact(UUID.randomUUID().toString(), REF_IDS[2])),
        generateConstraintFact(
            generateConditionFact(SecurityVulnerabilitySeverityConditionType.ID, REF_IDS[3]),
            generateConditionFact(SecurityVulnerabilityStatusConditionType.ID, REF_IDS[4]),
            generateConditionFact(UUID.randomUUID().toString(), REF_IDS[5])));
    final Set<String> actualRefIds = parseReferenceIds(policyViolation);
    final Set<String> expectedRefIds = ImmutableSet.of(REF_IDS[0], REF_IDS[1], REF_IDS[3], REF_IDS[4]);
    assertThat(actualRefIds).isEqualTo(expectedRefIds);
  }

  @Test
  public void testParseReferenceIds_parseInvalidVuln() {
    final PolicyViolation policyViolation = generatePVWithManyConditionFacts(
        PV_ID, TEST_COMPONENT_IDENTIFIER, "CVE-234");

    final Set<String> actualRefIds = parseReferenceIds(policyViolation);
    assertThat(actualRefIds).isEmpty();
  }

  @Test
  public void testParseReferenceIds_regex() {
    final PolicyViolation policyViolation =
        generatePVWithSingleConditionFact(PV_ID, TEST_COMPONENT_IDENTIFIER, REF_IDS);
    final Set<String> actualRefIds = parseReferenceIds(policyViolation);
    final Set<String> expectedRefIds = ImmutableSet.copyOf(REF_IDS);
    assertThat(actualRefIds).isEqualTo(expectedRefIds);
  }

  @Test
  public void testParseReferenceIds_missingConditionFacts() {
    final PolicyViolation policyViolation = generatePV(PV_ID, TEST_COMPONENT_IDENTIFIER, generateConstraintFact());
    final Set<String> actualRefIds = parseReferenceIds(policyViolation);
    assertThat(actualRefIds).isEmpty();
  }
}
