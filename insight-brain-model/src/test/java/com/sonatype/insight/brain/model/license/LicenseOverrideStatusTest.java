/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
  public void testGetByDisplayName() {
    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      assertEquals(status, LicenseOverrideStatus.getByDisplayName(status.getDisplayName()));
    }
  }

  @Test
  public void testGetByDisplayName_Null() {
    assertNull(LicenseOverrideStatus.getByDisplayName(null));
  }

  @Test
  public void testGetByDisplayName_Invalid() {
    try {
      LicenseOverrideStatus.getByDisplayName("Yeti");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertEquals("Unknown license override status with display name: Yeti", expected.getMessage());
    }
  }
}
