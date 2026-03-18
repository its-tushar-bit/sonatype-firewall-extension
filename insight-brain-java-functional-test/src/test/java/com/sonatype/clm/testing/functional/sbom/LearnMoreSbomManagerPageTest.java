/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.clm.testing.functional.pages.sbom.LearnMoreSbomManagerPage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;

public class LearnMoreSbomManagerPageTest
    extends AbstractFunctionalTest
{
  @After
  public void after() {
    logout();
  }

  @Test
  public void testLearnMoreSbomManagerPage_RendersSuccessfully() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    refreshOrOpen(LearnMoreSbomManagerPage.url());
    LearnMoreSbomManagerPage learnMoreSbomManagerPage = new LearnMoreSbomManagerPage();
    learnMoreSbomManagerPage.infoAlert()
        .shouldHave(text("SBOM Manager is currently not enabled for your " +
            "organization. Learn more about SBOM Manager."));
    learnMoreSbomManagerPage.infoLink()
        .shouldHave(attribute("href", "http://links.sonatype.com/products/sbom-manager-learn-more"));
  }

  @Test
  public void testPageRedirect_RedirectsToLearnMoreSbomManagerWhenNoSbomManagerPermission() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    refreshOrOpen(SbomManagerDashboardPage.url());
    waitUntilUrl(LearnMoreSbomManagerPage.url());
    LearnMoreSbomManagerPage learnMoreSbomManagerPage = new LearnMoreSbomManagerPage();
    learnMoreSbomManagerPage.infoAlert()
        .shouldHave(text("SBOM Manager is currently not enabled for your " +
            "organization. Learn more about SBOM Manager."));
  }

  @Test
  public void testPageRedirect_RedirectsToSbomManagerDashboardWhenThereIsSbomManagerPermission() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    refreshOrOpen(LearnMoreSbomManagerPage.url());
    waitUntilUrl(SbomManagerDashboardPage.url());
    SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    sbomManagerDashboardPage.title().shouldHave(text("SBOM Manager Dashboard"));
  }
}
