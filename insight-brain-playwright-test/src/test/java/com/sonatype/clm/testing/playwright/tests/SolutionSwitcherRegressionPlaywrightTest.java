/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.SbomManagerDashboardPage;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for Solution Switcher navigation entries (Firewall, SBOM Manager, Developer).
 * <p>
 * Firewall and Developer appear in the real test-server license by default (Firewall via
 * {@code PRODUCT_FIREWALL_V2}; Developer auto-added by {@code CLMLicenseManager} because
 * {@code mockDeveloperEnablementService.shouldEnableDeveloperProduct()} returns {@code true}).
 * SBOM Manager is not in the defaults, so its test calls {@link #setLicensedProducts}.
 * <p>
 * Solution Switcher links carry {@code target="_blank"};
 * {@link Page#waitForPopup} captures the new tab so the destination URL can be asserted there.
 * The server is called with {@code allowRelativeUrls=true} by the frontend, so relative fragment
 * URLs (e.g. {@code #/firewall/repositories}) are returned and the popup URL assertion matches.
 */
public class SolutionSwitcherRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void loginToDashboard() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testSolutionSwitcher_firewallEntryNavigatesToFirewallDashboard() {
    playwrightRefreshOrOpen(DashboardPage.url());

    HeaderComponent header = new HeaderComponent();
    header.solutionSwitcherToggle().click();
    Locator link = header.solutionSwitcherLink("Repository Firewall");
    assertThat(link).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    Page popup = page.waitForPopup(link::click);
    assertThat(popup).hasURL(Pattern.compile(".*" + routeOf(FirewallPage.url()) + ".*"));
    assertThat(popup.getByRole(AriaRole.MAIN)).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testSolutionSwitcher_sbomManagerEntryNavigatesToSbomManagerDashboard() {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    playwrightRefreshOrOpen(DashboardPage.url());

    HeaderComponent header = new HeaderComponent();
    header.solutionSwitcherToggle().click();
    Locator link = header.solutionSwitcherLink("SBOM Manager");
    assertThat(link).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    Page popup = page.waitForPopup(link::click);
    assertThat(popup).hasURL(Pattern.compile(".*" + routeOf(SbomManagerDashboardPage.url()) + ".*"));
    assertThat(popup.getByRole(AriaRole.MAIN)).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testSolutionSwitcher_developerEntryNavigatesToDeveloperDashboard() {
    playwrightRefreshOrOpen(DashboardPage.url());

    HeaderComponent header = new HeaderComponent();
    header.solutionSwitcherToggle().click();
    Locator link = header.solutionSwitcherLink("Developer");
    assertThat(link).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    Page popup = page.waitForPopup(link::click);
    assertThat(popup).hasURL(Pattern.compile(".*" + routeOf(SonatypeDeveloperPage.url()) + ".*"));
    assertThat(popup.getByRole(AriaRole.MAIN)).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  private static String routeOf(String pageUrl) {
    int idx = pageUrl.indexOf("#/");
    return idx >= 0 ? pageUrl.substring(idx + 2) : pageUrl;
  }
}
