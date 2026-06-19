/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponentAssertions;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for the Operational Reporting landing page.
 * <p>
 * Why "Operational" rather than "Enterprise" Reporting: the global sidebar in
 * {@code react/iqSidebarNav/IqSidebarNav.jsx} renders one or the other based on the
 * {@code integrated-enterprise-reporting} product feature. That feature is HDS-controlled
 * (see {@code CLMLicenseManager#hdsControlledFeatures}) and is therefore absent from the
 * default {@code TestProductLicense}, so the link the harness sees is
 * {@code #operational-reporting-button} pointing at {@code #/operationalReporting}.
 * <p>
 * Covers the canonical happy-path flow:
 * <ol>
 * <li>Admin logs in on the Dashboard.</li>
 * <li>Clicks the "Operational Reporting" item in the global sidebar
 * ({@code #operational-reporting-button}).</li>
 * <li>SPA navigates to {@code #/operationalReporting}.</li>
 * <li>Landing page heading + browser-tab title resolve to the values declared in
 * {@code operationalReporting/route.js} ({@code data.title = "Operational Reporting"}) and
 * {@code OperationalReportingLandingPage.jsx} (the {@code <NxH1>} text).</li>
 * </ol>
 */
public class OperationalReportingPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String EXPECTED_HEADING = "Operational Reporting";

  private static final String EXPECTED_SIDEBAR_LINK_TEXT = "Operational Reporting";

  private static final String EXPECTED_URL_FRAGMENT = "/operationalReporting";

  private static final String EXPECTED_PAGE_TAB_TITLE = "Operational Reporting - Lifecycle";

  @Before
  public void openDashboardAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testOperationalReporting_NavigateFromSidebarShowsLandingPage() {
    SidebarComponent sidebar = new SidebarComponent();
    SidebarComponentAssertions sidebarAssertions = new SidebarComponentAssertions(sidebar);
    OperationalReportingPage operationalReporting = new OperationalReportingPage();
    OperationalReportingPageAssertions operationalReportingAssertions =
        new OperationalReportingPageAssertions(operationalReporting);

    sidebarAssertions.shouldBeVisible();
    assertThat(sidebar.operationalReportingButton()).isVisible();
    assertThat(sidebar.operationalReportingButton()).hasText(EXPECTED_SIDEBAR_LINK_TEXT);

    sidebar.clickOperationalReportingNavigation();
    Assertions.assertThat(page.url())
        .as("Sidebar click should navigate to the Operational Reporting landing page hash route")
        .contains(EXPECTED_URL_FRAGMENT);

    operationalReportingAssertions.shouldBeLoaded();
    operationalReportingAssertions.shouldHaveHeading(EXPECTED_HEADING);

    Assertions.assertThat(page.title())
        .as("Browser tab title is set by documentTitle.js from the route data.title + product suffix")
        .isEqualTo(EXPECTED_PAGE_TAB_TITLE);

  }

  /**
   * Beyond the page heading + URL fragment covered by the sanity test, the landing page
   * renders a description paragraph, a "Rapid Response Reports" section, and a "Contact Us"
   * section. This regression-tier coverage guards against a future template change that
   * silently removes one of those structural elements without the heading-level test
   * failing.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOperationalReporting_landingPageContentSections() {
    SidebarComponent sidebar = new SidebarComponent();
    sidebar.clickOperationalReportingNavigation();

    OperationalReportingPage operationalReporting = new OperationalReportingPage();
    OperationalReportingPageAssertions assertions =
        new OperationalReportingPageAssertions(operationalReporting);

    assertions.shouldBeLoaded();
    assertions.shouldShowDescriptionContent();
    assertions.shouldShowReportingSections();
  }
}
