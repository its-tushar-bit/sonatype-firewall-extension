/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class AdvancedSearchConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#advanced-search-config-page-container";

  public AdvancedSearchConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/advancedSearchConfig";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Advanced Search Configuration").setExact(true));
  }

  public Locator tile() {
    return locator("#advanced-search-config");
  }
}
