/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// Copied from com.sonatype.nexus.procurement.ArtifactCoordinateTest
public class ArtifactCoordinateTest
{

  @Test
  public void testSimple() throws Exception {
    // one fixed coord
    ArtifactCoordinate coordF1 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("org.group",
        "artifact", "1.0"));

    // one fixed coord
    ArtifactCoordinate coordF2 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("com.group",
        "artifact", "2.0"));

    // one non-fixed coord
    ArtifactCoordinate coordN1 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("org.*", "artifact",
        "1.*"));

    // one non-fixed coord
    ArtifactCoordinate coordN2 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("com.*", "artifact",
        "2.*"));

    // one non-fixed coord
    ArtifactCoordinate coordN3 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("org*", "artifact",
        "1.*"));

    // pair matching
    // coordF1 is fixed, hence coordN1 is matchable against it and IS matching
    assertTrue(coordN1.matches(coordF1.getComponentIdentifier()));
    // coordN1 is not fixed, hence coordF1 is not matchable against it
    assertFalse(coordF1.matches(coordN1.getComponentIdentifier()));
    // coordF2 is fixed, hence coordN2 is matchable against it and IS matching
    assertTrue(coordN2.matches(coordF2.getComponentIdentifier()));
    // coordN2 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF2.matches(coordN2.getComponentIdentifier()));

    // cross matching
    // coordF2 is fixed, hence coordN1 is matchable against it and IS NOT matching
    assertFalse(coordN1.matches(coordF2.getComponentIdentifier()));
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF2.matches(coordN1.getComponentIdentifier()));
    // coordF1 is fixed, hence coordN2 is matchable against it and IS NOT matching
    assertFalse(coordN2.matches(coordF1.getComponentIdentifier()));
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF1.matches(coordN2.getComponentIdentifier()));

    // only subgroups
    assertTrue(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.2")));
    assertTrue(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.3")));
    assertFalse(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organ", "artifact", "1.2")));
    assertFalse(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organic", "artifact", "1.3")));
    assertFalse(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organic.group", "artifact", "1.2")));
    assertTrue(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group", "artifact", "1.2")));
    assertTrue(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group.where", "artifact", "1.3")));
    assertFalse(coordN1.matches(ComponentIdentifier
        .createMavenCoordinates("org.group.something.boo", "artifact", "2.0")));
    assertFalse(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "non-artifact",
        "1.0")));

    // given group and subgroups
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.2")));
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.3")));
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organ", "artifact", "1.2")));
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organic", "artifact", "1.3")));
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organic.group", "artifact", "1.2")));
    assertTrue(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org.group.where", "artifact", "1.3")));
    assertFalse(coordN3.matches(ComponentIdentifier
        .createMavenCoordinates("org.group.something.boo", "artifact", "2.0")));
    assertFalse(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "non-artifact",
        "1.0")));
  }

  @Test
  public void testNXCM195() {
    // path in question:
    // org/apache/maven/shared/maven-repository-builder/1.0-alpha-1/maven-repository-builder-1.0-alpha-1.jar
    // GAV: org.apache.maven.shared : maven-repository-builder : 1.0-alpha-1
    // one fixed coord
    ArtifactCoordinate coord1 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("org.apache.maven",
        "*", "*"));

    ArtifactCoordinate coord2 = new ArtifactCoordinate(ComponentIdentifier.createMavenCoordinates("org.apache.maven.*",
        "*", "*"));

    assertFalse("Should not match!", coord1.matches(ComponentIdentifier.createMavenCoordinates(
        "org.apache.maven.shared", "maven-repository-builder", "1.0-alpha-1")));
    assertTrue("Should match!", coord2.matches(ComponentIdentifier.createMavenCoordinates("org.apache.maven.shared",
        "maven-repository-builder", "1.0-alpha-1")));

    // assertFalse( "Should not match!", coord1.matches( "/org/apache/maven/kuku/a/v/" ) );
    // assertTrue( "Should match!", coord2.matches( "/org/apache/maven/kuku/a/v/" ) );
  }

  @Test
  public void testMatches_EmptyClassifierCoordinate_DoesMatch_EmptyClassifierValue() throws Exception {
    ArtifactCoordinate coordinateWithEmptyClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e");
    assertThat(coordinateWithEmptyClassifier.matches(valueWithEmptyClassifier), is(true));
  }

  @Test
  public void testMatches_EmptyClassifierCoordinate_DoesNotMatch_NonEmptyClassifierValue() throws Exception {
    ArtifactCoordinate coordinateWithEmptyClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    ComponentIdentifier valueWithClassifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThat(coordinateWithEmptyClassifier.matches(valueWithClassifier), is(false));
  }

  @Test
  public void testMatches_WildcardCoordinate_DoesMatch_EmptyClassifierValue() throws Exception {
    ArtifactCoordinate coordinateWithWildcardClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "*", "e"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "", "e");
    assertThat(coordinateWithWildcardClassifier.matches(valueWithEmptyClassifier), is(true));
  }

  @Test
  public void testMatches_NonEmptyCoordinate_DoesNotMatch_EmptyClassifierValue() throws Exception {
    ArtifactCoordinate coordinate = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("org.apache.commons", "commons-lang3", "3.5", "sources", "jar"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier
        .createMavenCoordinates("org.apache.commons", "commons-lang3", "3.5", "", "jar");
    assertThat(coordinate.matches(valueWithEmptyClassifier), is(false));
  }

  @Test
  public void testMatches_ValueWithNull_DoesNotThrowNullPointerException() throws Exception {
    ArtifactCoordinate mavenCoordinate = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g*", "a*", "v*", "c*", "e*"));
    ComponentIdentifier mavenValueWithNullGroupId = ComponentIdentifier
        .createMavenCoordinates(null, "a", "v", "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullGroupId), is(false));
    ComponentIdentifier mavenValueWithNullArtifactId = ComponentIdentifier
        .createMavenCoordinates("g", null, "v", "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullArtifactId), is(false));
    ComponentIdentifier mavenValueWithNullVersion = ComponentIdentifier
        .createMavenCoordinates("g", "a", null, "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullVersion), is(false));
    ComponentIdentifier mavenValueWithNullClassifier = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", null, "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullClassifier), is(false));
    ComponentIdentifier mavenValueWithNullExtension = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", null);
    assertThat(mavenCoordinate.matches(mavenValueWithNullExtension), is(false));

    ArtifactCoordinate anameCoordinate = new ArtifactCoordinate(
        ComponentIdentifier.createAnameCoordinates("n*", "q*", "v*"));
    ComponentIdentifier anameValueWithNullName = ComponentIdentifier.createAnameCoordinates(null, "q", "v");
    assertThat(anameCoordinate.matches(anameValueWithNullName), is(false));
    ComponentIdentifier anameValueWithNullQualifier = ComponentIdentifier.createAnameCoordinates("n", null, "v");
    assertThat(anameCoordinate.matches(anameValueWithNullQualifier), is(false));
    ComponentIdentifier anameValueWithNullVersion = ComponentIdentifier.createAnameCoordinates("n", "q", null);
    assertThat(anameCoordinate.matches(anameValueWithNullVersion), is(false));
  }
}
