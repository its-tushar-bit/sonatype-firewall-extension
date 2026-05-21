/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link DashboardPage}.
 */
public class DashboardPageAssertions
{
  private final DashboardPage page;

  public DashboardPageAssertions(DashboardPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.dashboardContainer()).isVisible();
    page.waitUntilSpinnersGone();
  }

  public void shouldShowViolationsTab() {
    assertThat(page.violationsTab()).isVisible();
  }

  public void violationsTabShouldBeActive() {
    assertThat(page.violationsTab()).hasAttribute("aria-selected", "true");
  }

  public void shouldShowAllTabs() {
    assertThat(page.violationsTab()).isVisible();
    assertThat(page.componentsTab()).isVisible();
    assertThat(page.applicationsTab()).isVisible();
    assertThat(page.waiversTab()).isVisible();
  }

  public void shouldShowFilterToggle() {
    assertThat(page.filterToggle()).isVisible();
  }

  public void shouldShowExportButton() {
    assertThat(page.exportResultsLink()).isVisible();
  }

  public void shouldNotShowDashboardDisabledMessage() {
    assertThat(page.dashboardDisabledMessage()).isHidden();
  }
}
