/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.MainHeaderNavigationButton;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.MainHeaderNavigationButton.CLASS_ACTIVE;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.NO_DATA_INFO_TEXT_MONTHLY;

public class SuccessMetricsChartsNavigationTest
    extends AbstractFunctionalTest
{
  @After
  public void after() {
    Selenide.clearBrowserCookies();
  }

  @Test
  public void navigateToSuccessMetricsCharts() {
    tempEntity.newSuccessMetricsReport("admin", "Test Success Metrics",
        JsonUtils.format(new SuccessMetricsReportScopeDTO()));

    refreshOrOpen(IndexPage.url());
    loginAsAdmin();

    SuccessMetricsReportListPage successMetricsPage = new SuccessMetricsReportListPage();
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    MainHeaderNavigationButton labsNavigationButton = MainHeader.labsNavigationButton();
    labsNavigationButton.shouldBe(visible).shouldNotHave(CLASS_ACTIVE).click();
    successMetricsPage.should(appear);
    labsNavigationButton.shouldBe(visible).shouldHave(CLASS_ACTIVE);
    successMetricsPage.reports().shouldHaveSize(1);
    successMetricsPage.report(0).link().click();
    successMetricsChartsPage.should(appear);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT_MONTHLY);
    labsNavigationButton.shouldBe(visible).shouldHave(CLASS_ACTIVE);
    successMetricsChartsPage.backButton().shouldBe(visible).shouldHave(text("Back to Success Metrics")).click();
    successMetricsPage.should(appear);
    successMetricsPage.subheaderDashboardLink().shouldBe(visible).shouldHave(text("Dashboard")).click();
    waitUntilUrl(DashboardPage.url());
  }
}
