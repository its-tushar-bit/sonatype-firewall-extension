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
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyWaiverRequestMatcherWrapperTest
    extends AbstractPolicyEvaluationTest
{
  private final String notNullComponentFactErrorMessage = "componentFact is required but got null instead";

  private final String associatedPackagedUrl = "pkg:maven/group/artifact@2.0?classifier=c1&type=jar";

  @Test
  public void testMatchesPolicyId_null() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(new PolicyWaiverRequest().setPolicyId("policyId"));

    assertThat(policyWaiverRequestMatcherWrapper.matchesPolicyId(null)).isFalse();
  }

  @Test
  public void testMatchesPolicyId_empty() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(new PolicyWaiverRequest().setPolicyId("policyId"));

    assertThat(policyWaiverRequestMatcherWrapper.matchesPolicyId("")).isFalse();
  }

  @Test
  public void testMatchesPolicyId() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(new PolicyWaiverRequest().setPolicyId("policyId"));

    assertThat(policyWaiverRequestMatcherWrapper.matchesPolicyId("policyId")).isTrue();
  }

  @Test
  public void testMatchesComponent_null_DEFAULT() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setHash("hash").setComponentMatchStrategy(DEFAULT));

    assertThatThrownBy(() -> policyWaiverRequestMatcherWrapper.matchesComponent(null))
        .hasMessage(notNullComponentFactErrorMessage)
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatchesComponent_null_EXACT_COMPONENT() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setHash("hash").setComponentMatchStrategy(EXACT_COMPONENT));

    assertThatThrownBy(() -> policyWaiverRequestMatcherWrapper.matchesComponent(null))
        .hasMessage(notNullComponentFactErrorMessage)
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatchesComponent_null_ALL_COMPONENTS() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setHash("hash").setComponentMatchStrategy(ALL_COMPONENTS));

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(null)).isFalse();
  }

  @Test
  public void testMatchesComponent_null_ALL_VERSIONS() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setHash("hash").setComponentMatchStrategy(ALL_VERSIONS));

    assertThatThrownBy(() -> policyWaiverRequestMatcherWrapper.matchesComponent(null))
        .hasMessage(notNullComponentFactErrorMessage)
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testMatchesComponent_DEFAULT() {
    String hash = "hash";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash(hash)
        .setComponentMatchStrategy(DEFAULT)
        .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiverRequest.getComponentIdentifier(), hash);
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_EXACT_COMPONENT() {
    String hash = "hash";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash(hash)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiverRequest.getComponentIdentifier(), hash);
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_EXACT_COMPONENT_nullHash() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest()
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setAssociatedPackageUrl(associatedPackagedUrl);
    ComponentFact componentFact = new ComponentFact(policyWaiverRequest.getComponentIdentifier(), null);
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_EXACT_COMPONENT_nullHash_missingRequiredCoordinates() {
    String associatedPackagedUrl = "pkg:pypi/name?extension=e&qualifier=q";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest()
        .setComponentMatchStrategy(EXACT_COMPONENT)
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
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_ALL_COMPONENTS() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash(null)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setAssociatedPackageUrl(associatedPackagedUrl);
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(null)).isTrue();
  }

  @Test
  public void testMatchesComponent_ALL_VERSIONS() {
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", "jar");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_ALL_VERSIONS_UnknownComponent() {
    String hash = "hash";
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash(hash)
        .setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentFact componentFact = new ComponentFact(null, "otherHash");
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isFalse();
  }

  @Test
  public void testMatchesComponent_ALL_VERSIONS_missingRequiredCoordinates() {
    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@2.0?type=jar";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setComponentMatchStrategy(ALL_VERSIONS)
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
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  /**
   * This case miss match only happens with pypi components
   */
  @Test
  public void testMatchesComponent_ALL_VERSIONS_caseMissMatch() {
    String associatedPackagedUrlAllVersions = "pkg:pypi/py-component@1.0?extension=whl&qualifier=py3-none-any";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("Py-component", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatchesComponent_ALL_VERSIONS_PythonPackageWithDot() {
    String associatedPackagedUrlAllVersions = "pkg:pypi/ruamel.yaml@0.17.35?extension=whl&qualifier=py3-none-any";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("ruamel.yaml", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testCompareWhenMissingRequiredCoordinates() {
    // compareWhenMissingRequiredCoordinates method expects the coordinates already have the version as a wildcard (*)
    ComponentIdentifier componentIdentifierSame = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {
      {
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "*");
      }
    });

    ComponentIdentifier componentIdentifierOther = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {
      {
        this.put("artifactId", "otherArtifact");
        this.put("groupId", "group");
        this.put("version", "*");
      }
    });

    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar&classifier=";

    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setComponentMatchStrategy(ALL_VERSIONS)
        .setAssociatedPackageUrl(associatedPackagedUrlAllVersions);

    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper
        .compareWhenMissingRequiredCoordinates(policyWaiverRequest.getComponentIdentifier(), componentIdentifierSame))
            .isTrue();
    assertThat(policyWaiverRequestMatcherWrapper
        .compareWhenMissingRequiredCoordinates(policyWaiverRequest.getComponentIdentifier(), componentIdentifierOther))
            .isFalse();
  }

  @Test
  public void testMatchesConstraintFactsJson() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setConstraintFacts(createConstraintFacts(1));
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesConstraintFactsJson("")).isFalse();

    String constraintFactsJson = policyWaiverRequest.getConstraintFactsJson();

    assertThat(policyWaiverRequestMatcherWrapper.matchesConstraintFactsJson(constraintFactsJson)).isTrue();
  }

  @Test
  public void testMatchesConstraintFacts() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setConstraintFacts(createConstraintFacts(3));
    List<ConstraintFact> constraintFacts = policyWaiverRequest.getConstraintFacts();
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesConstraintFacts(constraintFacts)).isTrue();

    policyWaiverRequest = new PolicyWaiverRequest().setConstraintFacts(createConstraintFacts(1));
    policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesConstraintFacts(constraintFacts)).isFalse();
  }

  @Test
  public void testMatchesComponentOrAnyVersionOfComponent_ALL_COMPONENTS() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setComponentMatchStrategy(ALL_COMPONENTS));
    assertThat(policyWaiverRequestMatcherWrapper.matchesComponentOrAnyVersionOfComponent(new ComponentFact())).isTrue();
  }

  @Test
  public void testMatcherWrapper_matchesComponentOrAnyVersionOfComponent_EXACT_COMPONENT() {
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper = new PolicyWaiverRequestMatcherWrapper(
        new PolicyWaiverRequest().setHash("My hash").setComponentMatchStrategy(EXACT_COMPONENT));
    assertThat(
        policyWaiverRequestMatcherWrapper.matchesComponentOrAnyVersionOfComponent(new ComponentFact(null, "My hash")))
            .isTrue();
  }

  @Test
  public void testMatchesComponentOrAnyVersionOfComponent_matchesAllVersionsOfComponent() {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setAssociatedPackageUrl(associatedPackagedUrl);
    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);
    assertThat(policyWaiverRequestMatcherWrapper
        .matchesComponentOrAnyVersionOfComponent(new ComponentFact(policyWaiverRequest.getComponentIdentifier(), null)))
            .isTrue();
  }

  @Test
  public void testMatchesComponent_EXACT_COMPONENT_differentFormat_WithHashNull_mavenEmptyExtension() {
    String associatedPackagedUrl = "pkg:pypi/ruamel.yaml@0.17.35?extension=whl&qualifier=py3-none-any";
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest()
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setAssociatedPackageUrl(associatedPackagedUrl);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", null);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);

    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesComponent_EXACT_COMPONENT_sameFormat_WithHashNull_mavenEmptyExtension() {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest()
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setAssociatedPackageUrl(associatedPackagedUrl);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", null);

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);

    PolicyWaiverRequestMatcherWrapper policyWaiverRequestMatcherWrapper =
        new PolicyWaiverRequestMatcherWrapper(policyWaiverRequest);

    assertThat(policyWaiverRequestMatcherWrapper.matchesComponent(componentFact)).isFalse();
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
