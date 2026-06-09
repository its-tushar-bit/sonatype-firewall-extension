/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OwnersTreePageAssertions
{
  private final OwnersTreePage page;

  public OwnersTreePageAssertions(OwnersTreePage page) {
    this.page = page;
  }

  public void shouldBeVisibleWithAtLeastOneItem() {
    assertThat(page.container()).isVisible();
    assertThat(page.firstItemLabel()).isVisible();
  }

  public void shouldShowLoadError() {
    assertThat(page.loadErrorAlert()).isVisible();
  }

  public void shouldShowTreeContent() {
    assertThat(page.tree()).isVisible();
  }
}
