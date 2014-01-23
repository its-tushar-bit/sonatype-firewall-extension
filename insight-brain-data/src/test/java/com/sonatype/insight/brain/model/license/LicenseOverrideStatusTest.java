/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @since 1.6
 */
public class LicenseOverrideStatusTest
{
  @Test
  public void testGetByName() {
    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      assertEquals(status, LicenseOverrideStatus.getByName(status.getName()));
    }
  }

  @Test
  public void testGetByName_Null() {
    assertNull(LicenseOverrideStatus.getByName(null));
  }

  @Test
  public void testGetByName_Invalid() {
    try {
      LicenseOverrideStatus.getByName("Yeti");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertEquals("Unknown license override status with name: Yeti", expected.getMessage());
    }
  }
}
