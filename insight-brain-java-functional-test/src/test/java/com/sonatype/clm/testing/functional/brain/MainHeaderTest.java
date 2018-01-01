/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Properties;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class MainHeaderTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(ReportListPage.URL);
  }

  @Test
  public void testLoggedInUserName() {
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testProductVersion() throws Exception {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/com/sonatype/insight/brain/version/version.properties"));
    String productVersion = clmLicenseManager.getLicenseSummary().productEdition + " " + props.get("version");
    MainHeader.productVersion().shouldHave(text(productVersion));
  }

  @Test
  public void testDashboardIcon_DashboardLicensed() {
    MainHeader.dashboardNavigationButton().shouldBe(visible);
  }

  @Test
  public void testDashboardIcon_DashboardNotLicensed() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    MainHeader.policiesNavigationButton().shouldBe(visible);
    MainHeader.dashboardNavigationButton().shouldNotBe(visible);
  }

  @Test
  public void testDefaultPage_DashboardLicensed() {
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(DashboardPage.URL);
  }

  @Test
  public void testDefaultPage_DashboardNotLicensed() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(ReportListPage.URL);
  }
}
