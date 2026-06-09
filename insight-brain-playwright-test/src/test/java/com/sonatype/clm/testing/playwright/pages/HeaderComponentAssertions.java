/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link HeaderComponent}.
 */
public class HeaderComponentAssertions
{
  private final HeaderComponent page;

  public HeaderComponentAssertions(HeaderComponent page) {
    this.page = page;
  }

  public void shouldBeLoggedIn() {
    assertThat(page.menuBar()).isVisible();
  }

  public void shouldShowUserName(String expectedName) {
    assertThat(page.userName()).hasText(expectedName);
  }

  public void shouldShowMenuBar() {
    assertThat(page.menuBar()).isVisible();
  }
}
