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
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverExclusionMatcherWrapperTest
    extends AbstractComponentH2Test
{
  private ListAppender<ILoggingEvent> listAppender;

  private Level originalLogLevel;

  @BeforeEach
  public void setupLogging() {
    Logger log = (Logger) LoggerFactory.getLogger(AutoPolicyWaiverExclusionMatcherWrapper.class);
    originalLogLevel = log.getLevel();
    listAppender = new ListAppender<>();
    listAppender.start();
    log.addAppender(listAppender);
    log.setLevel(Level.DEBUG);
  }

  @AfterEach
  public void tearDownLogging() {
    Logger log = (Logger) LoggerFactory.getLogger(AutoPolicyWaiverExclusionMatcherWrapper.class);
    log.detachAppender(listAppender);
    log.setLevel(originalLogLevel);
  }

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
    {
      {
        this.put("name", "name");
        this.put("extension", "e");
        this.put("qualifier", "q");
      }
    });
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
    {
      {
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
      }
    });
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
        {
          {
            this.put("artifactId", "artifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }
        });

    ComponentIdentifier componentIdentifierOther =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
        {
          {
            this.put("artifactId", "otherArtifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }
        });

    Application app = tempEntity.newApplicationWithParent();

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setComponentIdentifier(componentIdentifierSame);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(exclusion
            .getComponentIdentifier(), componentIdentifierSame))
        .isTrue();
    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(exclusion
            .getComponentIdentifier(), componentIdentifierOther))
        .isFalse();
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
    exclusion.setHash("someHash");
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

  @Test
  public void testDebugLog_policyIdNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    policyViolation.setPolicyId(null);

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .contains("Policy violation match failed: policy ID is null");
  }

  @Test
  public void testDebugLog_policyIdMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId("differentPolicyId");
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .contains("Policy violation match failed: policy IDs do not match")
        .contains("Exclusion policy ID:")
        .contains("violation policy ID:");
  }

  @Test
  public void testDebugLog_threatLevelNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(null);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("exclusion threat level is null"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Violation threat level:");
  }

  @Test
  public void testDebugLog_threatLevelMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(999);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("threat levels do not match"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion threat level:")
        .contains("violation threat level:");
  }

  @Test
  public void testDebugLog_hashNull() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, null, "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash("someHash");
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Hash is null"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion hash:")
        .contains("Violation hash:");
  }

  @Test
  public void testDebugLog_hashMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash("differentHash");
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Hashes do not match"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion hash:")
        .contains("Violation hash:");
  }

  @Test
  public void testDebugLog_componentIdentifierMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    ComponentIdentifier identifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(eval, policy, identifier1, "fakeHash", "fake");

    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    exclusion.setPolicyId(policy.getId());
    exclusion.setThreatLevel(policyViolation.getThreatLevel());
    exclusion.setHash(policyViolation.getHash());
    exclusion.setComponentIdentifier(identifier2);
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Component identifiers do not match"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion component identifier:")
        .contains("Violation component identifier:");
  }

  @Test
  public void testDebugLog_constraintFactsNull() {
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

    wrapper.matchesViolation(policyViolation);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Constraint facts are null"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion constraint facts:")
        .contains("Violation constraint facts:");
  }

  @Test
  public void testDebugLog_constraintFactsMismatch() {
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

    // Set constraint facts for exclusion to ensure we get past the null check
    ConstraintFact exclusionFact = new ConstraintFact("exclusionFact", "exclusionOperator", "exclusionValue");
    exclusion.setConstraintFacts(List.of(exclusionFact));
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    PolicyViolation policyViolationTwo = tempEntity.newPolicyViolation(eval, policy, identifier, "fakeHash", "fake");
    ConstraintFact newFact = new ConstraintFact("fakeFact", "fakeOperator", "fakeValue");
    List<ConstraintFact> newFacts = List.of(newFact, newFact);
    policyViolationTwo.setConstraintFacts(newFacts);
    policyViolationTwo.setConstraintFactsId("fakeId");

    wrapper.matchesViolation(policyViolationTwo);

    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Constraint facts do not match"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("Exclusion constraint facts:")
        .contains("Violation constraint facts:");
  }

  @Test
  public void testDebugLog_successfulMatch() {
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
    exclusion.setConstraintFacts(policyViolation.getConstraintFacts());
    exclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    AutoPolicyWaiverExclusionMatcherWrapper wrapper = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);

    boolean result = wrapper.matchesViolation(policyViolation);

    assertThat(result).isTrue();
    assertThat(listAppender.list.size()).isGreaterThan(0);
    ILoggingEvent logEvent = listAppender.list.stream()
        .filter(event -> event.getFormattedMessage().contains("Policy violation matched successfully"))
        .findFirst()
        .orElse(null);
    assertThat(logEvent).isNotNull();
    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logEvent.getFormattedMessage())
        .contains("policy ID")
        .contains("component identifier:");
  }
}
