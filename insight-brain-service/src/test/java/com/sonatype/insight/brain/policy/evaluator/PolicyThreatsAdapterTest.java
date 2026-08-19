/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.security.CurrentUser;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class PolicyThreatsAdapterTest
{
  @Test
  public void testCreatePolicyThreats() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        false, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);
    Map<String, Owner> policyIdPolicyOwnerIdMap = new HashMap<>();
    Application app = new Application("ROOT_ORGANIZATION_ID", "ROOT_ORGANIZATION", "ROOT_ORGANIZATION_ID");
    policyIdPolicyOwnerIdMap.put("policy1", app);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, "compliance",
        policyIdPolicyOwnerIdMap);
    assertThat(threats.stageTypeId).isEqualTo("compliance");
    validatePolicyValidationOwner(threats.aaData, app);
    assertPolicyThreats(threats, violations);
  }

  private void validatePolicyValidationOwner(List<PolicyThreats.Component> components, Owner owner) {
    String ownerId = owner.getId();
    String ownerType = owner.getType().toString();
    components.stream()
        .flatMap(component -> component.allViolations.stream())
        .forEach(policyViolation -> {
          assertThat(policyViolation.policyOwnerId).isEqualTo(ownerId);
          assertThat(policyViolation.policyOwnerType).isEqualTo(ownerType);
        });
  }

  @Test
  public void testCreatePolicyThreats_LargestPolicyThreat() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation10 = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation mavenViolation1 = buildPolicyViolation("policy2", "hash1", 1, mavenIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation10 = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation1 = buildPolicyViolation("policy2", "hash2", 1, nugetIdentifier, false,
        false, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation10, mavenViolation1, nugetViolation10,
        nugetViolation1);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    PolicyViolation largestMavenPolicyViolation = getLargestThreatViolation("hash1", violations);
    PolicyViolation largestNuGetPolicyViolation = getLargestThreatViolation("hash2", violations);

    // Make sure we have 2 components.
    assertThat(threats.aaData).hasSize(2);

    // Make sure the largest policy violations are set on the component.
    for (PolicyThreats.Component component : threats.aaData) {
      // hash1 == maven component.
      if (component.hash.equals("hash1")) {
        assertThat(component.policyId).isEqualTo(largestMavenPolicyViolation.getPolicyId());
        assertThat(component.policyName).isEqualTo(largestMavenPolicyViolation.getPolicyName());
        assertThat(component.policyThreatLevel).isEqualTo(largestMavenPolicyViolation.getThreatLevel());
      }
      else {
        assertThat(component.policyId).isEqualTo(largestNuGetPolicyViolation.getPolicyId());
        assertThat(component.policyName).isEqualTo(largestNuGetPolicyViolation.getPolicyName());
        assertThat(component.policyThreatLevel).isEqualTo(largestNuGetPolicyViolation.getThreatLevel());
      }
    }
  }

  @Test
  public void testCreatePolicyThreats_Waived() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, true,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, true,
        false, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    // Make sure each component has a waived policy.
    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.waivedViolations).hasSize(1);
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_WaivedWithAutoWaiver() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE", "", "jar");
    ComponentIdentifier cargoIdentifier = ComponentIdentifier.createCargoCoordinates("fauxpak", "0.7.0", "contrived");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, true,
        false, Action.ID_FAIL);

    PolicyViolation cargoViolation =
        buildPolicyViolation("policy2", "hash2", 10, cargoIdentifier, true, false, Action.ID_FAIL);
    cargoViolation.setAutoPolicyWaiverId("autoPolicyWaiverId1");

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, cargoViolation);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    // Make sure each component has a waived policy.
    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.waivedViolations).hasSize(1);
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_LegacyViolation() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        true, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        true, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.allViolations).hasSize(1);
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_ConstraintFactsJson() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false, true,
        Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false, true,
        Action.ID_FAIL);

    mavenViolation.setConstraintFacts(Collections.singletonList(new ConstraintFact("id1", "name1", "op1")));
    nugetViolation.setConstraintFacts(Collections.singletonList(new ConstraintFact("id2", "name2", "op2")));

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.allViolations).hasSize(1);
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_AllWaivedButTopViolationExists() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, true,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, true,
        false, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, null);

    // Make sure each component has a 'top violation' even though all violations are waived.
    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.waivedViolations).hasSize(1);
      assertThat(component.policyId).isNull();
      assertThat(component.policyName).isEqualTo("None");
      assertThat(component.policyThreatLevel).isEqualTo(0);
    }

    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreateProxyPolicyThreats_Actions() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        false, Action.ID_FAIL);
    mavenViolation.setStageTypeId("proxy");

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation);
    Map policyIdPolicyOwnerMap = new HashMap<String, Owner>();
    Application app = new Application("ROOT_ORGANIZATION_ID", "ROOT_ORGANIZATION", "ROOT_ORGANIZATION_ID");
    policyIdPolicyOwnerMap.put("policy1", app);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, policyIdPolicyOwnerMap);

    // Make sure we have two components.
    assertThat(threats.aaData).hasSize(1);

    // Each component has a fail action
    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.activeViolations).hasSize(1);
      assertThat(component.activeViolations.get(0).actions).hasSize(1);
      List<PolicyThreats.PolicyAction> actions = component.activeViolations.get(0).actions;
      assertThat(actions.get(0).actionType).isEqualTo(Action.ID_FAIL);
      assertThat(actions.get(0).actionSummary).isEqualTo("Proxy Failed");
    }
    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_Actions() {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("p", "v");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, false,
        false, Action.ID_FAIL);

    List<PolicyViolation> violations = Lists.newArrayList(mavenViolation, nugetViolation);
    Map policyIdPolicyOwnerMap = new HashMap<String, Owner>();
    Application app = new Application("ROOT_ORGANIZATION_ID", "ROOT_ORGANIZATION", "ROOT_ORGANIZATION_ID");
    policyIdPolicyOwnerMap.put("policy1", app);

    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(violations, null, policyIdPolicyOwnerMap);

    // Make sure we have two components.
    assertThat(threats.aaData).hasSize(2);

    // Each component has a fail action
    for (PolicyThreats.Component component : threats.aaData) {
      assertThat(component.activeViolations).hasSize(1);
      assertThat(component.activeViolations.get(0).actions).hasSize(1);
      List<PolicyThreats.PolicyAction> actions = component.activeViolations.get(0).actions;
      assertThat(actions.get(0).actionType).isEqualTo(Action.ID_FAIL);
      assertThat(actions.get(0).actionSummary).isEqualTo(ActionTypes.getById(Action.ID_FAIL).getSummary("stageId1"));
    }
    assertPolicyThreats(threats, violations);
  }

  @Test
  public void testCreatePolicyThreats_NullPolicyViolations() {
    PolicyThreats threats = PolicyThreatsAdapter.createPolicyThreats(null, null, null);

    assertThat(threats.aaData).isEmpty();
    assertThat(threats.version).isEqualTo(5);
  }

  @Test
  public void testAutoWaiverEval() {
    ComponentIdentifier cargoIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1.SONATYPE", "", "jar");
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient-core", "4.2.8", "", "jar");
    ComponentIdentifier nugetIdentifier = ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0");

    PolicyViolation mavenViolation = buildPolicyViolation("policy1", "hash1", 10, mavenIdentifier, false,
        false, Action.ID_FAIL);
    PolicyViolation nugetViolation = buildPolicyViolation("policy1", "hash2", 10, nugetIdentifier, true,
        false, Action.ID_FAIL);
    PolicyViolation cargoViolation = buildPolicyViolation("policy1", "hash3", 10, cargoIdentifier, true,
        true, Action.ID_FAIL);
    cargoViolation.setAutoPolicyWaiverId("autoPolicyWaiverId1");
    PolicyViolation cargoViolationTwo = buildPolicyViolation("policy1", "hash3", 10, cargoIdentifier, true,
        false, Action.ID_FAIL);

    boolean result = PolicyThreatsAdapter.isAutoWaived(cargoViolation);
    assertThat(result).isTrue();

    boolean resultTwo = PolicyThreatsAdapter.isAutoWaived(mavenViolation);
    assertThat(resultTwo).isFalse();

    boolean resultThree = PolicyThreatsAdapter.isAutoWaived(nugetViolation);
    assertThat(resultThree).isFalse();

    boolean resultFour = PolicyThreatsAdapter.isAutoWaived(cargoViolationTwo);
    assertThat(resultFour).isFalse();
  }

  private PolicyViolation buildPolicyViolation(
      String policyId,
      String hash,
      int threatLevel,
      ComponentIdentifier componentIdentifier,
      boolean waived,
      boolean legacyViolation,
      String actionType)
  {
    PolicyEvaluation evaluation =
        new PolicyEvaluation("applicationId1", "stageId1", "scanId1", CurrentUser.SYSTEM, ScanTriggerType.CLI);
    evaluation.setTime(new Date());

    PolicyViolation violation = new PolicyViolation(evaluation, policyId, policyId, threatLevel,
        PolicyThreatCategory.OTHER, hash, componentIdentifier, buildConstraintFact(policyId), null);
    if (waived) {
      violation.setWaiveTime(violation.getOpenTime());
    }
    if (legacyViolation) {
      violation.setLegacyViolationTime(new Date());
    }
    violation.setActionTypeId(actionType);

    return violation;
  }

  private List<ConstraintFact> buildConstraintFact(String policyId) {
    MatchStateConditionType matchStateConditionType = new MatchStateConditionType();
    ConstraintFact fact = new ConstraintFact("constraint-" + policyId, "constraint-" + policyId, "test-operator");

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vun-refId");

    ConditionFact condition = new ConditionFact(matchStateConditionType.getId(), 0 /* conditionIndex */,
        "Match state condition.", "Unknown match state.", triggerReference);
    fact.addConditionFact(condition);

    return Lists.newArrayList(fact);
  }

  private void assertPolicyThreats(PolicyThreats threats, List<PolicyViolation> violations) {
    for (PolicyThreats.Component component : threats.aaData) {
      assertPolicyThreatsComponent(component, violations);
      assertPolicyThreatsPolicyViolations(component.activeViolations, violations);
      assertPolicyThreatsPolicyViolations(component.waivedViolations, violations);
      assertPolicyThreatsPolicyViolations(component.allViolations, violations);
    }
  }

  private void assertPolicyThreatsComponent(PolicyThreats.Component component, List<PolicyViolation> violations) {
    PolicyViolation violation = getLargestThreatViolation(component.hash, violations);
    if (violation != null) {
      assertPolicyThreatsComponent(component, violation);
      return;
    }
    fail("Unable to find matching violation for policy threats component " + component + ".");
  }

  private PolicyViolation getLargestThreatViolation(String hash, List<PolicyViolation> violations) {
    PolicyViolation result = null;
    for (PolicyViolation violation : violations) {
      if (violation.getHash().equals(hash)) {
        if (result == null) {
          result = violation;
        }
        else if (violation.getThreatLevel() > result.getThreatLevel()) {
          result = violation;
        }
      }
    }

    return result;
  }

  private void assertPolicyThreatsComponent(PolicyThreats.Component component, PolicyViolation violation) {
    assertThat(component.hash).isEqualTo(violation.getHash());
    assertThat(component.componentIdentifier).isEqualTo(violation.getComponentIdentifier());
    if (violation.isActive()) {
      assertThat(component.policyId).isEqualTo(violation.getPolicyId());
      assertThat(component.policyName).isEqualTo(violation.getPolicyName());
      assertThat(component.policyThreatLevel).isEqualTo(violation.getThreatLevel());
    }
  }

  private void assertPolicyThreatsPolicyViolations(
      List<PolicyThreats.PolicyViolation> policyViolations,
      List<PolicyViolation> violations)
  {
    for (PolicyThreats.PolicyViolation policyViolation : policyViolations) {
      assertPolicyThreatsPolicyViolations(policyViolation, violations);
    }
  }

  private void assertPolicyThreatsPolicyViolations(
      PolicyThreats.PolicyViolation policyViolation,
      List<PolicyViolation> violations)
  {
    for (PolicyViolation violation : violations) {
      if (policyViolation.policyId.equals(violation.getPolicyId())
          && policyViolation.constraintFactsJson.equals(violation.getConstraintFactsJson()))
      {
        assertPolicyThreatsPolicyViolations(policyViolation, violation);
        return;
      }
    }

    fail("Unable to find matching violation for policy threats violation " + policyViolation.toString() + ".");
  }

  private void assertPolicyThreatsPolicyViolations(
      PolicyThreats.PolicyViolation policyViolation,
      PolicyViolation violation)
  {
    assertThat(policyViolation.policyId).isEqualTo(violation.getPolicyId());
    assertThat(policyViolation.policyViolationId).isEqualTo(violation.getId());
    assertThat(policyViolation.policyName).isEqualTo(violation.getPolicyName());
    assertThat(policyViolation.waived).isEqualTo(violation.isWaived());
    assertThat(policyViolation.waivedWithAutoWaiver).isEqualTo(
        violation.isWaived() && violation.getAutoPolicyWaiverId() != null);
    assertThat(policyViolation.legacyViolation).isEqualTo(violation.isLegacyViolation());
    assertThat(policyViolation.constraintFactsJson).isEqualTo(violation.getConstraintFactsJson());
    assertThat(policyViolation.policyThreatCategory).isEqualTo(violation.getThreatCategory().toString());

    for (PolicyThreats.PolicyAction action : policyViolation.actions) {
      assertThat(action.actionType).isEqualTo(violation.getActionTypeId());
      assertThat(action.actionSummary).isEqualTo(
          ActionTypes.getById(violation.getActionTypeId()).getSummary(violation.getStageTypeId()));
    }

    assertPolicyThreatsPolicyConstraints(policyViolation.constraints, violation.getConstraintFacts());
  }

  private void assertPolicyThreatsPolicyConstraints(
      List<PolicyThreats.PolicyConstraint> policyConstraints,
      List<ConstraintFact> facts)
  {
    for (PolicyThreats.PolicyConstraint policyConstraint : policyConstraints) {
      assertPolicyThreatsPolicyConstraints(policyConstraint, facts);
    }
  }

  private void assertPolicyThreatsPolicyConstraints(
      PolicyThreats.PolicyConstraint constraint,
      List<ConstraintFact> facts)
  {
    for (ConstraintFact fact : facts) {
      if (constraint.constraintId.equals(fact.getConstraintId())) {
        assertPolicyThreatsPolicyConstraints(constraint, fact);
        return;
      }
    }

    fail("Unable to find matching policy threats constraint fact " + constraint.toString() + ".");
  }

  private void assertPolicyThreatsPolicyConstraints(PolicyThreats.PolicyConstraint constraint, ConstraintFact fact) {
    assertThat(constraint.constraintId).isEqualTo(fact.getConstraintId());
    assertThat(constraint.constraintName).isEqualTo(fact.getConstraintName());
    assertThat(constraint.constraintOperator).isEqualTo(fact.getOperatorName());

    assertPolicyThreatsPolicyConditions(constraint.conditions, fact.getConditionFacts());
  }

  private void assertPolicyThreatsPolicyConditions(
      List<PolicyThreats.PolicyCondition> conditions,
      List<ConditionFact> conditionFacts)
  {
    for (PolicyThreats.PolicyCondition condition : conditions) {
      assertPolicyThreatsPolicyConditions(condition, conditionFacts);
    }
  }

  private void assertPolicyThreatsPolicyConditions(
      PolicyThreats.PolicyCondition condition,
      List<ConditionFact> conditionFacts)
  {
    for (ConditionFact fact : conditionFacts) {
      assertPolicyConditionTriggerReference(condition.conditionTriggerReference, fact.getReference());

      if (fact.getConditionTypeId().equals(condition.conditionType)
          && fact.getReason().equals(condition.conditionReason)
          && fact.getSummary().equals(condition.conditionSummary))
      {
        return;
      }
    }

    fail("Unable to find matching policy threats condition fact " + condition.toString() + ".");
  }

  private void assertPolicyConditionTriggerReference(
      PolicyThreats.PolicyConditionTriggerReference policyThreatsTriggerReference,
      TriggerReference conditionFactTriggerReference)
  {
    if (conditionFactTriggerReference == null) {
      assertThat(policyThreatsTriggerReference).isNull();
    }
    else {
      assertThat(policyThreatsTriggerReference.type).isEqualTo(conditionFactTriggerReference.getType());
      assertThat(policyThreatsTriggerReference.value).isEqualTo(conditionFactTriggerReference.getValue());
    }
  }
}
