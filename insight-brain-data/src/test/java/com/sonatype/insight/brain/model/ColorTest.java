/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColorTest
{
  @Test
  public void testToValueReplacesUnderscoresWithHyphens() {
    assertEquals("light-red", Color.light_red.toValue());
  }

  @Test
  public void testToValueDoesNotChangeNamesWithoutUnderscores() {
    assertEquals("orange", Color.orange.toValue());
  }
}
