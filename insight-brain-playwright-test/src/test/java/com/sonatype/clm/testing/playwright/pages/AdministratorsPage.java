/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the IQ Server Administrators list page.
 */
public class AdministratorsPage
    extends BasePage
{
  private static final String ROOT = ".nx-tile";

  public AdministratorsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/administrators";
  }

  // --------------- Role mapping table ---------------

  public Locator rows() {
    return locator(ROOT + " tbody .nx-table-row");
  }

  public int rowCount() {
    return rows().count();
  }

  public Locator row(int index) {
    return rows().nth(index);
  }

  public Locator roleCell(int rowIndex) {
    return row(rowIndex).locator(".nx-cell").nth(0);
  }

  public Locator membersCell(int rowIndex) {
    return row(rowIndex).locator(".nx-cell").nth(1);
  }

  public Locator chevron(int rowIndex) {
    return row(rowIndex).locator(".fa-chevron-right");
  }

}
