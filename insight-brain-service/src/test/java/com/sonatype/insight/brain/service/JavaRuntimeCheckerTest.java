/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JavaRuntimeCheckerTest
{
  @Before
  @After
  public void cleanup() {
    System.clearProperty(JavaRuntimeChecker.PROP_DISABLE);
  }

  @Test
  public void testIsSupportedJre() {
    assertThat(JavaRuntimeChecker.isSupportedJre("Oracle Corporation"), is(true));
    assertThat(JavaRuntimeChecker.isSupportedJre("IBM Corporation"), is(false));
    assertThat(JavaRuntimeChecker.isSupportedJre("Sun Microsystems Inc."), is(false));
    assertThat(JavaRuntimeChecker.isSupportedJre(null), is(false));
    assertThat(JavaRuntimeChecker.isSupportedJre(""), is(false));
  }

  @Test
  public void testDisableCheck() {
    System.setProperty(JavaRuntimeChecker.PROP_DISABLE, "true");
    assertThat(JavaRuntimeChecker.isSupportedJre("Oracle Corporation"), is(true));
    assertThat(JavaRuntimeChecker.isSupportedJre("IBM Corporation"), is(true));
    assertThat(JavaRuntimeChecker.isSupportedJre("Sun Microsystems Inc."), is(true));
    assertThat(JavaRuntimeChecker.isSupportedJre(null), is(true));
    assertThat(JavaRuntimeChecker.isSupportedJre(""), is(true));
  }
}
