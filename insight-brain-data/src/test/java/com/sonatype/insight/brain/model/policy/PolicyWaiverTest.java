/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;

public class PolicyWaiverTest
{
  private final String associatedPackagedUrl = "pkg:maven/group/artifact@1.0?classifier=c1&type=jar";

  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiver policyWaiver = new PolicyWaiver(longHash, null /* policyId */, null /* ownerId */, null /* comment */);
    assertThat(policyWaiver.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setHash(longHash);
    assertThat(policyWaiver.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testSetConstraintFactsJson() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(null);

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFactsJson_Empty() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFactsJson(" ");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiver.setConstraintFacts(constraintFacts);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson)
        .doesNotContain("\n", "\r", "\\n",
            "\\r");
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFacts_Null() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(null);

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts_Empty() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setConstraintFacts(Collections.emptyList());

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(constraintFactsJson)
        .doesNotContain("\n", "\r", "\\n",
            "\\r");
    assertConstraintFacts(policyWaiver.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorConstraintFacts_Null() {
    List<ConstraintFact> constraintFacts = null;

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts_Empty() {
    List<ConstraintFact> constraintFacts = Collections.emptyList();

    PolicyWaiver policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiver.getConstraintFactsJson()).isNull();
    assertThat(policyWaiver.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetComponentMatchStrategy_DEFAULT() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setComponentMatchStrategy(DEFAULT);
    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("DEFAULT");
  }

  @Test
  public void testSetComponentMatchStrategy_EXACT_COMPONENT() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setComponentMatchStrategy(EXACT_COMPONENT);
    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("EXACT_COMPONENT");
  }

  @Test
  public void testSetComponentMatchStrategy_ALL_COMPONENTS() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setComponentMatchStrategy(ALL_COMPONENTS);
    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("ALL_COMPONENTS");
  }

  @Test
  public void testSetComponentMatchStrategy_ALL_VERSIONS() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setComponentMatchStrategy(ALL_VERSIONS);
    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("ALL_VERSIONS");
  }

  @Test
  public void testSetComponentMatchStrategy_Null() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setComponentMatchStrategy(null);
    assertThat(policyWaiver.getComponentMatchStrategy()).isNull();
  }

  @Test
  public void testConstructorComponentMatchStrategy_DEFAULT() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("DEFAULT");

    policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiver.getComponentMatchStrategy().toString()).isEqualTo("DEFAULT");
  }

  @Test
  public void testSetAssociatedPackagedUrl() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setAssociatedPackageUrl(associatedPackagedUrl);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);
  }

  @Test
  public void testSetAssociatedPackagedUrl_Null() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setAssociatedPackageUrl(null);
    assertThat(policyWaiver.getAssociatedPackageUrl()).isNull();
  }

  @Test
  public void testSetAssociatedPackagedUrl_Empty() {
    PolicyWaiver policyWaiver = new PolicyWaiver();

    policyWaiver.setAssociatedPackageUrl("");
    assertThat(policyWaiver.getAssociatedPackageUrl()).isEmpty();
  }

  @Test
  public void testConstructorAssociatedPackagedUrl() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiver.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);

    policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiver.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);
  }

  @Test
  public void testGetComponentIdentifier() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    ComponentIdentifier componentIdentifier = policyWaiver.getComponentIdentifier();

    assertThat(componentIdentifier).isInstanceOf(ComponentIdentifier.class);
    assertThat(componentIdentifier.getFormat()).isEqualTo("maven");
    assertThat(componentIdentifier.getCoordinates()).hasSize(5).isEqualTo(new TreeMap<String, String>()
    {
      {
        this.put("artifactId", "artifact");
        this.put("classifier", "c1");
        this.put("extension", "jar");
        this.put("groupId", "group");
        this.put("version", "1.0");
      }
    });

    policyWaiver = new PolicyWaiver("hash", "policyId", "ownerId", null, DEFAULT, "comment");

    assertThat(policyWaiver.getComponentIdentifier()).isNull();
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("some trigger");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    return constraintFacts;
  }

  private void assertConstraintFacts(List<ConstraintFact> actual, List<ConstraintFact> expected) {
    assertThat(actual).hasSameSizeAs(expected);
    for (int constraintFactIndex = 0; constraintFactIndex < expected.size(); constraintFactIndex++) {
      ConstraintFact expectedConstraintFact = expected.get(constraintFactIndex);
      ConstraintFact actualConstraintFact = actual.get(constraintFactIndex);
      assertThat(actualConstraintFact.getConstraintId()).isEqualTo(expectedConstraintFact.getConstraintId());
      assertThat(actualConstraintFact.getConstraintName()).isEqualTo(expectedConstraintFact.getConstraintName());
      assertThat(actualConstraintFact.getOperatorName()).isEqualTo(expectedConstraintFact.getOperatorName());
      for (int conditionFactIndex = 0; conditionFactIndex < expectedConstraintFact.getConditionFacts()
          .size(); conditionFactIndex++)
      {
        ConditionFact expectedConditionFact = expectedConstraintFact.getConditionFacts().get(conditionFactIndex);
        ConditionFact actualConditionFact = actualConstraintFact.getConditionFacts().get(conditionFactIndex);
        assertThat(actualConditionFact.getConditionTypeId()).isEqualTo(expectedConditionFact.getConditionTypeId());
        assertThat(actualConditionFact.getConditionIndex()).isEqualTo(expectedConditionFact.getConditionIndex());
        assertThat(actualConditionFact.getSummary()).isEqualTo(expectedConditionFact.getSummary());
        assertThat(actualConditionFact.getReason()).isEqualTo(expectedConditionFact.getReason());
        assertThat(actualConditionFact.getTriggerJson()).isEqualTo(expectedConditionFact.getTriggerJson());
      }
    }
  }
}
