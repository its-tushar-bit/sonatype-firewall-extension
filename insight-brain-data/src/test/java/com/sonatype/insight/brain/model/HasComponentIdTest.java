/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class HasComponentIdTest
{

  @Test
  public void testJsonFormattingForMaven() {
    HasComponentId hasComponentId = new PolicyViolation();
    //classifier and extension are excluded from the stored results
    hasComponentId.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, null));
    assertThat(hasComponentId.getComponentIdCoordinatesJson(),
        is("{\"artifactId\":\"a\",\"groupId\":\"g\",\"version\":\"v\"}"));
    assertThat(hasComponentId.getComponentIdFormat(), is(ComponentIdentifier.FORMAT_MAVEN));
  }

  @Test
  public void testJsonFormattingForNuget() {
    HasComponentId hasComponentId = new LicenseOverride();
    hasComponentId.setComponentIdentifier(ComponentIdentifier.createNugetCoordinates("a", "v"));
    assertThat(hasComponentId.getComponentIdCoordinatesJson(), is("{\"packageId\":\"a\",\"version\":\"v\"}"));
    assertThat(hasComponentId.getComponentIdFormat(), is(ComponentIdentifier.FORMAT_NUGET));
  }

  @Test
  public void testNullFormat(){
    HasComponentId hasComponentId = new PolicyViolation();
    assertThat(hasComponentId.getComponentIdentifier(), is(nullValue()));
  }
}
