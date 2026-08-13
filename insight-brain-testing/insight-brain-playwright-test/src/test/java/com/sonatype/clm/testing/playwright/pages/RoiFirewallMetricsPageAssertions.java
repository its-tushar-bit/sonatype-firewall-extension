/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RoiFirewallMetricsPageAssertions
{
  private final RoiFirewallMetricsPage page;

  public RoiFirewallMetricsPageAssertions(RoiFirewallMetricsPage page) {
    this.page = page;
  }

  public void shouldShowTileWithThreeMetrics() {
    assertThat(page.container()).isVisible();
    assertThat(page.title()).hasText("Return on Investment (ROI)");
    assertThat(page.totalSaved()).isVisible();
    assertThat(page.malwareTile()).isVisible();
    assertThat(page.namespaceTile()).isVisible();
    assertThat(page.safeComponentsTile()).isVisible();
  }

  public void shouldShowConfigureRoiLink() {
    assertThat(page.configureLink()).isVisible();
  }
}
