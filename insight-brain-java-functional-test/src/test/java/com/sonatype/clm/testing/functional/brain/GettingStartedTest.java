/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.HelpMenu;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage.ProductLicenseSummaryTile;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;

public class GettingStartedTest
    extends AbstractFunctionalTest
{
  @After
  public void after() {
    logout();
  }

  @Test
  public void testNavigation() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
    HelpMenu help = MainHeader.helpMenu();

    help.dropdownToggle().shouldBe(visible).click();
    help.gettingStartedLink().shouldBe(visible).click();
    waitUntilUrl(GettingStartedPage.URL);
    new GettingStartedPage().shouldBe(visible);
  }

  @Test
  public void testGettingStartedPage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    testCLMServer.getHdsServer().setResponseForURI("ping", "alive", 200);
    refreshOrOpen(GettingStartedPage.URL);
    loginAsAdmin();
    GettingStartedPage gettingStartedPage = new GettingStartedPage();

    // default admin user sees all tiles
    gettingStartedPage.hdsConnectivityWarning().shouldNotBe(visible);
    gettingStartedPage.productLicenseSummary().shouldBe(visible);
    checkLicenseSummaryContent();
    gettingStartedPage.systemSetup().shouldBe(visible);
    eyesWatcher.eyesCheck("Default user - top of the page");
    scrollIntoView(gettingStartedPage.learningTopics(), false).shouldBe(visible);
    eyesWatcher.eyesCheck("Default user - bottom of the page");

    logout();
    createUser();
    login();
    testCLMServer.getHdsServer().setResponseForURI("ping", "", 503);
    refreshOrOpen(GettingStartedPage.URL);

    // non-admin user only sees the HDS connectivity warning and learning topics tile
    gettingStartedPage.hdsConnectivityWarning().shouldBe(visible).shouldHave(text("retry in a bit"));
    gettingStartedPage.productLicenseSummary().shouldNotBe(visible);
    gettingStartedPage.systemSetup().shouldNotBe(visible);
    scrollIntoView(gettingStartedPage.learningTopics()).shouldBe(visible);
    eyesWatcher.eyesCheck("Non-admin user");

    grantPermissions(getUsername(), GLOBAL_CONTEXT_ID, Permission.ADD_APPLICATION);
    refresh();

    // non-admin user that can add applications sees the HDS connectivity, system setup and learning topics tiles
    gettingStartedPage.hdsConnectivityWarning().shouldBe(visible);
    gettingStartedPage.productLicenseSummary().shouldNotBe(visible);
    gettingStartedPage.systemSetup().shouldBe(visible);
    scrollIntoView(gettingStartedPage.learningTopics()).shouldBe(visible);

    grantPermissions(getUsername(), GLOBAL_CONTEXT_ID, Permission.CONFIGURE_SYSTEM);
    refresh();

    // non-default admin user sees all tiles
    gettingStartedPage.hdsConnectivityWarning().shouldBe(visible);
    gettingStartedPage.productLicenseSummary().shouldBe(visible);
    checkLicenseSummaryContent();
    gettingStartedPage.systemSetup().shouldBe(visible);
    eyesWatcher.eyesCheck("Non-default admin user");
    scrollIntoView(gettingStartedPage.learningTopics()).shouldBe(visible);

    testCLMServer.getHdsServer().setResponseForURI("ping", "alive", 200);
    refresh();

    // just check that the HDS connectivity warning is gone
    gettingStartedPage.hdsConnectivityWarning().shouldNotBe(visible);
  }

  private void checkLicenseSummaryContent() {
    ProductLicenseSummaryTile licenseTile = new GettingStartedPage().productLicenseSummary();

    licenseTile.expiryDate().shouldBe(visible).should(matchText("[a-zA-Z]+ [0-9]+, 2[0-9]{3}"));
    licenseTile.daysToExpiration().shouldBe(visible).shouldHave(matchText("[0-1]"));
    licenseTile.products().shouldHave(texts("Nexus Lifecycle", "Nexus Firewall"));
    // NOTE: the emdashes are added in CSS and apparently don't show up here
    licenseTile.licensedDevelopersRows().shouldHave(texts("Lifecycle50", "Firewall45"));
    licenseTile.fingerprint().shouldBe(visible).should(matchText("1234"));
  }
}
