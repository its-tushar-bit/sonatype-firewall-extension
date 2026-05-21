/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AdvancedSearchPage}.
 */
public class AdvancedSearchPageAssertions
{
  private final AdvancedSearchPage page;

  public AdvancedSearchPageAssertions(AdvancedSearchPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldHaveHeading(String expected) {
    assertThat(page.pageHeading()).hasText(expected);
  }

  public void shouldHaveQueryValue(String expected) {
    assertThat(page.queryInput()).hasValue(expected);
  }
}
