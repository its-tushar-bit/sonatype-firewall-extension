/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HasComponentIdTest
{
  @Test
  public void testJsonFormattingForMaven() {
    HasComponentId hasComponentId = new PolicyViolation();
    // classifier and extension are excluded from the stored results
    hasComponentId.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, null));
    assertThat(hasComponentId.getComponentIdCoordinatesJson())
        .isEqualTo("{\"artifactId\":\"a\",\"groupId\":\"g\",\"version\":\"v\"}");
    assertThat(hasComponentId.getComponentIdFormat()).isEqualTo(ComponentIdentifier.FORMAT_MAVEN);
  }

  @Test
  public void testJsonFormattingForNuget() {
    HasComponentId hasComponentId = new LicenseOverrideInternal();
    hasComponentId.setComponentIdentifier(ComponentIdentifier.createNugetCoordinates("a", "v"));
    assertThat(hasComponentId.getComponentIdCoordinatesJson()).isEqualTo("{\"packageId\":\"a\",\"version\":\"v\"}");
    assertThat(hasComponentId.getComponentIdFormat()).isEqualTo(ComponentIdentifier.FORMAT_NUGET);
  }

  @Test
  public void testNullFormat() {
    HasComponentId hasComponentId = new PolicyViolation();
    assertThat(hasComponentId.getComponentIdentifier()).isNull();
  }
}
