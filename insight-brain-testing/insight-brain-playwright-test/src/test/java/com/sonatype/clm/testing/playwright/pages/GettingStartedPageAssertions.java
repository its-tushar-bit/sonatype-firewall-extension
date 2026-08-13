/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link GettingStartedPage}. */
public class GettingStartedPageAssertions
{
  private final GettingStartedPage page;

  public GettingStartedPageAssertions(GettingStartedPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowProductLicenseSummaryTile() {
    assertThat(page.productLicenseSummaryTile()).isVisible();
  }

  public void shouldShowSystemSetupSection() {
    assertThat(page.systemSetupSection()).isVisible();
  }

  public void shouldShowLearningTopicsSection() {
    assertThat(page.learningTopicsSection()).isVisible();
  }

  /** Asserts the page container plus the 3 always-present sections render; HDS warning is conditional and skipped. */
  public void shouldShowAllAlwaysPresentSections() {
    shouldBeVisible();
    shouldShowProductLicenseSummaryTile();
    shouldShowSystemSetupSection();
    shouldShowLearningTopicsSection();
  }

  public void shouldShowLicenseSummaryDetails() {
    assertThat(page.licenseExpiryDate()).isVisible();
    assertThat(page.licenseExpiryDate()).not().hasText("");
    assertThat(page.licenseDaysToExpiration()).isVisible();
    assertThat(page.licenseDaysToExpiration()).not().hasText("");
    assertThat(page.licenseFingerprint()).isVisible();
    assertThat(page.licenseFingerprint()).not().hasText("");
    assertThat(page.licenseProducts()).isVisible();
    assertThat(page.licenseProducts()).not().hasText("");
  }
}
