/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test of {@link HashComponentIdentifier} model.
 */
public class HashComponentIdentifierTest
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
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(longHash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertEquals(expectedTruncatedHash, hashComponentIdentifier.getHash());
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier();
    hashComponentIdentifier.setHash(longHash);
    assertEquals(expectedTruncatedHash, hashComponentIdentifier.getHash());
  }
}
