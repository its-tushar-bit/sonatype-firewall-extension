/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PrioritiesPageAssertions
{
  private final PrioritiesPage page;

  public PrioritiesPageAssertions(PrioritiesPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowExpiredWaiverIconOnRow(Locator row) {
    assertThat(page.expiredWaiverIcon(row)).isVisible();
  }

  public void shouldShowSoonToExpireWaiverIconOnRow(Locator row) {
    assertThat(page.soonToExpireWaiverIcon(row)).isVisible();
  }

}
