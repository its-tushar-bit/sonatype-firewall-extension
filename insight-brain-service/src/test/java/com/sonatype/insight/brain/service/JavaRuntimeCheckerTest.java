/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaRuntimeCheckerTest
{
  @Before
  @After
  public void cleanup() {
    System.clearProperty(JavaRuntimeChecker.PROP_DISABLE);
  }

  @Test
  public void testIsSupportedJre() {
    assertThat(JavaRuntimeChecker.isSupportedJre("Oracle Corporation")).isTrue();
    assertThat(JavaRuntimeChecker.isSupportedJre("IBM Corporation")).isFalse();
    assertThat(JavaRuntimeChecker.isSupportedJre("Sun Microsystems Inc.")).isFalse();
    assertThat(JavaRuntimeChecker.isSupportedJre(null)).isFalse();
    assertThat(JavaRuntimeChecker.isSupportedJre("")).isFalse();
  }

  @Test
  public void testDisableCheck() {
    System.setProperty(JavaRuntimeChecker.PROP_DISABLE, "true");
    assertThat(JavaRuntimeChecker.isSupportedJre("Oracle Corporation")).isTrue();
    assertThat(JavaRuntimeChecker.isSupportedJre("IBM Corporation")).isTrue();
    assertThat(JavaRuntimeChecker.isSupportedJre("Sun Microsystems Inc.")).isTrue();
    assertThat(JavaRuntimeChecker.isSupportedJre(null)).isTrue();
    assertThat(JavaRuntimeChecker.isSupportedJre("")).isTrue();
  }
}
