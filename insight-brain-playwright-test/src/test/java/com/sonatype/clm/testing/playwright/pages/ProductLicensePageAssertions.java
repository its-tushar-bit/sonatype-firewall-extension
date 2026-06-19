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

  public void shouldShowPageHeading() {
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldShowLicenseDetails() {
    assertThat(page.licenseDetails()).isVisible();
  }

  public void shouldShowExpirationDate() {
    assertThat(page.expirationDate()).isVisible();
  }

  public void shouldShowLicenseTier() {
    assertThat(page.licenseTier()).isVisible();
  }

  public void shouldShowLicenseTypes() {
    assertThat(page.licenseTypes().first()).isVisible();
  }

  public void shouldShowInstallButton() {
    assertThat(page.installLicenseButton()).isVisible();
  }

  public void shouldShowEulaModal() {
    assertThat(page.eulaModal()).isVisible();
  }

  public void shouldShowEulaHeading() {
    assertThat(page.eulaModalHeading()).isVisible();
  }

  public void shouldShowEulaAcceptButton() {
    assertThat(page.eulaAcceptButton()).isVisible();
  }

  public void shouldShowEulaDeclineButton() {
    assertThat(page.eulaDeclineButton()).isVisible();
  }
}
