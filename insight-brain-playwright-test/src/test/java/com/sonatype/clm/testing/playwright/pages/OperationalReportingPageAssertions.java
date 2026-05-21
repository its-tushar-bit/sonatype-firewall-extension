/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link OperationalReportingPage}.
 */
public class OperationalReportingPageAssertions
{
  private final OperationalReportingPage page;

  public OperationalReportingPageAssertions(OperationalReportingPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldHaveHeading(String expectedHeading) {
    assertThat(page.pageHeading()).hasText(expectedHeading);
  }
}
