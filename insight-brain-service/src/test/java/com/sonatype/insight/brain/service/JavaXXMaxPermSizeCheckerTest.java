/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JavaXXMaxPermSizeCheckerTest
{
  @Before
  @After
  public void cleanup() {
    System.clearProperty(JavaXXMaxPermSizeChecker.PROP_DISABLE);
  }

  @Test
  public void testIsValid() {
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", JavaXXMaxPermSizeChecker.MEGABYTE_IN_BYTES * 128), is(true));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", JavaXXMaxPermSizeChecker.MEGABYTE_IN_BYTES * 128 - 1),
        is(false));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", null), is(false));

    assertThat(JavaXXMaxPermSizeChecker.isValid("1.8.0_05", 0L), is(true));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.8.0_05", null), is(true));
  }

  @Test
  public void testDisableCheck() {
    System.setProperty(JavaXXMaxPermSizeChecker.PROP_DISABLE, "true");

    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", JavaXXMaxPermSizeChecker.MEGABYTE_IN_BYTES * 128), is(true));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", JavaXXMaxPermSizeChecker.MEGABYTE_IN_BYTES * 128 - 1),
        is(true));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.7.0_60", null), is(true));

    assertThat(JavaXXMaxPermSizeChecker.isValid("1.8.0_05", 0L), is(true));
    assertThat(JavaXXMaxPermSizeChecker.isValid("1.8.0_05", null), is(true));
  }

  @Test
  public void testGetJavaVersionFromString() {
    assertThat(JavaXXMaxPermSizeChecker.getJavaVersionFromString("1.7"), is(7));
    assertThat(JavaXXMaxPermSizeChecker.getJavaVersionFromString("1.7.0_60"), is(7));
    assertThat(JavaXXMaxPermSizeChecker.getJavaVersionFromString("1.8"), is(8));
    assertThat(JavaXXMaxPermSizeChecker.getJavaVersionFromString("1.8.0_05"), is(8));
  }
}
