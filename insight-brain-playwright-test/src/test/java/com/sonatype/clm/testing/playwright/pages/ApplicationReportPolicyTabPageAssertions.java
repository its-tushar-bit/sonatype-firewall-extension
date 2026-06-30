/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ApplicationReportPolicyTabPage}. */
public class ApplicationReportPolicyTabPageAssertions
{
  private final ApplicationReportPolicyTabPage page;

  public ApplicationReportPolicyTabPageAssertions(ApplicationReportPolicyTabPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.appReportMain()).isVisible();
  }

  public void shouldShowFilterPopoverOpen() {
    assertThat(page.filterPopover()).isVisible();
  }

  public void shouldShowFilterPopoverContainsOptions() {
    assertThat(page.violationStateFilter()).isVisible();
    assertThat(page.waivedFilterOption()).isVisible();
    assertThat(page.legacyFilterOption()).isVisible();
  }

  public void shouldShowLegacyIndicatorTagOn(Locator row) {
    assertThat(page.legacyIndicatorTagIn(row)).isVisible();
    assertThat(page.legacyIndicatorTagIn(row)).containsText("Legacy");
  }

  public void shouldHaveViolationStateSectionExpanded() {
    assertThat(page.violationStateSectionTrigger()).hasAttribute("aria-expanded", "true");
  }
}
