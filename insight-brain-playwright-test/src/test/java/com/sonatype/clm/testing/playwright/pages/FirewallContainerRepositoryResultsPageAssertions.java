/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link FirewallContainerRepositoryResultsPage}.
 */
public class FirewallContainerRepositoryResultsPageAssertions
{
  private final FirewallContainerRepositoryResultsPage page;

  public FirewallContainerRepositoryResultsPageAssertions(FirewallContainerRepositoryResultsPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowTitle(String expectedText) {
    assertThat(page.title()).containsText(expectedText);
  }

  public void shouldShowResultsTable() {
    assertThat(page.resultsTable()).isVisible();
  }
}
