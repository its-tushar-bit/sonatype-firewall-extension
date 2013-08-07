/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PolicyWaiverTest
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
    PolicyWaiver policyWaiver = new PolicyWaiver(longHash, null /* policyId */, null /* ownerId */, null /* comment */);
    assertEquals(expectedTruncatedHash, policyWaiver.getHash());
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    PolicyWaiver policyWaiver = new PolicyWaiver();
    policyWaiver.setHash(longHash);
    assertEquals(expectedTruncatedHash, policyWaiver.getHash());
  }
}
