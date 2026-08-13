/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for the Firewall Dashboard regression tests ({@link FirewallRegressionPage}).
 */
public class FirewallDashboardRegressionAssertions
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsHiddenOptions HIDDEN_OPTS =
      new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String COMPONENTS_TAB_ID = "components";

  private static final String CONTAINERS_TAB_ID = "containers";

  private final FirewallPage page;

  public FirewallDashboardRegressionAssertions(FirewallPage page) {
    this.page = page;
  }

  public void shouldShowLimitedAccessAlert() {
    assertThat(page.limitedAccessAlert()).isVisible();
  }

  public void shouldShowOuterTabBar() {
    assertThat(page.tab(COMPONENTS_TAB_ID)).isVisible(VISIBLE_OPTS);
    assertThat(page.tab(CONTAINERS_TAB_ID)).isVisible(VISIBLE_OPTS);
  }

  public void shouldHideOuterTabBar() {
    assertThat(page.tab(COMPONENTS_TAB_ID)).isHidden(HIDDEN_OPTS);
    assertThat(page.tab(CONTAINERS_TAB_ID)).isHidden(HIDDEN_OPTS);
  }

  public void shouldShowWelcomeModal() {
    assertThat(page.welcomeModal()).isVisible();
  }

  public void shouldHideWelcomeModal() {
    assertThat(page.welcomeModal()).isHidden(HIDDEN_OPTS);
  }

  public void shouldShowTabPanel(String tabId) {
    assertThat(page.tabPanel(tabId)).isVisible(VISIBLE_OPTS);
  }

  /** Asserts the green "fully protected" status indicator is visible. */
  public void shouldShowGreenStatus() {
    assertThat(page.statusFullyProtected()).isVisible();
  }

  public void shouldShowAmberStatus() {
    assertThat(page.statusPartiallyProtected()).isVisible();
  }

  /**
   * Asserts the quarantine table is not present — expected when the limited-access alert replaces dashboard content.
   */
  public void shouldHideDashboardContent() {
    assertThat(page.quarantineTable()).isHidden(HIDDEN_OPTS);
  }
}
