/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.EnumSet;
import java.util.Set;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class EnterpriseReportingNavigationPlaywrightTest
    extends AbstractIqUiTest
{
  private record HdsStubs(JsonNode currentVersion, JsonNode dashboards)
  {
  }

  private static final String EXPECTED_HEADING = "Enterprise Reporting";

  private static final String EXPECTED_SIDEBAR_LINK_TEXT = "Enterprise Reporting";

  private static final String EXPECTED_URL_FRAGMENT = "/enterpriseReportingLandingPage";

  private static final String EXPECTED_PAGE_TAB_TITLE = "Enterprise Data Insights - Lifecycle";

  private static final String EXPECTED_ENTERPRISE_DASHBOARDS_SECTION_TITLE = "Enterprise Dashboards";

  private static final String EXPECTED_MOCK_DASHBOARD_ID = "sbom-scorecard";

  private static final String EXPECTED_MOCK_DASHBOARD_TITLE = "Sbom Report Overview";

  private static final HdsStubs ENTERPRISE_REPORTING_HDS =
      TestDataManager.load("enterprise-reporting-hds-stubs", HdsStubs.class);

  @Before
  public void enableEnterpriseReportingLicenseAndOpenDashboardAsAdmin() {
    enableIntegratedEnterpriseReportingOnLicense();
    stubHdsEndpoints();
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testEnterpriseReporting_FromDashboardSidebarShowsLandingPage() {
    SidebarComponent sidebar = new SidebarComponent();
    EnterpriseReportingPage enterpriseReporting = new EnterpriseReportingPage();
    EnterpriseReportingPageAssertions assertions = new EnterpriseReportingPageAssertions(enterpriseReporting);

    assertThat(sidebar.container()).isVisible();
    assertThat(sidebar.enterpriseReportingButton()).isVisible();
    assertThat(sidebar.enterpriseReportingButton()).hasText(EXPECTED_SIDEBAR_LINK_TEXT);
    assertThat(sidebar.operationalReportingButton()).isHidden();

    sidebar.clickEnterpriseReportingNavigation();

    assertThat(page).hasURL(Pattern.compile(".*" + EXPECTED_URL_FRAGMENT + ".*"));

    assertions.shouldBeLoaded();
    assertions.shouldHaveHeading(EXPECTED_HEADING);
    assertions.shouldShowEnterpriseDashboardsSectionHeading(EXPECTED_ENTERPRISE_DASHBOARDS_SECTION_TITLE);
    assertions.shouldShowEnterpriseDashboardCardWithTitle(
        EXPECTED_MOCK_DASHBOARD_ID, EXPECTED_MOCK_DASHBOARD_TITLE);

    assertThat(page).hasTitle(EXPECTED_PAGE_TAB_TITLE);
  }

  private void enableIntegratedEnterpriseReportingOnLicense() {
    Set<LicensedFeature> baseline = productLicenseManager.getFeatures();
    EnumSet<LicensedFeature> merged =
        baseline != null && !baseline.isEmpty()
            ? EnumSet.copyOf(baseline)
            : EnumSet.allOf(LicensedFeature.class);
    merged.add(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    setFeatures(merged.toArray(new LicensedFeature[0]));
  }

  private void stubHdsEndpoints() {
    if (ENTERPRISE_REPORTING_HDS.currentVersion() == null || ENTERPRISE_REPORTING_HDS.dashboards() == null) {
      throw new IllegalStateException(
          "test-data/enterprise-reporting-hds-stubs.json must define non-null \"currentVersion\" and \"dashboards\" objects");
    }
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.currentVersion().toString())
        .atUri("rest/enterpriseReporting/currentVersion");
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.dashboards().toString())
        .atUri("rest/enterpriseReporting/dashboards");
  }
}
