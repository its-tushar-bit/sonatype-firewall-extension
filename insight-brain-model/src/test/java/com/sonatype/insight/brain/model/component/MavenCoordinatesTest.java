/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MavenCoordinatesTest
{
  @Test
  public void testgetGAVECString() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("gg", "aa", "vv", "ee", "cc");
    assertEquals("gg:aa:vv:ee:cc", mavenCoordinates.getGAVECString());
  }

  @Test
  public void testgetGAVECString_OptionalCoordinates() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("gg", "aa", "vv", null /* extension */, null /* classifier */);
    assertEquals("gg:aa:vv", mavenCoordinates.getGAVECString());
  }
}
