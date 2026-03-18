/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.140
 */
public class PolicyWaiverMatcherWrapperTest
    extends AbstractPolicyEvaluationTest
{
  private final String notNullComponentFactErrorMessage = "componentFact is required but got null instead";

  private final String associatedPackagedUrl = "pkg:maven/group/artifact@2.0?classifier=c1&type=jar";

  @Rule
  public LogOutput logOutput = new LogOutput(1, PolicyWaiverMatcherWrapperTest.class);

  @Test
  public void testMatcherWrapper_MatchesPolicyId_null() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setPolicyId("policyId"));

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId(null)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesPolicyId_empty() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setPolicyId("policyId"));

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId("")).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesPolicyId() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setPolicyId("policyId"));

    assertThat(policyWaiverMatcherWrapper.matchesPolicyId("policyId")).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_DEFAUTLT() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setHash("hash").setComponentMatchStrategy(DEFAULT));

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_EXACT_COMPONENT() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setHash("hash").setComponentMatchStrategy(EXACT_COMPONENT));

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_ALL_COMPONENTS() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setHash("hash").setComponentMatchStrategy(ALL_COMPONENTS));

    assertThatNoException().isThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null));
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_ALL_VERSIONS() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setHash("hash").setComponentMatchStrategy(ALL_VERSIONS));

    assertThatThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(null)).hasMessage(
        notNullComponentFactErrorMessage).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_DEFAULT() {
    String hash = "hash";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setHash(hash)
            .setComponentMatchStrategy(DEFAULT)
            .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiver.getComponentIdentifier(), hash);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT() {
    String hash = "hash";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setHash(hash)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiver.getComponentIdentifier(), hash);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT_nullHash() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiver.getComponentIdentifier(), null);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT_nullHash_missingRequiredCoordinates() {
    String associatedPackagedUrl = "pkg:pypi/name?extension=e&qualifier=q";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackageUrl(associatedPackagedUrl);

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_PYPI, new TreeMap<String, String>()
    {
      {
        this.put("name", "name");
        this.put("extension", "e");
        this.put("qualifier", "q");
      }
    });

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_COMPONENTS() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setHash(null)
            .setComponentMatchStrategy(ALL_COMPONENTS)
            .setAssociatedPackageUrl(associatedPackagedUrl);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(null)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS() {
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", "jar");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_UnknownComponent() {
    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setHash(hash)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentFact componentFact = new ComponentFact(null, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_missingRequiredCoordinates() {
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {
      {
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
      }
    });
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThatNoException().isThrownBy(() -> policyWaiverMatcherWrapper.matchesComponent(componentFact));
  }

  /**
   * This case miss match only happen with pypi components
   */
  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_caseMissMatch() {
    String associatedPackagedUrlAllVersions = "pkg:pypi/py-component@1.0?extension=whl&qualifier=py3-none-any";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("Py-component", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_PythonPackageWithDot() {
    String associatedPackagedUrlAllVersions = "pkg:pypi/ruamel.yaml@0.17.35?extension=whl&qualifier=py3-none-any";
    PolicyWaiver policyWaiver = new PolicyWaiver().setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("ruamel.yaml", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_CompareWhenMissingRequiredCoordinates() {
    // compareWhenMissingRequiredCoordinates method expects the coordinates already have the version as a wildcard (*)
    ComponentIdentifier componentIdentifierSame =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
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

    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar&classifier=";

    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(ALL_VERSIONS)
            .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

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
        new PolicyWaiverMatcherWrapper(new PolicyWaiver());

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFactsJson("")).isFalse();

    PolicyWaiver policyWaiver = new PolicyWaiver().setConstraintFacts(createConstraintFacts(1));
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);
    String constraintFactsJson = policyWaiver.getConstraintFactsJson();

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesConstraintFacts() {
    PolicyWaiver policyWaiver = new PolicyWaiver().setConstraintFacts(createConstraintFacts(3));
    List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFacts(constraintFacts)).isTrue();

    policyWaiver = new PolicyWaiver().setConstraintFacts(createConstraintFacts(1));
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesConstraintFacts(constraintFacts)).isFalse();
  }

  @Test
  public void testMatcherWrapper_IsLegacyWaiver() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(new PolicyWaiver());

    assertThat(policyWaiverMatcherWrapper.isLegacyWaiver()).isTrue();

    PolicyWaiver policyWaiver = new PolicyWaiver().setConstraintFacts(createConstraintFacts(1));
    policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.isLegacyWaiver()).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesComponentOrAnyVersionOfComponent_ALL_COMPONENTS() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver().setComponentMatchStrategy(ALL_COMPONENTS));
    assertThat(policyWaiverMatcherWrapper.matchesComponentOrAnyVersionOfComponent(new ComponentFact()))
        .isTrue();
  }

  @Test
  public void testMatcherWrapper_matchesComponentOrAnyVersionOfComponent_EXACT_COMPONENT() {
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper =
        new PolicyWaiverMatcherWrapper(
            new PolicyWaiver()
                .setHash("My hash")
                .setComponentMatchStrategy(EXACT_COMPONENT));
    assertThat(policyWaiverMatcherWrapper.matchesComponentOrAnyVersionOfComponent(new ComponentFact(null, "My hash")))
        .isTrue();
  }

  @Test
  public void testMatcherWrapper_matchesComponentOrAnyVersionOfComponent_matchesAllVersionsOfComponent() {
    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setAssociatedPackageUrl(associatedPackagedUrl);
    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);
    assertThat(policyWaiverMatcherWrapper.matchesComponentOrAnyVersionOfComponent(
        new ComponentFact(policyWaiver.getComponentIdentifier(), null)))
            .isTrue();
  }

  @Test
  public void testMatcherWrapper_matchesComponent_EXACT_COMPONENT_differentFormat_WithHashNull_mavenEmptyExtension() {
    String associatedPackagedUrl = "pkg:pypi/ruamel.yaml@0.17.35?extension=whl&qualifier=py3-none-any";
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackageUrl(associatedPackagedUrl);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", null);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);

    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isFalse();
    assertThat(logOutput).atWarnLevel()
        .doesNotContain("The following coordinates are missing for given format: [extension]");
  }

  @Test
  public void testMatcherWrapper_matchesComponent_EXACT_COMPONENT_sameFormat_WithHashNull_mavenEmptyExtension() {
    PolicyWaiver policyWaiver =
        new PolicyWaiver().setComponentMatchStrategy(EXACT_COMPONENT)
            .setAssociatedPackageUrl(associatedPackagedUrl);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", null);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);

    PolicyWaiverMatcherWrapper policyWaiverMatcherWrapper = new PolicyWaiverMatcherWrapper(policyWaiver);

    assertThat(policyWaiverMatcherWrapper.matchesComponent(componentFact)).isFalse();
    assertThat(logOutput).atWarnLevel().contains("The following coordinates are missing for given format: [extension]");
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("{ \"conditionIndex\": " + i + ", \"trigger\": \"some trigger\" }");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    return constraintFacts;
  }
}
