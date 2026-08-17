/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DeveloperRiskTablePage;
import com.sonatype.clm.testing.playwright.pages.DeveloperRiskTablePageAssertions;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Developer Dashboard's "Build Stage Risk Monitoring Summary" risk
 * table, gated on the {@code DEVELOPER_SUMMARY_TABLE} feature flag (captured/restored per-test).
 */
public class DeveloperRiskTablePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String MGMT_VIEW_APP_URL_FRAGMENT = "/management/view/application/";

  private boolean originalSummaryTableEnabled;

  private Organization org;

  private Application app;

  private String appName;

  @BeforeEach
  public void seedAppAndOpenDashboard() {
    originalSummaryTableEnabled = SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.isEnabled();
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.setEnabled(true);
    org = tempEntity.newOrganization("risk-" + TemporaryEntity.uuid());
    appName = "risk-app-" + TemporaryEntity.uuid();
    app = tempEntity.newApplication(appName, appName, org.getId());
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();
    new DeveloperRiskTablePageAssertions(new DeveloperRiskTablePage()).shouldBeVisible();
  }

  @AfterEach
  public void restoreFeatureFlag() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.setEnabled(originalSummaryTableEnabled);
  }

  @Test
  @Tag("regression")
  public void testRiskTable_applicationNameLinkOpensApplicationManagement() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    assertions.shouldShowRowForApp(appName);
    Locator row = table.rowByAppName(appName);
    assertions.shouldShowApplicationLinkInRow(row, MGMT_VIEW_APP_URL_FRAGMENT + app.getPublicId());
  }

  @Test
  @Tag("regression")
  public void testRiskTable_cicdConfigureButtonShownWhenIntegrationDisabled() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    Locator row = table.rowByAppName(appName);
    assertions.shouldShowCiCdConfigureButtonInRow(row);
  }

  @Test
  @Tag("regression")
  public void testRiskTable_scmConfigureButtonShownWhenSourceControlDisabled() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    Locator row = table.rowByAppName(appName);
    assertions.shouldShowScmConfigureButtonInRow(row);
  }

  @Test
  @Tag("regression")
  public void testRiskTable_dateColumnsShowNoneWhenNoCommitOrEvaluation() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    Locator row = table.rowByAppName(appName);
    assertions.shouldShowNoneInDateColumnsForRow(row);
  }

  @Test
  @Tag("regression")
  public void testRiskTable_prioritiesColumnShowsNAWhenNoReportAvailable() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    Locator row = table.rowByAppName(appName);
    assertions.shouldShowNAInPrioritiesColumnForRow(row);
  }

  @Test
  @Tag("regression")
  public void testRiskTable_searchByUnknownNameShowsEmptyState() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    table.searchInput().fill("nonexistent-app-name-" + TemporaryEntity.uuid());

    assertions.shouldShowEmptyState();
  }

  @Test
  @Tag("regression")
  public void testRiskTable_filterToggleOpensAndApplyClosesPopover() {
    DeveloperRiskTablePage table = new DeveloperRiskTablePage();
    DeveloperRiskTablePageAssertions assertions = new DeveloperRiskTablePageAssertions(table);

    assertions.shouldNotShowFilterPopover();

    table.filterToggleButton().click();
    assertions.shouldShowFilterPopover();
    assertions.shouldShowFilterPopoverFieldsets();

    table.filterApplyButton().click();
    assertions.shouldNotShowFilterPopover();
  }
}
