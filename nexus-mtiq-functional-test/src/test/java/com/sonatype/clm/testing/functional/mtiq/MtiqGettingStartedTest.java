/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.codeborne.selenide.Selenide;
import com.sonatype.clm.testing.functional.mtiq.pages.MtiqGettingStartedPage;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.mtiq.pages.MtiqGettingStartedPage.LearningTopicsSummaryTile;
import static com.sonatype.clm.testing.functional.mtiq.pages.MtiqGettingStartedPage.ProductLicenseSummaryTile;
import static com.sonatype.clm.testing.functional.mtiq.pages.MtiqGettingStartedPage.SystemSetupSummaryTile;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;

public class MtiqGettingStartedTest
    extends AbstractMtiqFunctionalTest
{
  @Test
  public void testMtiqGettingStartedPage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ProductLicenseDetails.PRODUCT_FIREWALL,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    refreshOrOpen(MtiqGettingStartedPage.url());

    createUser();
    login();
    MtiqGettingStartedPage mtiqGettingStartedPage = new MtiqGettingStartedPage();
    testCLMServer.getHdsServer().respondWith("").andStatus(503).atUri("ping");
    refreshOrOpen(MtiqGettingStartedPage.url());

    Selenide.sleep(1000);
    // non-admin user only sees the HDS connectivity warning and learning topics tile
    mtiqGettingStartedPage.hdsConnectivityWarning().shouldBe(visible);
    mtiqGettingStartedPage.productLicenseSummary().shouldNotBe(visible);
    mtiqGettingStartedPage.systemSetupSummary().shouldNotBe(visible);
    scrollIntoView(mtiqGettingStartedPage.learningTopicsSummary().getElement()).shouldBe(visible);
    checkMtiqNonAdminOmissions();

    grantPermissions(getUsername(), GLOBAL_CONTEXT_ID, Permission.ADD_APPLICATION);
    refresh();

    Selenide.sleep(1000);

    // non-admin user that can add applications sees the HDS connectivity, system setup and learning topics tiles
    mtiqGettingStartedPage.hdsConnectivityWarning().shouldBe(visible);
    mtiqGettingStartedPage.productLicenseSummary().shouldNotBe(visible);
    mtiqGettingStartedPage.systemSetupSummary().shouldBe(visible);
    scrollIntoView(mtiqGettingStartedPage.learningTopicsSummary().getElement()).shouldBe(visible);
    checkMtiqAdminOmissions();

    grantPermissions(getUsername(), GLOBAL_CONTEXT_ID, Permission.CONFIGURE_SYSTEM);
    refresh();

    Selenide.sleep(1000);

    // non-default admin user sees all tiles
    mtiqGettingStartedPage.hdsConnectivityWarning().shouldBe(visible);
    mtiqGettingStartedPage.productLicenseSummary().shouldBe(visible);
    checkLicenseSummaryContent();
    mtiqGettingStartedPage.systemSetupSummary().shouldBe(visible);
    eyesWatcher.eyesCheck("Non-default admin user");
    scrollIntoView(mtiqGettingStartedPage.learningTopicsSummary().getElement()).shouldBe(visible);
    checkMtiqAdminOmissions();

    testCLMServer.getHdsServer().respondWith("alive").atUri("ping");
    refresh();

    Selenide.sleep(1000);

    // just check that the HDS connectivity warning is gone
    mtiqGettingStartedPage.hdsConnectivityWarning().shouldNotBe(visible);
  }

  private void checkLicenseSummaryContent() {
    ProductLicenseSummaryTile licenseTile = new MtiqGettingStartedPage().productLicenseSummary();

    licenseTile.expiryDate().shouldBe(visible).should(matchText("[a-zA-Z]+ [0-9]+, 2[0-9]{3}"));
    licenseTile.daysToExpiration().shouldBe(visible).shouldHave(matchText("[0-1]"));
    licenseTile.products()
        .shouldHave(texts("Sonatype Lifecycle Cloud", "Sonatype Developer", "Sonatype Lifecycle",
            "Sonatype Repository Firewall"));
    // NOTE: the emdashes are added in CSS and apparently don't show up here
    licenseTile.licensedDevelopersRows().shouldHave(texts("Lifecycle50", "Firewall45"));
  }

  private void checkMtiqNonAdminOmissions() {
    LearningTopicsSummaryTile topicsTile = new MtiqGettingStartedPage().learningTopicsSummary();
    ProductLicenseSummaryTile licenseTile = new MtiqGettingStartedPage().productLicenseSummary();

    licenseTile.fingerprint().shouldNotBe(visible);
    topicsTile.sectionTopics().shouldHave(texts("Policies", "Hierarchy And Inheritance", "Dashboard"));
  }

  // MTIQ omissions for MTIQ when admin or non-admin users who can add applications
  private void checkMtiqAdminOmissions() {
    LearningTopicsSummaryTile topicsTile = new MtiqGettingStartedPage().learningTopicsSummary();
    ProductLicenseSummaryTile licenseTile = new MtiqGettingStartedPage().productLicenseSummary();
    SystemSetupSummaryTile setupTile = new MtiqGettingStartedPage().systemSetupSummary();

    licenseTile.fingerprint().shouldNotBe(visible);
    setupTile.setupSections().shouldHave(texts("Adding Users", "Onboarding Applications"));
    setupTile.addingUsersTopics().shouldHave(exactText("INVITE USERS"));
    topicsTile.sectionTopics().shouldHave(texts("Policies", "Hierarchy And Inheritance", "Dashboard"));
  }
}
