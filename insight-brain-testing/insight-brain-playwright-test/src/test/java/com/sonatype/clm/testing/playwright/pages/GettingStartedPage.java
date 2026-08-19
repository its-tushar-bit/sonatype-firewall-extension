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
 * Page object for the Getting Started page at {@code /assets/index.html#/gettingStarted}. The
 * three always-present tiles are located by region role + accessible name.
 */
public class GettingStartedPage
    extends BasePage
{
  public GettingStartedPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/gettingStarted";
  }

  public Locator container() {
    return locator("#getting-started");
  }

  /** Region landmark with accessible name "Product License". */
  public Locator productLicenseSummaryTile() {
    return page.getByRole(AriaRole.REGION,
        new Page.GetByRoleOptions().setName("Product License"));
  }

  /** Region landmark with accessible name "System Setup". */
  public Locator systemSetupSection() {
    return page.getByRole(AriaRole.REGION,
        new Page.GetByRoleOptions().setName("System Setup"));
  }

  /** Region landmark with accessible name "Learning Topics". */
  public Locator learningTopicsSection() {
    return page.getByRole(AriaRole.REGION,
        new Page.GetByRoleOptions().setName("Learning Topics"));
  }

  // Leaf license-detail ids — no ARIA role on NxReadOnly rows.

  public Locator licenseExpiryDate() {
    return productLicenseSummaryTile().locator("#license-expiry-date");
  }

  public Locator licenseDaysToExpiration() {
    return productLicenseSummaryTile().locator("#license-days-to-expiration");
  }

  public Locator licenseFingerprint() {
    return productLicenseSummaryTile().locator("#license-fingerprint");
  }

  public Locator licenseProducts() {
    return productLicenseSummaryTile().locator("#license-products");
  }
}
