/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Firewall main page.
 */
public class FirewallPage
    extends BasePage
{
  private static final String ROOT = "#firewall-page";

  public FirewallPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/firewall/dashboard";
  }

  public static String quarantineTabUrl() {
    return "/assets/index.html#/firewall/dashboard/components/quarantine";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return byRole(AriaRole.HEADING, "Firewall");
  }

  public Locator loadingSpinners() {
    return locator(ROOT + " .nx-loading-spinner");
  }

  public Locator limitedAccessAlert() {
    return locator(".iq-limited-firewall-access-alert");
  }

  // Firewall Status
  public Locator firewallStatus() {
    return locator("#firewall-status");
  }

  public Locator statusTitle() {
    return locator("#firewall-status .nx-h1");
  }

  public Locator statusPartiallyProtected() {
    return locator("#firewall-status .iq-firewall-status__status-indicator.nx-status-indicator--intermediate");
  }

  public Locator statusFullyProtected() {
    return locator("#firewall-status .iq-firewall-status__status-indicator.nx-status-indicator--positive");
  }

  public Locator componentsMonitored() {
    return locator("#firewall-status .iq-firewall-status__components-monitored");
  }

  // Firewall Metrics
  public Locator firewallMetrics() {
    return locator("#firewall-metrics");
  }

  // Firewall Quarantine Table
  public Locator quarantineTable() {
    return locator("#firewall-quarantine-table");
  }

  public Locator quarantineTableTitle() {
    return locator("#firewall-quarantine-table .nx-h3");
  }

  public Locator quarantineTableBody() {
    return locator("#iq-firewall-quarantine-table-body");
  }

  public Locator quarantineTableRows() {
    return locator("#iq-firewall-quarantine-table-body tr");
  }

  public Locator quarantineTableRow(int index) {
    return locator("#iq-firewall-quarantine-table-body tr:nth-child(" + (index + 1) + ")");
  }

  public Locator quarantineTableCellLink(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1) + ") td:nth-child(4) .nx-text-link");
  }

  /**
   * Anchored "go to component details" link inside the quarantine table — encapsulates the raw
   * selector previously sprinkled across tests. {@code rowIndex} is 0-based.
   */
  public Locator componentDetailsLink(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1)
            + ") #iq-firewall-quarantine-table--component-details-page");
  }

  /**
   * Click the component-details link in the first quarantine-table row. Used by tests that need
   * to drill into the {@link FirewallComponentDetailsPage} but don't care which component they
   * land on.
   */
  public void openFirstQuarantinedComponent() {
    componentDetailsLink(0).click();
  }

  // Firewall Auto-Unquarantine Status
  public Locator autoUnquarantineStatus() {
    return locator("#firewall-auto-unquarantine-status");
  }

  public Locator autoUnquarantineStatusTitle() {
    return locator("#firewall-auto-unquarantine-status .nx-h3");
  }

  // Tabs
  public Locator tab(String tabId) {
    return locator("#firewall-" + tabId + "-tab");
  }

  public Locator tabPanel(String tabId) {
    return locator("#firewall-" + tabId + "-tab-panel");
  }

  // CIP Modal
  public Locator cipModal() {
    return locator("#cip-modal");
  }

  // Firewall Welcome Modal
  public Locator welcomeModal() {
    return locator("#firewall-welcome-modal");
  }

  public Locator welcomeModalCloseButton() {
    return welcomeModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"));
  }
}
