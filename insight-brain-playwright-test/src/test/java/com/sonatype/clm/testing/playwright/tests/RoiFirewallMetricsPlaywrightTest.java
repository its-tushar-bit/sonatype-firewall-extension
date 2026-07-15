/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.RoiFirewallMetricsPage;
import com.sonatype.clm.testing.playwright.pages.RoiFirewallMetricsPageAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Metric values are hardcoded in {@code roiFirewallMetricsSlice.js#loadMetrics} (total 600000,
 * malware 100000, namespace 200000, safe 300000) — no server call fetches them. The only real
 * network call in the slice is the CONFIGURE_SYSTEM permission check.
 */
public class RoiFirewallMetricsPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void openRoiTabAsAdmin() {
    playwrightRefreshOrOpen(RoiFirewallMetricsPage.url());
    playwrightLogin();
  }

  @After
  public void cleanup() {
    playwrightLogout();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiFirewallMetrics_tileRendersWithThreeMetricsAndConfigureLink() {
    RoiFirewallMetricsPageAssertions assertions =
        new RoiFirewallMetricsPageAssertions(new RoiFirewallMetricsPage());

    assertions.shouldShowTileWithThreeMetrics();
    assertions.shouldShowConfigureRoiLink();
  }

  // Load-error/retry not automated here — see divergences log.
}
