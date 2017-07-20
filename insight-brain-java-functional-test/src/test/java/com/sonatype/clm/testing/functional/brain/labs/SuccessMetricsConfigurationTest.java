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
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartsPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsConfigurationPage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SuccessMetricsConfigurationTest
    extends AbstractFunctionalTest
{

  private final static String SUCCESS_METRICS_DISABLED_TEXT =
      "Success metrics have been disabled by your system administrator";

  private final SystemConfigMenu systemConfigMenu = new SystemConfigMenu();

  private final SuccessMetricsConfigurationPage metricsConfigPage = new SuccessMetricsConfigurationPage();

  private SuccessMetricsChartsPage successMetricsChartsPage = new SuccessMetricsChartsPage();

  private RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage =
      new RootOrganizationSuccessMetricsPage();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Test
  public void testSuccessMetricsConfiguration() {
    systemConfigMenu.menu().click();
    systemConfigMenu.successMetrics().parent().shouldNotHave(cssClass("active"));
    systemConfigMenu.successMetrics().shouldBe(visible).shouldHave(text("Success Metrics")).click();
    waitUntilUrl(SuccessMetricsConfigurationPage.URL);

    // check configuration menu entry is selected
    systemConfigMenu.menu().click();
    systemConfigMenu.successMetrics().parent().shouldHave(cssClass("active"));

    // close the menu
    systemConfigMenu.menu().click();
    systemConfigMenu.successMetrics().shouldNotBe(visible);

    // check initial state
    metricsConfigPage.header().shouldHave(text("Success Metrics"));
    metricsConfigPage.explanation().shouldHave(text("Here you can enable or disable Success Metrics."));
    metricsConfigPage.toggle().shouldBe(enabled, checked).shouldHave(text("Enabled"));
    MainHeader.labsNavigationButton().shouldBe(visible);

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
    refreshOrOpen(SuccessMetricsChartsPage.URL);
    waitUntilUrl(SuccessMetricsChartsPage.URL);
    successMetricsChartsPage.rootOrganizationActionItem().shouldNot(exist);
    successMetricsChartsPage.errorBox().shouldBe(visible).shouldHave(text(SUCCESS_METRICS_DISABLED_TEXT));

    // ... and the success metrics details page for root org
    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    waitUntilUrl(RootOrganizationSuccessMetricsPage.URL);
    SummaryStatementTile.root().shouldNot(exist);
    ApplicationCountsTile.root().shouldNot(exist);
    rootOrganizationSuccessMetricsPage.errorBox().shouldBe(visible).shouldHave(text(SUCCESS_METRICS_DISABLED_TEXT));

    // now re-enable success metrics.
    refreshOrOpen(SuccessMetricsConfigurationPage.URL);
    waitUntilUrl(SuccessMetricsConfigurationPage.URL);
    metricsConfigPage.toggle().shouldBe(enabled).shouldNotBe(checked).click();
    metricsConfigPage.update().shouldNotBe(CLM.DISABLED).click();

    // check that it worked on the header,
    MainHeader.labsNavigationButton().should(exist);
    refreshOrOpen(SuccessMetricsChartsPage.URL);
    waitUntilUrl(SuccessMetricsChartsPage.URL);

    // ... the success metrics list page,
    successMetricsChartsPage.errorBox().shouldNotBe(visible);
    successMetricsChartsPage.rootOrganizationActionItem().shouldBe(visible).click();

    // ... and the success metrics details page for root org
    waitUntilUrl(RootOrganizationSuccessMetricsPage.URL);
    rootOrganizationSuccessMetricsPage.noDataInfoPane().shouldBe(visible);
    rootOrganizationSuccessMetricsPage.errorBox().shouldNotBe(visible);
  }
}
