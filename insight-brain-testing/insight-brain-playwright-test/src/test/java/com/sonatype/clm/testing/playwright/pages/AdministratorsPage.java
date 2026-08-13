/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the IQ Server Administrators list page.
 */
public class AdministratorsPage
    extends BasePage
{
  private static final String ROOT = "#administrators-config-container";

  public AdministratorsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/administrators";
  }

  /** Administrators page accessed from Firewall context. Matches ui-router state {@code firewall.administrators}. */
  public static String firewallUrl() {
    return "/assets/index.html#/firewall/administrators";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Administrators").setExact(true));
  }

  public Locator tileHeader() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure Administrators"));
  }

  public Locator table() {
    return container().getByRole(AriaRole.TABLE);
  }

  public Locator tableHeaderRoleCell() {
    return table().locator("thead th").first();
  }

  public Locator tableHeaderMembersCell() {
    return table().locator("thead th").nth(1);
  }

  public Locator rows() {
    return table().getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHas(locator("td")));
  }

  public int rowCount() {
    return rows().count();
  }

  public Locator row(int index) {
    return rows().nth(index);
  }

  public Locator roleCell(int rowIndex) {
    return row(rowIndex).getByRole(AriaRole.CELL).nth(0);
  }

  public Locator membersCell(int rowIndex) {
    return row(rowIndex).getByRole(AriaRole.CELL).nth(1);
  }

  public Locator chevron(int rowIndex) {
    return row(rowIndex).getByRole(AriaRole.CELL)
        .last()
        .getByRole(AriaRole.BUTTON);
  }

  public Locator errorMessage() {
    return table().getByRole(AriaRole.ALERT);
  }

  public Locator retryButton() {
    return table().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Retry"));
  }

  public Locator emptyMessage() {
    return table().locator(".nx-cell--meta-info");
  }

}
