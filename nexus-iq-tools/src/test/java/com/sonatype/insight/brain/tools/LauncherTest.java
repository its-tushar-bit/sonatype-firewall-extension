/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LauncherTest
{
  @Test
  public void testLauncher_DbUtil() {
    assertThat(new Launcher("dbutil").runDbUtil).isTrue();
    assertThat(new Launcher("-dbutil").runDbUtil).isTrue();
  }

  @Test
  public void testLauncher_ReportGenerator() {
    assertThat(new Launcher("reportgenerator").reportGenerator).isTrue();
    assertThat(new Launcher("-reportgenerator").reportGenerator).isTrue();
  }

  @Test
  public void testLauncher_UrlRunner() {
    assertThat(new Launcher("urlrunner").runUrlRunner).isTrue();
    assertThat(new Launcher("-urlrunner").runUrlRunner).isTrue();
  }

  @Test
  public void testLauncher_ScanScrubber() {
    assertThat(new Launcher("scanscrubber").scanScrubber).isTrue();
    assertThat(new Launcher("-scanscrubber").scanScrubber).isTrue();
  }
}
