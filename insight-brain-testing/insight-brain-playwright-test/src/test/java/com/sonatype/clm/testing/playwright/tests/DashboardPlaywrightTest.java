/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Dashboard shell tests using Playwright: tab bar, tab switching, and shared filter/export controls.
 *
 * <p>
 * This class owns assertions that span <strong>all</strong> dashboard tabs (page chrome, tab
 * switching, filter/export visibility). Per-tab content tests live in dedicated classes so each
 * tab follows the same one-test/one-json/one-page-object slice as the rest of the module:
 * <ul>
 * <li>Violations tab → {@code DashboardViolationsPlaywrightTest}</li>
 * <li>Components tab → {@code DashboardComponentsPlaywrightTest}</li>
 * <li>Applications tab → {@code DashboardApplicationsPlaywrightTest}</li>
 * <li>Waivers tab → {@code DashboardWaiversPlaywrightTest}</li>
 * <li>Waiver Requests tab → {@code DashboardWaiverRequestsPlaywrightTest}</li>
 * </ul>
 *
 * <p>
 * No nested {@code Data} or {@code Seeder} is needed here because tab-navigation checks do not
 * require seeded violations / components / waivers — those live in the per-tab classes.
 */
public class DashboardPlaywrightTest
    extends AbstractIqUiTest
{

  @BeforeEach
  public void openDashboardAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    new DashboardPage().waitUntilSpinnersGone();
  }

  @AfterEach
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Tag("sanity")
  public void testDashboardLoadsAndTabsAreVisible() {
    DashboardPage dashboard = new DashboardPage();
    DashboardPageAssertions dashboardAssertions = new DashboardPageAssertions(dashboard);
    dashboardAssertions.shouldBeLoaded();
    dashboardAssertions.shouldShowAllTabs();
    dashboardAssertions.violationsTabShouldBeActive();
    assertThat(page).hasURL(Pattern.compile(".*/dashboard/violations.*"));
  }

  @Test
  @Tag("sanity")
  public void testSwitchBetweenTabs() {
    DashboardPage dashboard = new DashboardPage();

    dashboard.selectComponentsTabAndWait();

    dashboard.selectApplicationsTabAndWait();

    dashboard.selectWaiversTabAndWait();

    dashboard.selectViolationsTabAndWait();
  }

  @Test
  @Tag("sanity")
  public void testFilterToggleAndExportButton_areVisible() {
    DashboardPage dashboard = new DashboardPage();
    DashboardPageAssertions dashboardAssertions = new DashboardPageAssertions(dashboard);
    dashboardAssertions.shouldShowFilterToggle();
    dashboardAssertions.shouldShowExportButton();
  }
}
