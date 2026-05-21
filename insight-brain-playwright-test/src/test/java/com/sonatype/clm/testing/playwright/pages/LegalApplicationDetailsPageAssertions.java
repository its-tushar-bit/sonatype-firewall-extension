/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link LegalApplicationDetailsPage}.
 */
public class LegalApplicationDetailsPageAssertions
{
  private final LegalApplicationDetailsPage page;

  public LegalApplicationDetailsPageAssertions(LegalApplicationDetailsPage page) {
    this.page = page;
  }

  public void shouldShowTitle(String appName) {
    assertThat(page.pageTitle()).containsText(appName + " Obligations");
  }

  public void shouldShowTableWithRowCount(int expectedCount) {
    assertThat(page.table()).isVisible();
    assertThat(page.tableDataRows()).hasCount(expectedCount);
  }

  public void shouldShowColumnHeader(String columnText) {
    assertThat(
        page.table().locator("th.nx-cell--header").filter(new Locator.FilterOptions().setHasText(columnText)))
            .isVisible();
  }

  public void shouldShowFilterSidebarClosed() {
    assertThat(page.filterSidebar()).not().isVisible();
  }
}
