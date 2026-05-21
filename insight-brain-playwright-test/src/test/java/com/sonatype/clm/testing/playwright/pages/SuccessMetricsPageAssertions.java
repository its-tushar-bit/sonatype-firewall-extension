/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link SuccessMetricsPage}.
 */
public class SuccessMetricsPageAssertions
{
  private final SuccessMetricsPage page;

  public SuccessMetricsPageAssertions(SuccessMetricsPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.reportsTile()).isVisible();
  }

  public void shouldHaveHeading(String expectedHeading) {
    assertThat(page.pageHeading()).hasText(expectedHeading);
  }

  public void shouldHaveDescriptionContaining(String expectedSubstring) {
    assertThat(page.pageDescription()).containsText(expectedSubstring);
  }

  public void shouldHaveReportsTileHeading(String expectedHeading) {
    assertThat(page.reportsTileHeading()).hasText(expectedHeading);
  }

  public void shouldHaveReportsTileSubtitleContaining(String expectedSubstring) {
    assertThat(page.reportsTileSubtitle()).containsText(expectedSubstring);
  }

  public void shouldExposeDataApiDocLink() {
    assertThat(page.dataApiDocLink()).isVisible();
    assertThat(page.dataApiDocLink()).hasAttribute("href", SuccessMetricsPage.DATA_API_DOC_LINK_HREF);
  }

  public void shouldShowAddReportButton(String expectedLabelSubstring) {
    assertThat(page.addReportButton()).isVisible();
    assertThat(page.addReportButton()).isEnabled();
    assertThat(page.addReportButton()).containsText(expectedLabelSubstring);
  }
}
