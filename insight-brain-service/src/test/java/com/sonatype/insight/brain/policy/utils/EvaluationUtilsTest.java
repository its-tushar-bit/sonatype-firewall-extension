/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EvaluationUtilsTest
{
  @Test
  public void testIsRemediatedByVersionChange_Upgrade_ReturnsTrue() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    List<Component> foundComponents = List.of(createComponentWithVersion("2.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isTrue();
  }

  @Test
  public void testIsRemediatedByVersionChange_Downgrade_ReturnsTrue() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("2.0.0");
    List<Component> foundComponents = List.of(createComponentWithVersion("1.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isTrue();
  }

  @Test
  public void testIsRemediatedByVersionChange_SameVersion_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    List<Component> foundComponents = List.of(createComponentWithVersion("1.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_NullComponents_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");

    boolean result = EvaluationUtils.isRemediatedByVersionChange(null, oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_EmptyComponents_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");

    boolean result = EvaluationUtils.isRemediatedByVersionChange(Collections.emptyList(), oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_MultipleComponentsWithoutOldVersion_ReturnsTrue() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    List<Component> foundComponents = List.of(
        createComponentWithVersion("2.0.0"),
        createComponentWithVersion("3.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isTrue();
  }

  @Test
  public void testIsRemediatedByVersionChange_MultipleComponentsWithOldVersion_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    List<Component> foundComponents = List.of(
        createComponentWithVersion("1.0.0"),
        createComponentWithVersion("2.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_NullOldVersion_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion(null);
    List<Component> foundComponents = List.of(createComponentWithVersion("2.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_NullNewVersion_ReturnsFalse() {
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    List<Component> foundComponents = List.of(createComponentWithVersion(null));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  @Test
  public void testIsRemediatedByVersionChange_NullComponentIdentifier_ReturnsFalse() {
    PolicyViolation oldViolation = mock(PolicyViolation.class);
    when(oldViolation.getComponentIdentifier()).thenReturn(null);
    List<Component> foundComponents = List.of(createComponentWithVersion("2.0.0"));

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  private PolicyViolation createPolicyViolationWithVersion(String version) {
    PolicyViolation violation = mock(PolicyViolation.class);
    Map<String, String> coordinates = new TreeMap<>();
    coordinates.put("groupId", "com.example");
    coordinates.put("artifactId", "test-artifact");
    if (version != null) {
      coordinates.put(ComponentIdentifier.VERSION, version);
    }
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    when(violation.getComponentIdentifier()).thenReturn(componentIdentifier);
    return violation;
  }

  @Test
  public void testIsRemediatedByVersionChange_MixedNullAndNonNullVersions_ReturnsFalse() {
    // Test mixed scenario where foundComponents contains both null and valid version components
    PolicyViolation oldViolation = createPolicyViolationWithVersion("1.0.0");
    Component nullVersionComponent = createComponentWithVersion(null);
    Component validVersionComponent = createComponentWithVersion("2.0.0");
    List<Component> foundComponents = List.of(nullVersionComponent, validVersionComponent);

    boolean result = EvaluationUtils.isRemediatedByVersionChange(foundComponents, oldViolation);

    assertThat(result).isFalse();
  }

  private Component createComponentWithVersion(String version) {
    Component component = mock(Component.class);
    when(component.getVersion()).thenReturn(version);
    return component;
  }
}
