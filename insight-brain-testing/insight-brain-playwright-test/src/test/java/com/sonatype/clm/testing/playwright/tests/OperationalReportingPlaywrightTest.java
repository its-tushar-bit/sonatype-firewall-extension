/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPage;
import com.sonatype.clm.testing.playwright.pages.OperationalReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponentAssertions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for the Operational Reporting landing page.
 * <p>
 * Why "Operational" rather than "Enterprise" Reporting: the global sidebar renders one or the
 * other based on the {@code integrated-enterprise-reporting} product feature. That feature is
 * HDS-controlled and absent from the default {@code TestProductLicense}, so the harness sees
 * the Operational Reporting link pointing at {@code #/operationalReporting}.
 */
public class OperationalReportingPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String EXPECTED_HEADING = "Operational Reporting";

  private static final String EXPECTED_SIDEBAR_LINK_TEXT = "Operational Reporting";

  private static final String EXPECTED_URL_FRAGMENT = "/operationalReporting";

  private static final String EXPECTED_PAGE_TAB_TITLE = "Operational Reporting - Lifecycle";

  @BeforeEach
  public void openDashboardAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
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
    Assertions.assertThat(page.url()).contains(EXPECTED_URL_FRAGMENT);

    operationalReportingAssertions.shouldBeLoaded();
    operationalReportingAssertions.shouldHaveHeading(EXPECTED_HEADING);

    Assertions.assertThat(page.title()).isEqualTo(EXPECTED_PAGE_TAB_TITLE);

  }

  @Test
  @Tag("regression")
  public void testOperationalReporting_react2ShellCardIsVisible() {
    SidebarComponent sidebar = new SidebarComponent();
    sidebar.clickOperationalReportingNavigation();

    OperationalReportingPage operationalReporting = new OperationalReportingPage();
    OperationalReportingPageAssertions assertions =
        new OperationalReportingPageAssertions(operationalReporting);

    assertions.shouldBeLoaded();
    assertions.shouldShowReact2ShellCard();
  }

  @Test
  @Tag("regression")
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
