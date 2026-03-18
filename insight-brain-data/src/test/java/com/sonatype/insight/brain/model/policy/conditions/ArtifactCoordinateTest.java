/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Copied from com.sonatype.nexus.procurement.ArtifactCoordinateTest
public class ArtifactCoordinateTest
{
  @Test
  public void testSimple() {
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
    assertThat(coordN1.matches(coordF1.getComponentIdentifier())).isTrue();
    // coordN1 is not fixed, hence coordF1 is not matchable against it
    assertThat(coordF1.matches(coordN1.getComponentIdentifier())).isFalse();
    // coordF2 is fixed, hence coordN2 is matchable against it and IS matching
    assertThat(coordN2.matches(coordF2.getComponentIdentifier())).isTrue();
    // coordN2 is not fixed, hence coordF2 is not matchable against it
    assertThat(coordF2.matches(coordN2.getComponentIdentifier())).isFalse();

    // cross matching
    // coordF2 is fixed, hence coordN1 is matchable against it and IS NOT matching
    assertThat(coordN1.matches(coordF2.getComponentIdentifier())).isFalse();
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertThat(coordF2.matches(coordN1.getComponentIdentifier())).isFalse();
    // coordF1 is fixed, hence coordN2 is matchable against it and IS NOT matching
    assertThat(coordN2.matches(coordF1.getComponentIdentifier())).isFalse();
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertThat(coordF1.matches(coordN2.getComponentIdentifier())).isFalse();

    // only subgroups
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.2"))).isTrue();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.3"))).isTrue();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organ", "artifact", "1.2"))).isFalse();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organic", "artifact", "1.3"))).isFalse();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("organic.group", "artifact", "1.2")))
        .isFalse();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group", "artifact", "1.2"))).isTrue();
    assertThat(coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group.where", "artifact", "1.3")))
        .isTrue();
    assertThat(
        coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "artifact", "2.0")))
            .isFalse();
    assertThat(
        coordN1.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "non-artifact", "1.0")))
            .isFalse();

    // given group and subgroups
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.2"))).isTrue();
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org", "artifact", "1.3"))).isTrue();
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organ", "artifact", "1.2"))).isTrue();
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organic", "artifact", "1.3"))).isTrue();
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("organic.group", "artifact", "1.2")))
        .isTrue();
    assertThat(coordN3.matches(ComponentIdentifier.createMavenCoordinates("org.group.where", "artifact", "1.3")))
        .isTrue();
    assertThat(
        coordN3.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "artifact", "2.0")))
            .isFalse();
    assertThat(
        coordN3.matches(ComponentIdentifier.createMavenCoordinates("org.group.something.boo", "non-artifact", "1.0")))
            .isFalse();
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

    assertThat(coord1.matches(ComponentIdentifier.createMavenCoordinates("org.apache.maven.shared",
        "maven-repository-builder", "1.0-alpha-1"))).as("Should not match!").isFalse();
    assertThat(coord2.matches(ComponentIdentifier.createMavenCoordinates("org.apache.maven.shared",
        "maven-repository-builder", "1.0-alpha-1"))).as("Should match!").isTrue();

    // assertFalse( "Should not match!", coord1.matches( "/org/apache/maven/kuku/a/v/" ) );
    // assertTrue( "Should match!", coord2.matches( "/org/apache/maven/kuku/a/v/" ) );
  }

  @Test
  public void testMatches_EmptyClassifierCoordinate_DoesMatch_EmptyClassifierValue() {
    ArtifactCoordinate coordinateWithEmptyClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e");
    assertThat(coordinateWithEmptyClassifier.matches(valueWithEmptyClassifier)).isTrue();
  }

  @Test
  public void testMatches_EmptyClassifierCoordinate_DoesNotMatch_NonEmptyClassifierValue() {
    ArtifactCoordinate coordinateWithEmptyClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"));
    ComponentIdentifier valueWithClassifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThat(coordinateWithEmptyClassifier.matches(valueWithClassifier)).isFalse();
  }

  @Test
  public void testMatches_WildcardCoordinate_DoesMatch_EmptyClassifierValue() {
    ArtifactCoordinate coordinateWithWildcardClassifier = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "*", "e"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "", "e");
    assertThat(coordinateWithWildcardClassifier.matches(valueWithEmptyClassifier)).isTrue();
  }

  @Test
  public void testMatches_NonEmptyCoordinate_DoesNotMatch_EmptyClassifierValue() {
    ArtifactCoordinate coordinate = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("org.apache.commons", "commons-lang3", "3.5", "sources", "jar"));
    ComponentIdentifier valueWithEmptyClassifier = ComponentIdentifier
        .createMavenCoordinates("org.apache.commons", "commons-lang3", "3.5", "", "jar");
    assertThat(coordinate.matches(valueWithEmptyClassifier)).isFalse();
  }

  @Test
  public void testMatches_ValueWithNull_DoesNotThrowNullPointerException() {
    ArtifactCoordinate mavenCoordinate = new ArtifactCoordinate(
        ComponentIdentifier.createMavenCoordinates("g*", "a*", "v*", "c*", "e*"));
    ComponentIdentifier mavenValueWithNullGroupId = ComponentIdentifier
        .createMavenCoordinates(null, "a", "v", "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullGroupId)).isFalse();
    ComponentIdentifier mavenValueWithNullArtifactId = ComponentIdentifier
        .createMavenCoordinates("g", null, "v", "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullArtifactId)).isFalse();
    ComponentIdentifier mavenValueWithNullVersion = ComponentIdentifier
        .createMavenCoordinates("g", "a", null, "c", "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullVersion)).isFalse();
    ComponentIdentifier mavenValueWithNullClassifier = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", null, "e");
    assertThat(mavenCoordinate.matches(mavenValueWithNullClassifier)).isFalse();
    ComponentIdentifier mavenValueWithNullExtension = ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", null);
    assertThat(mavenCoordinate.matches(mavenValueWithNullExtension)).isFalse();

    ArtifactCoordinate anameCoordinate = new ArtifactCoordinate(
        ComponentIdentifier.createAnameCoordinates("n*", "q*", "v*"));
    ComponentIdentifier anameValueWithNullName = ComponentIdentifier.createAnameCoordinates(null, "q", "v");
    assertThat(anameCoordinate.matches(anameValueWithNullName)).isFalse();
    ComponentIdentifier anameValueWithNullQualifier = ComponentIdentifier.createAnameCoordinates("n", null, "v");
    assertThat(anameCoordinate.matches(anameValueWithNullQualifier)).isFalse();
    ComponentIdentifier anameValueWithNullVersion = ComponentIdentifier.createAnameCoordinates("n", "q", null);
    assertThat(anameCoordinate.matches(anameValueWithNullVersion)).isFalse();
  }

  @Test
  public void testMatchesIgnoreCase() {
    ComponentIdentifier pypiCoordinate1 =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe");
    ComponentIdentifier pypiCoordinate2 =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.11", "WIN32-py3.2", "EXE");
    ComponentIdentifier pypiCoordinate3 =
        ComponentIdentifier.createPypiCoordinates("PyYAML", "3.12", "win-amd64-py2.8", "zip");

    ArtifactCoordinate pypyExactCoordinate = new ArtifactCoordinate(
        ComponentIdentifier.createPypiCoordinates("pyyaml", "3.11", "win-amd64-py2.7", "exe"));
    assertThat(pypyExactCoordinate.matches(pypiCoordinate1)).isTrue();
    assertThat(pypyExactCoordinate.matches(pypiCoordinate2)).isFalse();
    assertThat(pypyExactCoordinate.matches(pypiCoordinate3)).isFalse();

    ArtifactCoordinate pypyCoordWithWildcardQualifier = new ArtifactCoordinate(
        ComponentIdentifier.createPypiCoordinates("pyyaml", "3.11", "win*", "exe"));
    assertThat(pypyCoordWithWildcardQualifier.matches(pypiCoordinate1)).isTrue();
    assertThat(pypyCoordWithWildcardQualifier.matches(pypiCoordinate2)).isTrue();
    assertThat(pypyCoordWithWildcardQualifier.matches(pypiCoordinate3)).isFalse();

    ArtifactCoordinate pypyCoordWithAllWildcards = new ArtifactCoordinate(
        ComponentIdentifier.createPypiCoordinates("pyy*", "3*", "win*", "*"));
    assertThat(pypyCoordWithAllWildcards.matches(pypiCoordinate1)).isTrue();
    assertThat(pypyCoordWithAllWildcards.matches(pypiCoordinate2)).isTrue();
    assertThat(pypyCoordWithAllWildcards.matches(pypiCoordinate3)).isTrue();
  }

  @Test
  public void testMatchesIgnoreCase_NullCandidateIdentifier() {
    ArtifactCoordinate pypyCoord = new ArtifactCoordinate(
        ComponentIdentifier.createPypiCoordinates("pyy*", "3*", "win*", "*"));
    assertThat(pypyCoord.matches(null)).isFalse();
  }

  @Test
  public void testMatchesIgnoreCase_FormatMismatch() {
    ComponentIdentifier mavenCoordinates = ComponentIdentifier.createMavenCoordinates("pyy", "*", "*", "*", "*");

    ArtifactCoordinate pypyCoord = new ArtifactCoordinate(
        ComponentIdentifier.createPypiCoordinates("pyy*", "3*", "win*", "*"));
    assertThat(pypyCoord.matches(mavenCoordinates)).isFalse();
  }
}
