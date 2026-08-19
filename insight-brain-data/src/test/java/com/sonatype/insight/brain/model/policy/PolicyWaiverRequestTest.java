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
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverRequestTest
{
  private final String associatedPackagedUrl = "pkg:maven/group/artifact@1.0?classifier=c1&type=jar";

  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest(longHash, null /* policyId */, null /* ownerId */, null /* comment */);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();
    policyWaiverRequest.setHash(longHash);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testSetConstraintFactsJson() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiverRequest.setConstraintFactsJson(constraintFactsJson);
    assertThat(policyWaiverRequest.getConstraintFactsJson()).isEqualTo(constraintFactsJson);
    assertConstraintFacts(policyWaiverRequest.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFactsJson_Null() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setConstraintFactsJson(null);

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFactsJson_Empty() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setConstraintFactsJson(" ");

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    policyWaiverRequest.setConstraintFacts(constraintFacts);
    assertThat(policyWaiverRequest.getConstraintFactsJson()).isEqualTo(constraintFactsJson)
        .doesNotContain("\n", "\r",
            "\\n",
            "\\r");
    assertConstraintFacts(policyWaiverRequest.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testSetConstraintFacts_Null() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setConstraintFacts(null);

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetConstraintFacts_Empty() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setConstraintFacts(Collections.emptyList());

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts() {
    List<ConstraintFact> constraintFacts = createConstraintFacts(2);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);

    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isEqualTo(constraintFactsJson)
        .doesNotContain("\n", "\r",
            "\\n",
            "\\r");
    assertConstraintFacts(policyWaiverRequest.getConstraintFacts(), constraintFacts);
  }

  @Test
  public void testConstructorConstraintFacts_Null() {
    List<ConstraintFact> constraintFacts = null;

    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testConstructorConstraintFacts_Empty() {
    List<ConstraintFact> constraintFacts = Collections.emptyList();

    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", constraintFacts, "comment");

    assertThat(policyWaiverRequest.getConstraintFactsJson()).isNull();
    assertThat(policyWaiverRequest.getConstraintFacts()).isNull();
  }

  @Test
  public void testSetComponentMatchStrategy_DEFAULT() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setComponentMatchStrategy(DEFAULT);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.DEFAULT);
  }

  @Test
  public void testSetComponentMatchStrategy_EXACT_COMPONENT() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setComponentMatchStrategy(EXACT_COMPONENT);
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
  }

  @Test
  public void testSetComponentMatchStrategy_ALL_COMPONENTS() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setComponentMatchStrategy(ALL_COMPONENTS);
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
  }

  @Test
  public void testSetComponentMatchStrategy_ALL_VERSIONS() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setComponentMatchStrategy(ALL_VERSIONS);
    assertThat(policyWaiverRequest.getComponentMatchStrategy())
        .isEqualTo(ComponentMatcherStrategyForWaiver.ALL_VERSIONS);
  }

  @Test
  public void testSetComponentMatchStrategy_Null() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setComponentMatchStrategy(null);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isNull();
  }

  @Test
  public void testConstructorComponentMatchStrategy_DEFAULT() {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.DEFAULT);

    policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForWaiver.DEFAULT);
  }

  @Test
  public void testSetAssociatedPackagedUrl() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setAssociatedPackageUrl(associatedPackagedUrl);
    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);
  }

  @Test
  public void testSetAssociatedPackagedUrl_Null() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setAssociatedPackageUrl(null);
    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isNull();
  }

  @Test
  public void testSetAssociatedPackagedUrl_Empty() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest();

    policyWaiverRequest.setAssociatedPackageUrl("");
    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEmpty();
  }

  @Test
  public void testConstructorAssociatedPackagedUrl() {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);

    policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", associatedPackagedUrl, DEFAULT, "comment");

    assertThat(policyWaiverRequest.getAssociatedPackageUrl()).isEqualTo(associatedPackagedUrl);
  }

  @Test
  public void testGetComponentIdentifier() {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("hash", "policyId", "ownerId", null, associatedPackagedUrl, DEFAULT, "comment");

    ComponentIdentifier componentIdentifier = policyWaiverRequest.getComponentIdentifier();

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

    policyWaiverRequest = new PolicyWaiverRequest("hash", "policyId", "ownerId", null, DEFAULT, "comment");

    assertThat(policyWaiverRequest.getComponentIdentifier()).isNull();
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
