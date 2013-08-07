/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// Copied from com.sonatype.nexus.procurement.ArtifactCoordinateTest
public class ArtifactCoordinateTest
{

  @Test
  public void testSimple() throws Exception {
    // one fixed coord
    ArtifactCoordinate coordF1 = new ArtifactCoordinate("org.group", "artifact", "1.0");
    assertTrue(coordF1.isFixed());

    // one fixed coord
    ArtifactCoordinate coordF2 = new ArtifactCoordinate("com.group", "artifact", "2.0");
    assertTrue(coordF2.isFixed());

    // one non-fixed coord
    ArtifactCoordinate coordN1 = new ArtifactCoordinate("org.*", "artifact", "1.*");
    assertFalse(coordN1.isFixed());

    // one non-fixed coord
    ArtifactCoordinate coordN2 = new ArtifactCoordinate("com.*", "artifact", "2.*");
    assertFalse(coordN2.isFixed());

    // one non-fixed coord
    ArtifactCoordinate coordN3 = new ArtifactCoordinate("org*", "artifact", "1.*");
    assertFalse(coordN3.isFixed());

    // one non-fixed coord
    ArtifactCoordinate coordN4 = new ArtifactCoordinate("com*", "artifact", "2.*");
    assertFalse(coordN4.isFixed());

    // pair matching
    // coordF1 is fixed, hence coordN1 is matchable against it and IS matching
    assertTrue(coordN1.matches(coordF1));
    // coordN1 is not fixed, hence coordF1 is not matchable against it
    assertFalse(coordF1.matches(coordN1));
    // coordF2 is fixed, hence coordN2 is matchable against it and IS matching
    assertTrue(coordN2.matches(coordF2));
    // coordN2 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF2.matches(coordN2));

    // cross matching
    // coordF2 is fixed, hence coordN1 is matchable against it and IS NOT matching
    assertFalse(coordN1.matches(coordF2));
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF2.matches(coordN1));
    // coordF1 is fixed, hence coordN2 is matchable against it and IS NOT matching
    assertFalse(coordN2.matches(coordF1));
    // coordN1 is not fixed, hence coordF2 is not matchable against it
    assertFalse(coordF1.matches(coordN2));

    // only subgroups
    assertTrue(coordN1.matches("org", "artifact", "1.2"));
    assertTrue(coordN1.matches("org", "artifact", "1.3"));
    assertFalse(coordN1.matches("organ", "artifact", "1.2"));
    assertFalse(coordN1.matches("organic", "artifact", "1.3"));
    assertFalse(coordN1.matches("organic.group", "artifact", "1.2"));
    assertTrue(coordN1.matches("org.group", "artifact", "1.2"));
    assertTrue(coordN1.matches("org.group.where", "artifact", "1.3"));
    assertFalse(coordN1.matches("org.group.something.boo", "artifact", "2.0"));
    assertFalse(coordN1.matches("org.group.something.boo", "non-artifact", "1.0"));

    // given group and subgroups
    assertTrue(coordN3.matches("org", "artifact", "1.2"));
    assertTrue(coordN3.matches("org", "artifact", "1.3"));
    assertTrue(coordN3.matches("organ", "artifact", "1.2"));
    assertTrue(coordN3.matches("organic", "artifact", "1.3"));
    assertTrue(coordN3.matches("organic.group", "artifact", "1.2"));
    assertTrue(coordN3.matches("org.group.where", "artifact", "1.3"));
    assertFalse(coordN3.matches("org.group.something.boo", "artifact", "2.0"));
    assertFalse(coordN3.matches("org.group.something.boo", "non-artifact", "1.0"));
  }

  @Test
  public void testNXCM195() {
    // path in question:
    // org/apache/maven/shared/maven-repository-builder/1.0-alpha-1/maven-repository-builder-1.0-alpha-1.jar
    // GAV: org.apache.maven.shared : maven-repository-builder : 1.0-alpha-1
    // one fixed coord
    ArtifactCoordinate coord1 = new ArtifactCoordinate("org.apache.maven", "*", "*");

    ArtifactCoordinate coord2 = new ArtifactCoordinate("org.apache.maven.*", "*", "*");

    assertFalse("Should not match!",
        coord1.matches("org.apache.maven.shared", "maven-repository-builder", "1.0-alpha-1"));
    assertTrue("Should match!", coord2.matches("org.apache.maven.shared", "maven-repository-builder", "1.0-alpha-1"));

    // assertFalse( "Should not match!", coord1.matches( "/org/apache/maven/kuku/a/v/" ) );
    // assertTrue( "Should match!", coord2.matches( "/org/apache/maven/kuku/a/v/" ) );
  }

}
