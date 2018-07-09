/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LauncherTest
{
  @Test
  public void testLauncher_DbUtil() {
    assertTrue(new Launcher("dbutil").runDbUtil);
    assertTrue(new Launcher("-dbutil").runDbUtil);
  }

  @Test
  public void testLauncher_UrlRunner() {
    assertTrue(new Launcher("urlrunner").runUrlRunner);
    assertTrue(new Launcher("-urlrunner").runUrlRunner);
  }

  @Test
  public void testLauncher_ScanScrubber() {
    assertTrue(new Launcher("scanscrubber").scanScrubber);
    assertTrue(new Launcher("-scanscrubber").scanScrubber);
  }
}
