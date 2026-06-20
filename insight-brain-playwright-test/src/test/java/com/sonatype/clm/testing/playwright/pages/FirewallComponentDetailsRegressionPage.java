/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-specific page object for the Firewall Component Details page.
 * Sanity-facing locators and URL helpers live in {@link FirewallComponentDetailsPage}.
 */
public class FirewallComponentDetailsRegressionPage
    extends FirewallComponentDetailsPage
{
  public FirewallComponentDetailsRegressionPage() {
    super();
  }

  public Locator overviewTab() {
    return container().getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Overview"));
  }

  /** "Policy Violations" tab button (live label; manual suite listed it as "Violations"). */
  public Locator violationsTab() {
    return container().getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Policy Violations"));
  }

  @Override
  public Locator securityTab() {
    return container().getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Security"));
  }

  public Locator legalTab() {
    return container().getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Legal"));
  }

  public Locator labelsTab() {
    return container().getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Labels"));
  }
}
