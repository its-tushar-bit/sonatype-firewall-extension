/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class MtiqSystemPreferencesPage
    extends BasePage
{
  public MtiqSystemPreferencesPage() {
    super();
  }

  /** Trigger button has no accessible name — scoped via {@code #system-configuration-menu} container ID. */
  public Locator menuToggle() {
    return locator("#system-configuration-menu button");
  }

  /**
   * "Administrators" menu item ({@code id="system-configuration-administrators"}).
   * Present in all MTIQ tenants — used as a positive gate before asserting excluded items.
   */
  public Locator administratorsLink() {
    return locator("#system-configuration-administrators");
  }

  /**
   * "Base URL" menu item ({@code id="system-configuration-base-url"}).
   * Absent in MTIQ: {@code selectIsBaseUrlConfigurationEnabled} returns {@code false}
   * ({@code selectTenantMode === SINGLE_TENANT} is false in MTIQ).
   */
  public Locator baseUrlLink() {
    return locator("#system-configuration-base-url");
  }

  /**
   * "Email" menu item ({@code id="system-configuration-email"}).
   * Absent in MTIQ for tenants with no custom mail config (CLM-38607):
   * {@code MTIQFeatureService} removes {@code EMAIL_CONFIGURATION} when
   * {@code mailConfigurationDAO.getWithoutFallback() == null}.
   */
  public Locator emailLink() {
    return locator("#system-configuration-email");
  }
}
