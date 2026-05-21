/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ApplicationReportPage}.
 */
public class ApplicationReportPageAssertions
{
  private final ApplicationReportPage page;

  public ApplicationReportPageAssertions(ApplicationReportPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.appReportMain()).isVisible();
  }

  public void shouldShowReportHeaderContaining(String text) {
    // Wait for the report shell first; the title renders only after metadata loads.
    assertThat(page.appReportMain()).isVisible();
    assertThat(page.reportHeaderTitle()).containsText(text);
  }
}
