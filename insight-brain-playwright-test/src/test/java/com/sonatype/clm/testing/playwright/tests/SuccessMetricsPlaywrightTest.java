/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPage;
import com.sonatype.clm.testing.playwright.pages.SuccessMetricsPageAssertions;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SuccessMetricsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String EXPECTED_HEADING = "Success Metrics";

  private static final String EXPECTED_SIDEBAR_LINK_TEXT = "Success Metrics";

  private static final String EXPECTED_DESCRIPTION_SUBSTRING =
      "Success Metrics is an experimental feature providing high-level statistics on the past performance of Sonatype Lifecycle.";

  private static final String EXPECTED_REPORTS_TILE_HEADING = "Reports";

  private static final String EXPECTED_REPORTS_TILE_SUBTITLE_SUBSTRING =
      "Success Metrics data is also accessible via the";

  private static final String EXPECTED_DATA_API_LINK_TEXT = "Success Metrics Data API.";

  private static final String EXPECTED_ADD_REPORT_BUTTON_LABEL_SUBSTRING = "Add a Report";

  private static final String EXPECTED_EMPTY_REPORTS_MESSAGE = "No reports have been created.";

  private static final String EXPECTED_URL_FRAGMENT = "/labs/successMetrics";

  private static final String EXPECTED_PAGE_TAB_TITLE = "Success Metrics - Lifecycle";

  @Before
  public void ensureFeatureEnabledAndOpenDashboard() {
    ensureSuccessMetricsEnabled();
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testSuccessMetrics_FromDashboardNavigatesAndLandingPageLoads() {
    SidebarComponent sidebar = new SidebarComponent();
    SuccessMetricsPage successMetrics = new SuccessMetricsPage();
    SuccessMetricsPageAssertions assertions = new SuccessMetricsPageAssertions(successMetrics);

    assertThat(sidebar.container()).isVisible();
    assertThat(sidebar.labsButton()).isVisible();
    assertThat(sidebar.labsButton()).hasText(EXPECTED_SIDEBAR_LINK_TEXT);

    sidebar.clickSuccessMetricsNavigation();

    assertThat(page).hasURL(Pattern.compile(".*" + EXPECTED_URL_FRAGMENT + ".*"));

    assertions.shouldBeLoaded();
    assertions.shouldHaveHeading(EXPECTED_HEADING);
    assertions.shouldHaveDescriptionContaining(EXPECTED_DESCRIPTION_SUBSTRING);

    assertThat(page).hasTitle(EXPECTED_PAGE_TAB_TITLE);

    assertions.shouldHaveReportsTileHeading(EXPECTED_REPORTS_TILE_HEADING);
    assertions.shouldHaveReportsTileSubtitleContaining(EXPECTED_REPORTS_TILE_SUBTITLE_SUBSTRING);
    assertThat(successMetrics.dataApiDocLink()).hasText(EXPECTED_DATA_API_LINK_TEXT);
    assertions.shouldExposeDataApiDocLink();
    assertions.shouldShowAddReportButton(EXPECTED_ADD_REPORT_BUTTON_LABEL_SUBSTRING);

    assertThat(successMetrics.emptyReportListItem()).isVisible();
    assertThat(successMetrics.emptyReportListItem()).hasText(EXPECTED_EMPTY_REPORTS_MESSAGE);
  }

  private void ensureSuccessMetricsEnabled() {
    lookup(SystemConfigurationPropertyDAO.class).set(SuccessMetricsService.PROPERTY_ENABLED, "true");
  }
}
