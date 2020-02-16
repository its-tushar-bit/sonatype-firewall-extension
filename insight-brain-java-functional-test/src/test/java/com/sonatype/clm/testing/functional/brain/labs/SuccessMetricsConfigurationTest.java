/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsConfigurationPage;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SuccessMetricsConfigurationTest
    extends AbstractFunctionalTest
{
  private static final String SUCCESS_METRICS_DISABLED_TEXT =
      "Success metrics have been disabled by your system administrator";

  private final SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();

  private final SuccessMetricsConfigurationPage metricsConfigPage = new SuccessMetricsConfigurationPage();

  private SuccessMetricsReportListPage successMetricsPage = new SuccessMetricsReportListPage();

  private SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testSuccessMetricsConfiguration() {
    SuccessMetricsReport successMetricsReport = tempEntity.newSuccessMetricsReport("admin", "Test Success Metrics",
        JsonUtils.format(new SuccessMetricsReportScopeDTO()));
    String successMetricsChartsPageUrl = SuccessMetricsReportPage.url(successMetricsReport.getId());

    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.successMetrics().parent().shouldNotHave(cssClass("active"));
    systemConfigMenu.successMetrics().shouldBe(visible).shouldHave(text("Success Metrics")).click();
    waitUntilUrl(SuccessMetricsConfigurationPage.url());

    // check configuration menu entry is selected
    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.successMetrics().parent().shouldHave(cssClass("active"));

    // close the menu
    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.successMetrics().shouldBe(hidden);

    // check initial state
    metricsConfigPage.header().shouldHave(text("Success Metrics"));
    metricsConfigPage.explanation().shouldHave(text("Here you can enable or disable Success Metrics."));
    metricsConfigPage.toggle().shouldBe(enabled, checked).shouldHave(text("Enabled"));
    MainHeader.labsNavigationButton().shouldBe(visible);
    eyesWatcher.eyesCheck();

    // check the tooltip on the update button
    metricsConfigPage.update().shouldBe(CLM.DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("There are no changes to update"));
    metricsConfigPage.cancel().shouldBe(disabled).hover();
    Tooltip.get().shouldNot(exist);

    // check the toggle works
    metricsConfigPage.toggle().click();
    metricsConfigPage.toggle().shouldBe(enabled).shouldNotBe(checked).shouldHave(text("Disabled"));

    // check the cancel button works
    metricsConfigPage.cancel().shouldNotBe(disabled).click();
    metricsConfigPage.toggle().shouldBe(enabled, checked).shouldHave(text("Enabled"));

    // disable success metrics
    metricsConfigPage.toggle().click();
    metricsConfigPage.update().shouldNotBe(CLM.DISABLED).click();
    metricsConfigPage.update().shouldBe(CLM.DISABLED);

    // check that it worked on the header,
    MainHeader.labsNavigationButton().shouldNot(exist);
    // (after refresh, too)
    refresh();
    MainHeader.labsNavigationButton().shouldNot(exist);

    // ... the success metrics list page,
    refreshOrOpen(SuccessMetricsReportListPage.url());
    waitUntilUrl(SuccessMetricsReportListPage.url());
    successMetricsPage.successMetricsChartActionItems().elements().shouldHaveSize(0);
    successMetricsPage.errorBox().shouldBe(visible).shouldHave(text(SUCCESS_METRICS_DISABLED_TEXT));
    successMetricsPage.errorBox().retryButton().shouldBe(hidden);
    successMetricsPage.addSuccessMetricsBtn().shouldBe(hidden);

    // ... and the success metrics details page for root org
    refreshOrOpen(successMetricsChartsPageUrl);
    waitUntilUrl(successMetricsChartsPageUrl);
    SummaryStatementTile.root().shouldNot(exist);
    ApplicationCountsTile.root().shouldNot(exist);
    successMetricsChartsPage.errorBox().shouldBe(visible).shouldHave(text(SUCCESS_METRICS_DISABLED_TEXT));
    successMetricsChartsPage.errorBox().retryButton().shouldBe(hidden);

    // now re-enable success metrics.
    refreshOrOpen(SuccessMetricsConfigurationPage.url());
    waitUntilUrl(SuccessMetricsConfigurationPage.url());
    metricsConfigPage.toggle().shouldBe(enabled).shouldNotBe(checked).click();
    metricsConfigPage.update().shouldNotBe(CLM.DISABLED).click();

    // check that it worked on the header,
    MainHeader.labsNavigationButton().should(exist);
    refreshOrOpen(SuccessMetricsReportListPage.url());
    waitUntilUrl(SuccessMetricsReportListPage.url());

    // ... the success metrics list page,
    successMetricsPage.errorBox().shouldBe(hidden);
    successMetricsPage.successMetricsChartActionItems().elements().shouldHaveSize(1);
    successMetricsPage.successMetricsChartActionItems().element(0).click();

    // ... and the success metrics details page for root org
    waitUntilUrl(successMetricsChartsPageUrl);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible);
    successMetricsChartsPage.errorBox().shouldBe(hidden);
  }
}
