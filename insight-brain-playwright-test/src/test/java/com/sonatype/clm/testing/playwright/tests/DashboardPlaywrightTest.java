/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

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

  @Before
  public void openDashboardAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    new DashboardPage().waitUntilSpinnersGone();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testDashboardLoadsAndTabsAreVisible() {
    DashboardPage dashboard = new DashboardPage();
    DashboardPageAssertions dashboardAssertions = new DashboardPageAssertions(dashboard);
    dashboardAssertions.shouldBeLoaded();
    dashboardAssertions.shouldShowAllTabs();
  }

  @Test
  @Category(SanityTest.class)
  public void testSwitchBetweenTabs() {
    DashboardPage dashboard = new DashboardPage();

    dashboard.selectComponentsTabAndWait();

    dashboard.selectApplicationsTabAndWait();

    dashboard.selectWaiversTabAndWait();

    dashboard.selectViolationsTabAndWait();
  }

  @Test
  @Category(SanityTest.class)
  public void testFilterToggleAndExportButton_areVisible() {
    DashboardPage dashboard = new DashboardPage();
    DashboardPageAssertions dashboardAssertions = new DashboardPageAssertions(dashboard);
    dashboardAssertions.shouldShowFilterToggle();
    dashboardAssertions.shouldShowExportButton();
  }
}
