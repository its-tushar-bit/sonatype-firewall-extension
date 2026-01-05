/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil.CveData;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConstraintFactsUtil}, specifically focusing on CVSS parsing and CVE data extraction.
 */
public class ConstraintFactsUtilTest
{
  @Test
  public void testParseAttackVectorFromCvssVector_NetworkAttack() {
    String cvssVector = "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Network");
  }

  @Test
  public void testParseAttackVectorFromCvssVector_AdjacentAttack() {
    String cvssVector = "CVSS:3.1/AV:A/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Adjacent");
  }

  @Test
  public void testParseAttackVectorFromCvssVector_LocalAttack() {
    String cvssVector = "CVSS:3.0/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Local");
  }

  @Test
  public void testParseAttackVectorFromCvssVector_PhysicalAttack() {
    String cvssVector = "CVSS:3.0/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Physical");
  }

  @Test
  public void testParseAttackVectorFromCvssVector_CvssV2Format() {
    // CVSS v2 format doesn't have "CVSS:3.0/" prefix
    String cvssVector = "AV:N/AC:L/Au:N/C:P/I:P/A:P";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Network");
  }

  @Test
  public void testParseAttackVectorFromCvssVector_NullInput() {
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(null);
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_EmptyString() {
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector("");
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_NoAttackVector() {
    String cvssVector = "CVSS:3.0/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"; // Missing AV component
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_UnknownCode() {
    String cvssVector = "CVSS:3.0/AV:X/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"; // Invalid code "X"
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("X"); // Should return the unknown code as-is
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_OnlyAV() {
    // Custom CVSS vector with only "AV:" and no value - should handle gracefully
    String cvssVector = "CVSS:3.0/AV:";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_AVWithSpaces() {
    // Custom CVSS vector with spaces around the value
    String cvssVector = "CVSS:3.0/AV: N /AC:L";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Network"); // Should trim and parse correctly
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_CompletelyInvalid() {
    // Completely malformed custom string - should handle gracefully
    String cvssVector = "This is not a CVSS vector at all!";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_SpecialCharacters() {
    // Custom CVSS vector with special characters - should handle gracefully
    String cvssVector = "CVSS:3.0/AV:N#$%/AC:L";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("N#$%"); // Returns the parsed code as-is
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_NoSlashes() {
    // Custom CVSS vector without slashes - should handle gracefully
    String cvssVector = "CustomVectorNoSlashes";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isNull();
  }

  @Test
  public void testParseAttackVectorFromCvssVector_MalformedCustomVector_EmptyComponents() {
    // CVSS vector with empty components between slashes
    String cvssVector = "CVSS:3.0//AV:N//AC:L";
    String attackVector = ConstraintFactsUtil.parseAttackVectorFromCvssVector(cvssVector);
    assertThat(attackVector).isEqualTo("Network"); // Should still find AV:N
  }

  @Test
  public void testExtractCveData_WithFullCveData() throws Exception {
    // Create constraint facts with CVE data
    List<ConstraintFact> constraintFacts = createConstraintFactsWithCve(
        "CVE-2021-44906",
        9.8,
        "ARBITRARY_CODE_EXECUTION",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"
    );

    CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);

    assertThat(cveData).isNotNull();
    assertThat(cveData.cveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(cveData.cvssScore()).isEqualTo(9.8);
    assertThat(cveData.vulnerabilityCategory()).isEqualTo("ARBITRARY_CODE_EXECUTION");
    assertThat(cveData.malwareSeverity()).isNull(); // CVE, not malware
    assertThat(cveData.malwareAttackVector()).isNull(); // CVE, not malware
    assertThat(cveData.cvssAttackVector()).isEqualTo("Network");
    assertThat(cveData.threatTypes()).isNull(); // Not available in constraint facts
    assertThat(cveData.hasCveData()).isTrue();
  }

  @Test
  public void testExtractCveData_WithoutVectorString() throws Exception {
    // CVE data without vectorString (attackVector will be null)
    List<ConstraintFact> constraintFacts = createConstraintFactsWithCve(
        "CVE-2021-44906",
        9.8,
        "ARBITRARY_CODE_EXECUTION",
        null // No vectorString
    );

    CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);

    assertThat(cveData).isNotNull();
    assertThat(cveData.cveNumber()).isEqualTo("CVE-2021-44906");
    assertThat(cveData.cvssScore()).isEqualTo(9.8);
    assertThat(cveData.vulnerabilityCategory()).isEqualTo("ARBITRARY_CODE_EXECUTION");
    assertThat(cveData.malwareSeverity()).isNull();
    assertThat(cveData.malwareAttackVector()).isNull();
    assertThat(cveData.cvssAttackVector()).isNull(); // No vector string provided
    assertThat(cveData.threatTypes()).isNull(); // Not available in constraint facts
    assertThat(cveData.hasCveData()).isTrue();
  }

  @Test
  public void testExtractCveData_NullConstraintFacts() {
    CveData cveData = ConstraintFactsUtil.extractCveData(null);

    assertThat(cveData).isNotNull();
    assertThat(cveData.cveNumber()).isNull();
    assertThat(cveData.cvssScore()).isNull();
    assertThat(cveData.vulnerabilityCategory()).isNull();
    assertThat(cveData.malwareSeverity()).isNull();
    assertThat(cveData.malwareAttackVector()).isNull();
    assertThat(cveData.cvssAttackVector()).isNull();
    assertThat(cveData.threatTypes()).isNull();
    assertThat(cveData.hasCveData()).isFalse();
  }

  @Test
  public void testExtractCveData_EmptyConstraintFacts() {
    List<ConstraintFact> emptyList = new ArrayList<>();
    CveData cveData = ConstraintFactsUtil.extractCveData(emptyList);

    assertThat(cveData).isNotNull();
    assertThat(cveData.hasCveData()).isFalse();
  }

  @Test
  public void testExtractCveData_NoConditionFacts() {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact("cons1", "constraint1", "AND");
    constraintFact.setConditionFacts(null); // No condition facts
    constraintFacts.add(constraintFact);

    CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);

    assertThat(cveData).isNotNull();
    assertThat(cveData.hasCveData()).isFalse();
  }

  @Test
  public void testExtractCveData_InvalidJsonInTrigger() {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact("cons1", "constraint1", "AND");
    List<ConditionFact> conditionFacts = new ArrayList<>();
    ConditionFact conditionFact = new ConditionFact("cond1", 0, "SECURITY", "Invalid JSON");
    conditionFact.setTriggerJson("{ invalid json }"); // Invalid JSON
    conditionFacts.add(conditionFact);
    constraintFact.setConditionFacts(conditionFacts);
    constraintFacts.add(constraintFact);

    CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);

    // Should handle parse exception gracefully
    assertThat(cveData).isNotNull();
    assertThat(cveData.hasCveData()).isFalse();
  }

  @Test
  public void testExtractCveData_PartialCveData() throws Exception {
    // Only CVE number, no other fields
    Map<String, Object> triggerData = new HashMap<>();
    triggerData.put("refId", "CVE-2023-12345");
    // No severity, vulnerabilityCategoryId, or vectorString

    List<ConstraintFact> constraintFacts = createConstraintFactsFromTriggerData(triggerData);

    CveData cveData = ConstraintFactsUtil.extractCveData(constraintFacts);

    assertThat(cveData).isNotNull();
    assertThat(cveData.cveNumber()).isEqualTo("CVE-2023-12345");
    assertThat(cveData.cvssScore()).isNull();
    assertThat(cveData.vulnerabilityCategory()).isNull();
    assertThat(cveData.malwareSeverity()).isNull();
    assertThat(cveData.malwareAttackVector()).isNull();
    assertThat(cveData.cvssAttackVector()).isNull();
    assertThat(cveData.threatTypes()).isNull();
    assertThat(cveData.hasCveData()).isTrue(); // Has CVE number
  }

  // Helper methods

  private List<ConstraintFact> createConstraintFactsWithCve(
      String cveNumber,
      Object cvssScore,
      String vulnerabilityCategory,
      String vectorString) throws Exception
  {
    Map<String, Object> triggerData = new HashMap<>();
    if (cveNumber != null) {
      triggerData.put("refId", cveNumber);
    }
    if (cvssScore != null) {
      triggerData.put("severity", cvssScore);
    }
    if (vulnerabilityCategory != null) {
      triggerData.put("vulnerabilityCategoryId", vulnerabilityCategory);
    }
    if (vectorString != null) {
      triggerData.put("vectorString", vectorString);
    }

    return createConstraintFactsFromTriggerData(triggerData);
  }

  private List<ConstraintFact> createConstraintFactsFromTriggerData(Map<String, Object> triggerData) throws Exception {
    // Create ConditionTrigger with the trigger data using constructor
    ConditionTrigger trigger = new ConditionTrigger(0, triggerData);

    // Serialize to JSON
    String triggerJson = JsonUtils.writeUnformatted(trigger);

    // Create ConditionFact with the trigger JSON
    ConditionFact conditionFact = new ConditionFact("cond1", 0, "SECURITY", "Security violation");
    conditionFact.setTriggerJson(triggerJson);

    // Create ConstraintFact containing the condition
    List<ConditionFact> conditionFacts = new ArrayList<>();
    conditionFacts.add(conditionFact);

    ConstraintFact constraintFact = new ConstraintFact("cons1", "constraint1", "AND");
    constraintFact.setConditionFacts(conditionFacts);

    List<ConstraintFact> constraintFacts = new ArrayList<>();
    constraintFacts.add(constraintFact);

    return constraintFacts;
  }
}
