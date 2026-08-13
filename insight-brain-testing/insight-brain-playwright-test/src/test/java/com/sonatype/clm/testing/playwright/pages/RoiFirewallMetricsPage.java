/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** ROI tab of the Firewall dashboard — only rendered when {@code roiEnabled=true} is in the URL. */
public class RoiFirewallMetricsPage
    extends BasePage
{
  public RoiFirewallMetricsPage() {
    super();
  }

  public static String url() {
    return FirewallPage.url() + "/roi?roiEnabled=true";
  }

  public Locator container() {
    return byTestId("roi-firewall-metrics");
  }

  /** NxTooltip title concatenates into the h2's accessible name — anchor by data-testid instead. */
  public Locator title() {
    return byTestId("roi-firewall-metrics-title");
  }

  public Locator totalSaved() {
    return byTestId("roi-firewall-metrics-total");
  }

  public Locator configureLink() {
    return container().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Configure ROI values"));
  }

  public Locator malwareTile() {
    return byTestId("roi-firewall-metrics-content__title__malware-attacks-prevented");
  }

  public Locator namespaceTile() {
    return byTestId("roi-firewall-metrics-content__title__namespace-attacks-prevented");
  }

  public Locator safeComponentsTile() {
    return byTestId("roi-firewall-metrics-content__title__safe-components-auto-selected");
  }
}
