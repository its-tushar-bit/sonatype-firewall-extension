/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;

/**
 * @since 1.140
 */
public class PolicyWaiverMatcherWrapperTest
    extends AbstractPolicyEvaluationTest
{
  private final String notNullComponentFactErrorMessage = "componentFact is required but got null instead";

  private final String associatedPackagedUrl = "pkg:maven/group/artifact@*?classifier=c1&type=jar";

  @Test
  public void testMatcherWrapper_PolicyWaiverMatcherWrapperConstructor() {
    List<ConstraintFact> constraintFacts = Collections.emptyList();
    PolicyWaiver policyWaiver = new PolicyWaiverBuilder().setHash("hash").setPolicyId("policyId").setOwnerId("ownerId")
        .setConstraintFacts(constraintFacts).setComment("comment").build();

    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);
    assertThat(policyWaiverMatcherWrapper.policyWaiver).isEqualTo(policyWaiver);
  }

  @Test
  public void testMatcherWrapper_MatchesPolicyId_null() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setPolicyId("policyId").build());

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId(null)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesPolicyId_empty() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setPolicyId("policyId").build());

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId("")).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesPolicyId() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setPolicyId("policyId").build());

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId("policyId")).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_DEFAUTLT() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setHash("hash").setComponentMatchStrategy(DEFAULT).build());

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_EXACT_COMPONENT() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setHash("hash").setComponentMatchStrategy(EXACT_COMPONENT).build());

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_ALL_COMPONENTS() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setHash("hash").setComponentMatchStrategy(ALL_COMPONENTS).build());

    assertThatNoException().isThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null));
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_ALL_VERSIONS() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiverBuilder().setHash("hash").setComponentMatchStrategy(EXACT_COMPONENT).build());

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_DEFAULT() {
    String hash = "hash";
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(DEFAULT)
            .setAssociatedPackagedUrl(associatedPackagedUrl).build();
    ComponentFact componentFact = new ComponentFact(policyWaiver.getComponentIdentifier(), hash);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT() {
    String hash = "hash";
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackagedUrl(associatedPackagedUrl).build();
    ComponentFact componentFact = new ComponentFact(policyWaiver.getComponentIdentifier(), hash);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_COMPONENTS() {
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(null).setComponentMatchStrategy(ALL_COMPONENTS)
            .setAssociatedPackagedUrl(associatedPackagedUrl).build();
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(null)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS() {
    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackagedUrl(associatedPackagedUrlAllVersions).build();

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>() {{
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
        this.put("extension", "jar");
      }});
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_UnknownComponent() {
    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackagedUrl(associatedPackagedUrlAllVersions).build();

    ComponentFact componentFact = new ComponentFact(null, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_missingRequiredCoordinates() {
    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackagedUrl(associatedPackagedUrlAllVersions).build();

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>() {{
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
      }});
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThatNoException().isThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(componentFact));
  }

  @Test
  public void testMatcherWrapper_CompareWhenMissingRequiredCoordinates() {
    ComponentIdentifier componentIdentifierSame =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>() {{
            this.put("artifactId", "artifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    ComponentIdentifier componentIdentifierOther =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>() {{
            this.put("artifactId", "otherArtifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar&classifier=";

    PolicyWaiver policyWaiver =
        new PolicyWaiverBuilder().setHash(hash).setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackagedUrl(associatedPackagedUrlAllVersions).build();

    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper
        .compareWhenMissingRequiredCoordinates(policyWaiver
            .getComponentIdentifier(), componentIdentifierSame)).isTrue();
    assertThat(policyWaiverMatcherWrapper
        .compareWhenMissingRequiredCoordinates(policyWaiver
            .getComponentIdentifier(), componentIdentifierOther)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesConstraintFactsJson() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(new PolicyWaiverBuilder().build());

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFactsJson("")).isFalse();

    PolicyWaiver policyWaiver = new PolicyWaiverBuilder().setConstraintFacts(1).build();
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);
    String constraintFactsJson = policyWaiver.getConstraintFactsJson();

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesConstraintFacts() {
    PolicyWaiver policyWaiver = new PolicyWaiverBuilder().setConstraintFacts(3).build();
    List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFacts(constraintFacts)).isTrue();

    policyWaiver = new PolicyWaiverBuilder().setConstraintFacts(1).build();
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFacts(constraintFacts)).isFalse();
  }

  @Test
  public void testMatcherWrapper_IsLegacyWaiver() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(new PolicyWaiverBuilder().build());

    assertThat(policyWaiverMatcherWrapper.isLegacyWaiver()).isTrue();

    PolicyWaiver policyWaiver = new PolicyWaiverBuilder().setConstraintFacts(1).build();
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.isLegacyWaiver()).isFalse();
  }

  private static class PolicyWaiverBuilder
  {
    private final PolicyWaiver policyWaiver;

    PolicyWaiverBuilder() {
      this.policyWaiver = new PolicyWaiver();
    }

    public PolicyWaiverBuilder setHash(final String hash) {
      policyWaiver.setHash(hash);
      return this;
    }

    public PolicyWaiverBuilder setPolicyId(final String policyId) {
      policyWaiver.setPolicyId(policyId);
      return this;
    }

    public PolicyWaiverBuilder setOwnerId(final String ownerId) {
      policyWaiver.setOwnerId(ownerId);
      return this;
    }

    public PolicyWaiverBuilder setConstraintFacts(final List<ConstraintFact> constraintFacts) {
      policyWaiver.setConstraintFacts(constraintFacts);
      return this;
    }

    private PolicyWaiverBuilder setConstraintFacts(int count) {
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
        ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
            0 /* conditionIndex */, "some summary", "some reason");
        conditionFact.setTriggerJson("{ \"conditionIndex\": " + i + ", \"trigger\": \"some trigger\" }");
        constraintFact.addConditionFact(conditionFact);
        constraintFacts.add(constraintFact);
      }
      policyWaiver.setConstraintFacts(constraintFacts);
      return this;
    }

    public PolicyWaiverBuilder setComment(final String comment) {
      policyWaiver.setComment(comment);
      return this;
    }

    public PolicyWaiverBuilder setComponentMatchStrategy(final ComponentMatcherStrategyForWaiver matchStrategy) {
      this.policyWaiver.setComponentMatchStrategy(matchStrategy);
      return this;
    }

    public PolicyWaiverBuilder setAssociatedPackagedUrl(final String associatedPackagedUrl) {
      this.policyWaiver.setAssociatedPackageUrl(associatedPackagedUrl);
      return this;
    }

    public PolicyWaiver build() {
      return this.policyWaiver;
    }
  }
}
