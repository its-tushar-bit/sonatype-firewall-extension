/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AdvancedSearchNavigationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String EXPECTED_SIDEBAR_LINK_TEXT = "Advanced Search";

  private static final String EXPECTED_PAGE_HEADING = "Advanced Search";

  private static final String EXPECTED_URL_FRAGMENT = "/advancedSearch";

  private static final String EXPECTED_PAGE_TAB_TITLE = "Advanced Search - Lifecycle";

  private static final String SAMPLE_KEYWORD = "log4j";

  private static final String EXPECTED_RESULTS_LABEL_PREFIX = "Results:";

  @BeforeEach
  public void ensureAdvancedSearchEnabledAndOpenDashboardAsAdmin() {
    ensureAdvancedSearchEnabled();
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testAdvancedSearch_FromDashboardSidebarNavigationAndKeywordFlow() {
    HeaderComponent header = new HeaderComponent();
    SidebarComponent sidebar = new SidebarComponent();
    AdvancedSearchPage advancedSearch = new AdvancedSearchPage();
    AdvancedSearchPageAssertions assertions = new AdvancedSearchPageAssertions(advancedSearch);

    assertThat(header.menuBar()).isVisible();
    assertThat(sidebar.container()).isVisible();
    assertThat(sidebar.advancedSearchButton()).isVisible();
    assertThat(sidebar.advancedSearchButton()).hasText(EXPECTED_SIDEBAR_LINK_TEXT);

    sidebar.clickAdvancedSearchNavigation();
    assertThat(page).hasURL(Pattern.compile(".*" + EXPECTED_URL_FRAGMENT + ".*"));
    assertions.shouldBeLoaded();
    assertions.shouldHaveHeading(EXPECTED_PAGE_HEADING);
    assertThat(page).hasTitle(EXPECTED_PAGE_TAB_TITLE);

    advancedSearch.runKeywordSearch(SAMPLE_KEYWORD);
    assertThat(advancedSearch.resultCountHeading()).containsText(EXPECTED_RESULTS_LABEL_PREFIX);

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightSpaNavigateToHashFragment(AdvancedSearchPage.hashRouteWithSearchQuery(SAMPLE_KEYWORD));
    assertThat(page).hasURL(Pattern.compile(
        ".*" + EXPECTED_URL_FRAGMENT + ".*search=" + SAMPLE_KEYWORD));
    assertions.shouldBeLoaded();
    advancedSearch.ensureDeepLinkKeywordApplied(SAMPLE_KEYWORD);
    assertThat(advancedSearch.resultCountHeading()).containsText(EXPECTED_RESULTS_LABEL_PREFIX);

    playwrightRefreshOrOpen(DashboardPage.url());
    sidebar.clickAdvancedSearchNavigation();
    assertions.shouldBeLoaded();
  }

  private void ensureAdvancedSearchEnabled() {
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true");
  }

  @Test
  @Tag("regression")
  public void testAdvancedSearchConfigurationPageRenders() {
    playwrightRefreshOrOpen(AdvancedSearchConfigurationPage.url());

    AdvancedSearchConfigurationPage configPage = new AdvancedSearchConfigurationPage();
    assertThat(configPage.container()).isVisible();
    assertThat(configPage.pageHeading()).isVisible();
    assertThat(configPage.tile()).isVisible();
  }
}
