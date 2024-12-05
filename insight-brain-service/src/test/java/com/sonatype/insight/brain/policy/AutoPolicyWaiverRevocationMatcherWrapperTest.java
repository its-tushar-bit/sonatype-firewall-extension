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
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
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

public class AutoPolicyWaiverRevocationMatcherWrapperTest
    extends AbstractComponentTest
{
  @Test
  public void testMatcherWrapper_MatchesViolation_null_POLICY_VIOLATION() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    assertThatThrownBy(() -> wrapper.matchesViolation(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("policyViolation is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_null_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    assertThatThrownBy(() -> wrapper.matchesViolation(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("policyViolation is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesViolation_null_ALL_VERSIONS() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(
        app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(componentIdentifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(componentIdentifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentIdentifier(componentIdentifierSame);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(revocation
            .getComponentIdentifier(), componentIdentifierSame)).isTrue();
    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(revocation
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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId("fakePolicyId");
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(10);
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(null);
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash("anotherHashValue");
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash("anotherHashValue");
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolationOne.getThreatLevel());
    revocation.setHash(policyViolationOne.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setPolicyId(policy.getId());
    revocation.setThreatLevel(policyViolation.getThreatLevel());
    revocation.setHash(policyViolation.getHash());
    revocation.setComponentIdentifier(identifier);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    assertThat(wrapper.matchesViolation(policyViolation)).isFalse();
  }
}
