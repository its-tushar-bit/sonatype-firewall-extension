/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;

import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

/**
 * This class tests the various situations in which the page load and login might happen, including the license not
 * being installed, access to pages that don't require authentication, etc
 */
public class PageLoadTest
    extends AbstractFunctionalTest
{
  @Before
  public void setup() {
    // ensure we are not logged in
    hardreset();
  }

  @Test
  public void testIndexHtml() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    waitUntilUrl(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testLoadUnauthenticatedPage() {
    refreshOrOpen(VulnerabilitySearchPage.url());

    LoginModal loginModal = new LoginModal();
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    // ensure that logging in after going straight to an unauth page works
    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible);
    loginModal.vulnerabilityLookupText().shouldNotBe(visible);
    vulnPage.shouldBe(visible);
    loginAsAdmin();

    // wait a bit to ensure that the page isn't redirecting somewhere else (like the dashboard)
    Selenide.sleep(1000);
    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoadIndexHtml_NoLicense() {
    uninstallLicense();

    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    waitUntilUrl(ProductLicensePage.url());
    ProductLicensePage.installLicenseBtn().shouldBe(visible);

    refreshOrOpen(DashboardPage.url());
    Selenide.sleep(1000);

    // no effect - navigating to other pages not allowed
    ProductLicensePage.installLicenseBtn().shouldBe(visible);

    refreshOrOpen(VulnerabilitySearchPage.url());
    Selenide.sleep(1000);

    // no effect - navigating to unauthenticated pages not allowed either
    ProductLicensePage.installLicenseBtn().shouldBe(visible);
  }

}
