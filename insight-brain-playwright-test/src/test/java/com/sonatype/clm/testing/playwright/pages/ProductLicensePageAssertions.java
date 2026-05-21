/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ProductLicensePage}.
 */
public class ProductLicensePageAssertions
{
  private final ProductLicensePage page;

  public ProductLicensePageAssertions(ProductLicensePage page) {
    this.page = page;
  }

  public void shouldShowInstallButton() {
    assertThat(page.installLicenseButton()).isVisible();
  }
}
