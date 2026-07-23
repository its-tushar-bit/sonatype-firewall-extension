/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class SystemConfigMenuComponent
    extends BasePage
{
  private static final String MENU_ID = "#system-configuration-menu";

  public SystemConfigMenuComponent() {
    super();
  }

  public Locator menu() {
    return locator(MENU_ID);
  }

  public Locator dropdownToggle() {
    return locator(MENU_ID + " button");
  }

  /** Open the dropdown so the item locators below resolve to visible elements. */
  public void open() {
    dropdownToggle().click();
  }

  public Locator users() {
    return locator("#system-configuration-users");
  }

  public Locator roles() {
    return locator("#system-configuration-roles");
  }

  public Locator administrators() {
    return locator("#system-configuration-administrators");
  }

  public Locator productLicense() {
    return locator("#system-configuration-product-license");
  }

  public Locator ldap() {
    return locator("#system-configuration-ldap");
  }

  public Locator automaticScmConfiguration() {
    return locator("#system-configuration-automatic-scm-configuration");
  }

  public Locator webhooks() {
    return locator("#system-configuration-webhooks");
  }

  public Locator systemNotice() {
    return locator("#system-configuration-system-notice");
  }

  public Locator successMetrics() {
    return locator("#system-configuration-success-metrics");
  }

  public Locator emailConfiguration() {
    return locator("#system-configuration-email");
  }

  public Locator proxyConfiguration() {
    return locator("#system-configuration-proxy");
  }

  public Locator advancedSearchConfiguration() {
    return locator("#system-configuration-advanced-search");
  }

  public Locator baseUrlConfiguration() {
    return locator("#system-configuration-base-url");
  }

  public Locator samlConfiguration() {
    return locator("#system-configuration-saml");
  }
}
