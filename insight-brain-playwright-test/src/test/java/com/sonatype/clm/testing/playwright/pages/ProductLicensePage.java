/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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
    return "/assets/index.html#/productlicense";
  }

  public Locator pageHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Product License"));
  }

  public Locator licenseDetails() {
    return page.locator(".nx-read-only").first();
  }

  public Locator expirationDate() {
    return locator("#license-expiry-date");
  }

  public Locator licenseTier() {
    return locator("#license-tier");
  }

  public Locator licenseTypes() {
    return page.locator(".license-product");
  }

  public Locator daysToExpiration() {
    return locator("#license-days-to-expiration");
  }

  public Locator installLicenseButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Install License"));
  }

  public Locator licenseFileInput() {
    return locator("#license-input");
  }

  public Locator eulaModal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator eulaModalHeading() {
    return eulaModal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("End User License Agreement"));
  }

  public Locator eulaAcceptButton() {
    return eulaModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("I Accept"));
  }

  public Locator eulaDeclineButton() {
    return eulaModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("I Decline"));
  }
}
