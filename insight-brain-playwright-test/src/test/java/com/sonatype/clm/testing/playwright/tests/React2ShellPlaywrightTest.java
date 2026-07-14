/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.React2ShellPage;
import com.sonatype.clm.testing.playwright.pages.React2ShellPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** {@code /api/v2/componentSearch/cveAffectedComponents} is mocked — embedded server has no React2Shell scan data. */
public class React2ShellPlaywrightTest
    extends AbstractIqUiTest
{
  private record ImpactData(JsonNode apiResponse)
  {
  }

  private static final String CVE_AFFECTED_COMPONENTS_ENDPOINT =
      "**/api/v2/componentSearch/cveAffectedComponents**";

  private static final ImpactData IMPACT_DATA =
      TestDataManager.load("react2shell-impact-data", ImpactData.class);

  @Before
  public void openReact2ShellPageAsAdmin() {
    playwrightRefreshOrOpen(React2ShellPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testReact2Shell_impactSummaryTableRendersWithData() {
    stubImpactDataEndpoint();
    try {
      navigateToReact2ShellPage();

      React2ShellPage react2Shell = new React2ShellPage();
      React2ShellPageAssertions assertions = new React2ShellPageAssertions(react2Shell);

      assertions.shouldShowPageChrome();
      assertions.shouldShowImpactSummary();
      assertions.shouldShowImpactTableColumns();
      assertions.shouldShowTableRowWithData("TestReact2ShellApp", "CVE-2025-55182");
    }
    finally {
      page.unrouteAll();
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testReact2Shell_emptyTableWhenNoScannedComponents() {
    stubEmptyImpactDataEndpoint();
    try {
      navigateToReact2ShellPage();

      React2ShellPage react2Shell = new React2ShellPage();
      React2ShellPageAssertions assertions = new React2ShellPageAssertions(react2Shell);
      assertions.shouldShowPageChrome();
      assertions.shouldShowEmptyTable();
    }
    finally {
      page.unrouteAll();
    }
  }

  private void stubImpactDataEndpoint() {
    page.route(CVE_AFFECTED_COMPONENTS_ENDPOINT,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(IMPACT_DATA.apiResponse().toString())));
  }

  private void stubEmptyImpactDataEndpoint() {
    page.route(CVE_AFFECTED_COMPONENTS_ENDPOINT,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("{\"pageNumber\":1,\"pageSize\":10,\"totalCount\":0,"
                + "\"aggregates\":{\"totalAffectedApplications\":0,\"affectedComponents\":0,"
                + "\"violatingComponents\":0,\"activeWaivers\":0},\"results\":[]}")));
  }

  private void navigateToReact2ShellPage() {
    playwrightRefreshOrOpen(React2ShellPage.url());
  }
}
