/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Product License page.
 */
public class ProductLicensePage
    extends BasePage
{
  public ProductLicensePage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/productLicense";
  }

  public Locator installLicenseButton() {
    return locator("#install-license-btn");
  }

}
