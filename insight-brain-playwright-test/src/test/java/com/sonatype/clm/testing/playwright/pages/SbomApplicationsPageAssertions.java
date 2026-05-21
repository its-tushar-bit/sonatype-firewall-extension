/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link SbomApplicationsPage}.
 */
public class SbomApplicationsPageAssertions
{
  private final SbomApplicationsPage page;

  public SbomApplicationsPageAssertions(SbomApplicationsPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.loadingSpinner()).isHidden();
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowTitle() {
    assertThat(page.title()).isVisible();
  }

  public void shouldHaveColumnCount(int expectedCount) {
    assertThat(page.tableHeaderCells()).hasCount(expectedCount);
  }

  public void shouldHaveRowCount(int expectedCount) {
    assertThat(page.tableBodyRows()).hasCount(expectedCount);
  }

  public void firstRowShouldContainText(String expectedText) {
    assertThat(page.tableBodyRows().first()).containsText(expectedText);
  }

  public void firstRowShouldNotContainText(String notExpectedText) {
    assertThat(page.tableBodyRows().first()).not().containsText(notExpectedText);
  }

  public void shouldShowPaginationStatus(String expectedText) {
    assertThat(page.paginationStatus()).containsText(expectedText);
  }
}
