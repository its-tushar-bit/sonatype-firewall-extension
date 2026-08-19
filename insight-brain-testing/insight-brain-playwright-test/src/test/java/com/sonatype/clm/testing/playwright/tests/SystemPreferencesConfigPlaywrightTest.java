/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SystemPreferencesRoutes;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SystemPreferencesConfigPlaywrightTest
    extends AbstractIqUiTest
{
  private SystemPreferencesRoutes routesPage;

  private List<Map.Entry<String, Supplier<Locator>>> routes;

  @BeforeEach
  public void setUp() {
    // Land on Mail Config to log in; Mail is intentionally absent from `routes` below — its render
    // assertion lives in MailConfigurationPlaywrightTest, so duplicating it here would be churn.
    playwrightRefreshOrOpen(SystemPreferencesRoutes.mailConfigUrl());
    playwrightLogin();
    routesPage = new SystemPreferencesRoutes();
    routes = List.of(
        Map.entry(SystemPreferencesRoutes.systemNoticeUrl(), routesPage::systemNoticeContainer),
        Map.entry(SystemPreferencesRoutes.waivedComponentUpgradesUrl(),
            routesPage::waivedComponentUpgradesContainer),
        Map.entry(SystemPreferencesRoutes.roiConfigurationUrl(), routesPage::roiConfigurationContainer),
        Map.entry(SystemPreferencesRoutes.automaticApplicationsUrl(),
            routesPage::automaticApplicationsContainer),
        Map.entry(SystemPreferencesRoutes.automaticSourceControlUrl(),
            routesPage::automaticSourceControlToggle),
        Map.entry(SystemPreferencesRoutes.scmOnboardingUrl(), routesPage::scmOnboardingContainer),
        Map.entry(SystemPreferencesRoutes.gettingStartedUrl(), routesPage::gettingStartedContainer),
        Map.entry(SystemPreferencesRoutes.dataInsightsUrl(), routesPage::dataInsightsContainer),
        Map.entry(SystemPreferencesRoutes.apiDocumentationUrl(), routesPage::apiDocumentationContainer),
        Map.entry(SystemPreferencesRoutes.userActivityUrl(), routesPage::userActivityContainer));
  }

  /** Wiring check — asserts each system-preferences route's container renders. */
  @Test
  @Tag("regression")
  public void testSystemPreferencePagesRender() {
    for (Map.Entry<String, Supplier<Locator>> route : routes) {
      playwrightRefreshOrOpen(route.getKey());
      assertThat(route.getValue().get()).isVisible();
    }
  }

  /**
   * Wiring check for the Zscaler route. Set as a firewall-only license because the {@code firewall/}
   * URL prefix in {@code SystemPreferencesMenu.jsx} only takes effect when the license is firewall-only
   * (or standalone firewall) — under the default regression license the menu wouldn't expose the route
   * at all, and the {@code firewall/} prefix wouldn't apply.
   */
  @Test
  @Tag("regression")
  public void testZscalerConfigurationPageRenders() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    playwrightRefreshOrOpen(SystemPreferencesRoutes.zscalerConfigUrl());
    assertThat(routesPage.zscalerConfigContainer()).isVisible();
  }
}
