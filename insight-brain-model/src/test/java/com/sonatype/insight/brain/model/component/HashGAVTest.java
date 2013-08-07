/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test of {@link HashGAV} model.
 */
public class HashGAVTest
{
  private final String longHash = "123456789012345678901";

  /**
   * 20 characters as currently specified in HashHelper
   */
  private final String expectedTruncatedHash = "12345678901234567890";

  @Before
  public void preconditions() {
    assertTrue(longHash.length() > 20);
  }

  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    HashGAV hashGAV = new HashGAV(longHash, null /* groupId */, null /* artifactId */, null /* version */,
        null /* extension */, null /* classifier */);
    assertEquals(expectedTruncatedHash, hashGAV.getHash());
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    HashGAV hashGAV = new HashGAV();
    hashGAV.setHash(longHash);
    assertEquals(expectedTruncatedHash, hashGAV.getHash());
  }
}
