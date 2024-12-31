/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AutoPolicyWaiverExclusionMatcherWrapperTest
    extends AbstractComponentTest
{
  @Test
  public void testMatcherWrapper_MatchesViolation_null_POLICY_VIOLATION() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThatThrownBy(() -> wrapper.matchesViolation(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("policyViolation is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_null_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThatThrownBy(() -> wrapper.matchesViolation(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("policyViolation is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_null_ALL_VERSIONS() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThatThrownBy(() -> wrapper.matchesViolation(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("policyViolation is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHashValue", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(
        app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_EXACT_COMPONENT_nullHash() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, null, "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_EXACT_COMPONENT_nullHash_missingRequiredCoordinates() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_PYPI, new TreeMap<String, String>()
    {{
        this.put("name", "name");
        this.put("extension", "e");
        this.put("qualifier", "q");
      }});
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(eval, policy, componentIdentifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(componentIdentifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    policyViolation.setHash(null);
    policyViolation.setComponentIdentifier(componentIdentifier);
    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_ALL_VERSIONS() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "3.5", "", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "otherHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_ALL_VERSIONS_UnknownComponent() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "3.5", "", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "otherHash", "fake");
    policyViolation.setComponentIdentifier(null);

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_ALL_VERSIONS_missingRequiredCoordinates() {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {{
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
      }});
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "otherHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThatNoException().isThrownBy(() -> wrapper.matchesViolation(policyViolation));
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_ALL_VERSIONS_caseMisMatch() {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier =
        ComponentIdentifier.createPypiCoordinates("py-component", "otherVersion", "py3-none-any", "whl");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "otherHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_ALL_VERSIONS_PythonPackageWithDot() {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("ruamel.yaml", "otherVersion", "py3-none-any", "whl");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(eval, policy, componentIdentifier, "otherHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(componentIdentifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isTrue();
  }

  @Test
  public void testMatcherWrapper_CompareWhenMissingRequiredCoordinates() {
    ComponentIdentifier componentIdentifierSame =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap()
        {{
            this.put("artifactId", "artifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    ComponentIdentifier componentIdentifierOther =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
        {{
            this.put("artifactId", "otherArtifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    Application app = tempEntity.newApplicationWithParent();

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(componentIdentifierSame);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(exclusion
            .getComponentIdentifier(), componentIdentifierSame)).isTrue();
    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(exclusion
            .getComponentIdentifier(), componentIdentifierOther)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_policyIdMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId("fakePolicyId");
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_policyIdNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    policyViolation.setPolicyId(null);

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_threatLevelMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(10);
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_threatLevelNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(null);
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_hashMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash("anotherHashValue");
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_hashNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, null, "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash("anotherHashValue");
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_constraintFactsIdNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_constraintFactsMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolationOne = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolationOne.getThreatLevel());
    exclusion.setHash(policyViolationOne.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    PolicyViolation policyViolationTwo = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    ConstraintFact newFact = new ConstraintFact("fakeFact", "fakeOperator", "fakeValue");
    List<ConstraintFact> newFacts = List.of(newFact, newFact);
    policyViolationTwo.setConstraintFacts(newFacts);
    policyViolationTwo.setConstraintFactsId("fakeId");
    assertThat(wrapper.matchesViolation(policyViolationTwo)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesViolation_POLICY_VIOLATION_invalidConstraintFactsId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }
}
