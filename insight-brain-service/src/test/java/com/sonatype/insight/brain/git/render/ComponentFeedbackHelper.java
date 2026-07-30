/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.utils.RandomGenerator;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityCustomData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilitySeverity;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import com.google.common.collect.ImmutableList;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;

public final class ComponentFeedbackHelper
{
  public static final ComponentIdentifier TEST_COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates(
      "my-group-id",
      "my-artifact-id",
      "1.0.0");

  private static final List<String> VALID_VULN_CONDITION_TYPE_IDS = ImmutableList.of(
      SecurityVulnerabilitySeverityConditionType.ID,
      SecurityVulnerabilityStatusConditionType.ID);

  private static final RandomGenerator RANDOM_GENERATOR = new RandomGenerator(ComponentFeedbackHelper.class);

  private static final int MAX_THREAT_LEVEL = 10;

  private static final int MIN_THREAT_LEVEL = 1;

  private ComponentFeedbackHelper() {
  }

  public static String generateRandomValidVulnConditionTypeId() {
    return RANDOM_GENERATOR.randomElement(VALID_VULN_CONDITION_TYPE_IDS);
  }

  public static SecurityVulnerabilityData generateSecurityVulnerabilityData(
      final String refId,
      final String description,
      final Float mainCvssScore,
      final SecurityVulnerabilityResearchType researchType)
  {
    return generateSecurityVulnerabilityData(refId, description, mainCvssScore, null, researchType);
  }

  public static SecurityVulnerabilityData generateSecurityVulnerabilityData(
      final String refId,
      final String description,
      final Float mainCvssScore,
      final Float customCvssScore,
      final SecurityVulnerabilityResearchType researchType)
  {
    final SecurityVulnerabilityData securityVulnerabilityData = new SecurityVulnerabilityData(refId);
    securityVulnerabilityData.mainSeverity = new SecurityVulnerabilitySeverity();
    securityVulnerabilityData.mainSeverity.score = isNull(mainCvssScore) ? 0f : mainCvssScore;
    securityVulnerabilityData.customData = new SecurityVulnerabilityCustomData();
    securityVulnerabilityData.customData.cvssSeverity = customCvssScore;
    securityVulnerabilityData.description = description;
    securityVulnerabilityData.researchType = researchType;
    return securityVulnerabilityData;
  }

  public static PolicyViolation generatePVWithManyConditionFacts(
      final String id,
      final ComponentIdentifier componentIdentifier,
      final String... refIds)
  {
    return generatePVWithManyConditionFacts(id, componentIdentifier, -1, refIds);
  }

  public static PolicyViolation generatePVWithManyConditionFacts(
      final String id,
      final ComponentIdentifier componentIdentifier,
      final int threatLevel,
      final String... refIds)
  {
    final ConditionFact[] conditionFacts = Arrays.stream(refIds)
        .map(refId -> generateConditionFact(generateRandomValidVulnConditionTypeId(), refId))
        .toArray(ConditionFact[]::new);
    return generatePV(id, componentIdentifier, threatLevel, generateConstraintFact(conditionFacts));
  }

  public static PolicyViolation generatePVWithSingleConditionFact(
      final String id,
      final ComponentIdentifier componentIdentifier,
      final String... refIds)
  {
    final String formattedValue = "startText" + IntStream.range(0, refIds.length)
        .boxed()
        .map(x -> "%s")
        .collect(Collectors.joining("sometext")) + "endText";
    final String value = String.format(formattedValue, (Object[]) refIds);
    return generatePV(id, componentIdentifier,
        generateConstraintFact(
            generateConditionFact(generateRandomValidVulnConditionTypeId(), value)));
  }

  public static PolicyViolation generatePV(
      final String id,
      final ComponentIdentifier componentIdentifier,
      final ConstraintFact... constraintFacts)
  {
    return generatePV(id, componentIdentifier, -1, constraintFacts);
  }

  public static PolicyViolation generatePV(
      final String id,
      final ComponentIdentifier componentIdentifier,
      final int threatLevel,
      final ConstraintFact... constraintFacts)
  {
    final PolicyViolation pv = new PolicyViolation();
    pv.setOwnerId(UUID.randomUUID().toString());
    pv.setComponentIdentifier(componentIdentifier);
    pv.setThreatLevel(threatLevel <= 0
        ? RANDOM_GENERATOR.randomInt(MIN_THREAT_LEVEL, MAX_THREAT_LEVEL + 1)
        : threatLevel);
    pv.setId(id);
    if (constraintFacts.length > 0) {
      pv.setConstraintFacts(copyOf(constraintFacts));
    }
    return pv;
  }

  public static ConstraintFact generateConstraintFact(final ConditionFact... conditionFacts) {
    final ConstraintFact cf = new ConstraintFact();
    if (conditionFacts.length > 0) {
      cf.setConditionFacts(ImmutableList.copyOf(conditionFacts));
    }
    return cf;
  }

  public static ConditionFact generateConditionFact(final String conditionTypeId, final String value) {
    final TriggerReference tr = new TriggerReference();
    final ConditionFact cf = new ConditionFact();
    cf.setReference(tr);
    cf.setConditionTypeId(conditionTypeId);
    tr.setValue(value);
    return cf;
  }
}
