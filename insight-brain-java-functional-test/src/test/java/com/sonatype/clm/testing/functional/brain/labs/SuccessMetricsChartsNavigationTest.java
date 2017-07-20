/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.MainHeaderNavigationButton;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartsPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.IQ_DISABLED;
import static com.sonatype.clm.testing.functional.elements.MainHeaderNavigationButton.CLASS_ACTIVE;
import static com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.ApplicationCountsTile;
import static com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.MttrTile;
import static com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.SummaryStatementTile;
import static com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.NO_DATA_INFO_TEXT;

public class SuccessMetricsChartsNavigationTest
    extends AbstractFunctionalTest
{

  @After
  public void startup() {
    logout();
  }

  @Test
  public void navigateToRootOrgSuccessMetricsCharts() {
    refreshOrOpen(BaseUrl.uriBuilder().build().toString());
    loginAsAdmin();

    SuccessMetricsChartsPage successMetricsChartsPage = new SuccessMetricsChartsPage();
    RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

    MainHeaderNavigationButton labsNavigationButton = MainHeader.labsNavigationButton();
    labsNavigationButton.shouldBe(visible).shouldNotHave(CLASS_ACTIVE).click();
    successMetricsChartsPage.should(appear);
    labsNavigationButton.shouldBe(visible).shouldHave(CLASS_ACTIVE);
    successMetricsChartsPage.rootOrganizationActionItem().shouldBe(visible).click();
    rootOrganizationSuccessMetricsPage.should(appear);
    rootOrganizationSuccessMetricsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT);
    labsNavigationButton.shouldBe(visible).shouldHave(CLASS_ACTIVE);
    rootOrganizationSuccessMetricsPage.backButton().shouldBe(visible).shouldHave(text("Back to Success Metrics"))
        .click();
    successMetricsChartsPage.should(appear);
  }

  @Test
  public void navigateToSuccessMetrics_noRootOrgConfigured() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    refreshOrOpen(BaseUrl.uriBuilder().build().toString());
    loginAsAdmin();

    SuccessMetricsChartsPage successMetricsPage = new SuccessMetricsChartsPage();

    MainHeader.labsNavigationButton().shouldBe(visible).click();
    successMetricsPage.should(appear);

    successMetricsPage.noRootOrgWarning().shouldBe(visible);

    successMetricsPage.rootOrganizationActionItem().shouldBe(visible).shouldHave(IQ_DISABLED).click();
    new RootOrganizationSuccessMetricsPage().shouldNot(appear);
    successMetricsPage.shouldBe(visible);
  }

  @Test
  public void navigateToRootOrgSuccessMetricsCharts_noRootOrgConfigured() {
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    loginAsAdmin();

    new RootOrganizationSuccessMetricsPage().noRootOrgError().shouldBe(visible);
    SummaryStatementTile.root().shouldNotBe(visible);
    ApplicationCountsTile.root().shouldNotBe(visible);
    MttrTile.root().shouldNotBe(visible);
  }
}
