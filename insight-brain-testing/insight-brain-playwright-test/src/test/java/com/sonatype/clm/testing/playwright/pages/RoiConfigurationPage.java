/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class RoiConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#roi-configuration-page";

  public RoiConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/roiConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Return on Investment").setExact(true));
  }

  public Locator tileHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Return on Investment Values").setExact(true));
  }

  public Locator editLink() {
    return container().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Edit").setExact(true));
  }

  public Locator baselineDaysValue() {
    return locator("#roi-configuration-page__numeric-value__baseline-days-to-resolve-violation");
  }

  public Locator dailyRiskValue() {
    return locator("#roi-configuration-page__numeric-value__daily-risk-cost-of-unfixed-violation");
  }

  public Locator lifecycleMetricsHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Lifecycle Metrics").setExact(true).setLevel(2));
  }

  public Locator firewallMetricsHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Repository Firewall Metrics").setExact(true).setLevel(2));
  }
}
