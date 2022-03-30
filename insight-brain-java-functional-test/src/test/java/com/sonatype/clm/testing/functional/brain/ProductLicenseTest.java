/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.apache.tools.ant.util.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ProductLicenseTest
    extends AbstractFunctionalTest
{
  private static final String FINGERPRINT_PATTERN = "[0-9a-fA-F]+";

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ProductLicensePage.url());
    loginAsAdmin();
  }

  @Before
  public void before() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ProductLicenseDetails.PRODUCT_FIREWALL,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    setExpirationDate(DateUtils.parseIso8601Date("2100-05-01"));
    refreshOrOpen(ProductLicensePage.url());
  }

  @Test
  public void testGuidelinesNotVisibleWhenLicenseInstalled() {
    ProductLicensePage.licenseInstallGuideline().shouldNotBe(visible);
    ProductLicensePage.licenseProxyGuideline().shouldNotBe(visible);
  }

  @Test
  public void testGuidelinesVisibleWhenLicenseNotInstalled() {
    uninstallLicense();
    refreshOrOpen(ProductLicensePage.url());
    ProductLicensePage.licenseInstallGuideline().shouldBe(visible);
    ProductLicensePage.licenseProxyGuideline().shouldBe(visible);
  }

  @Test
  public void testLicenseInformation() {
    ProductLicensePage.expiryDate().shouldBe(visible).should(matchText("[a-zA-Z]+ [0-9]+, 2[0-9]{3}"));
    ProductLicensePage.daysToExpiration().shouldBe(visible).shouldHave(matchText("[0-9]+"));
    ProductLicensePage.contactName().shouldBe(visible).shouldHave(text("Billy"));
    ProductLicensePage.contactCompany().shouldBe(visible).shouldHave(text("Acme"));
    ProductLicensePage.contactEmail().shouldBe(visible).shouldHave(text("billy@example.com"));
    ProductLicensePage.licensedDevelopers().shouldBe(visible);
    ProductLicensePage.licensedApplications().shouldNotBe(visible);

    ProductLicensePage.licensedDevelopersRows()
        .shouldHave(texts("Lifecycle — 50", "Lifecycle Cloud — 50", "Firewall — 45"));
    ProductLicensePage.licensedApplications().shouldBe(hidden);
    ProductLicensePage.products().shouldHave(texts("Nexus Lifecycle Cloud", "Nexus Lifecycle", "Nexus Firewall"));
    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText(FINGERPRINT_PATTERN));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testLicenseInformation_Auditor() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldNotBe(visible);
    ProductLicensePage.licensedApplications().shouldBe(visible).shouldHave(text("100 (0 in use)"));
    ProductLicensePage.products().shouldHave(texts("Nexus Auditor"));

    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "Auditor"));
    eyesWatcher.eyesCheck("Nexus Auditor Logo");
  }

  @Test
  public void testLicenseInformation_FirewallOnly() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("45"));
    ProductLicensePage.licensedApplications().shouldNotBe(visible);
    ProductLicensePage.products().shouldHave(texts("Nexus Firewall"));

    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "Firewall"));
    eyesWatcher.eyesCheck("Nexus Firewall Logo");
  }

  @Test
  public void testLicenseInformation_LifecycleOnly() throws Exception {
    productLicenseManager.setMaxFirewallUsers(null);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("50"));
    ProductLicensePage.licensedApplications().shouldNotBe(visible);
    ProductLicensePage.products().shouldHave(texts("Nexus Lifecycle"));

    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "Lifecycle"));
    eyesWatcher.eyesCheck("Nexus Lifecycle Logo");
  }

  @Test
  public void testLicenseInformation_LifecycleCloudOnly() throws Exception {
    productLicenseManager.setMaxFirewallUsers(null);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("50"));
    ProductLicensePage.licensedApplications().shouldNotBe(visible);
    ProductLicensePage.products().shouldHave(texts("Nexus Lifecycle Cloud"));

    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "Lifecycle Cloud"));
    eyesWatcher.eyesCheck("Nexus Lifecycle Logo");
  }

  @Test
  public void testLicenseInformation_LifecycleFoundationOnly() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("50"));
    ProductLicensePage.licensedApplications().shouldNotBe(visible);
    ProductLicensePage.products().shouldHave(texts("Nexus Lifecycle Foundation"));

    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "Lifecycle Foundation"));
  }

  @Test
  public void testLicenseInformation_NexusPlus() throws Exception {
    productLicenseManager.setMaxFirewallUsers(null);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);

    refreshOrOpen(ProductLicensePage.url());

    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("50"));
    ProductLicensePage.licensedApplications().shouldNotBe(visible);
    ProductLicensePage.products().shouldHave(texts("Nexus Pro+"));
  }

  @Test
  public void testUpdateLicense() {
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Update License"));

    uploadMockLicense();

    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText(FINGERPRINT_PATTERN));
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Update License"));
  }

  @Test
  public void testUninstallAndInstallLicense() throws Exception {
    ProductLicensePage.uninstallLicenseBtn().shouldBe(visible).shouldHave(text("Uninstall License"));
    ProductLicensePage.uninstallLicenseBtn().click();

    ProductLicensePage.ProductLicenseUninstallModal uninstallModal =
        new ProductLicensePage.ProductLicenseUninstallModal();

    uninstallModal.shouldBe(visible);
    uninstallModal.uninstallBtn().shouldBe(visible);

    // simulate dead network by shutting off the server
    testCLMServer.stop();
    uninstallModal.uninstallBtn().click();
    uninstallModal.errorMessage().shouldBe(visible);
    uninstallModal.retryBtn().shouldBe(visible);
    staticTempEntity.cleanupAllPersistedUserSessions();
    // restart the server and log back in
    testCLMServer.start();
    beforeClass();

    // continue license uninstall
    ProductLicensePage.uninstallLicenseBtn().click();
    uninstallModal.shouldBe(visible);
    uninstallModal.errorMessage().shouldNotBe(visible);
    uninstallModal.retryBtn().shouldNotBe(visible);
    uninstallModal.shouldBe(visible);
    uninstallModal.uninstallBtn().shouldBe(visible).click();

    FormMask.seeAndWaitForDismissal();

    ProductLicensePage.fingerprint().shouldBe(hidden);
    ProductLicensePage.uninstallLicenseBtn().shouldBe(hidden);
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Install License"));

    eyesWatcher.eyesCheck("Sonatype Logo");

    uploadMockLicense();

    // should redirect to Getting Started page after fresh license install
    new GettingStartedPage().shouldBe(visible);

    refreshOrOpen(ProductLicensePage.url());
    refreshOrOpen(ProductLicensePage.url());
    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText(FINGERPRINT_PATTERN));
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Update License"));
  }

  private void uploadMockLicense() {
    // NOTE: the contents of the license file don't matter for this test because the MockProductLicenseManager ignores
    // it anyway
    ProductLicensePage.installLicenseFileUpload()
        .uploadFromClasspath("com/sonatype/clm/testing/functional/brain/ProductLicenseTest/mockLicense");

    ProductLicensePage.ProductLicenseEulaModal eulaModal = new ProductLicensePage.ProductLicenseEulaModal();
    eulaModal.shouldBe(visible);
    eulaModal.header().shouldHave(text("End User License Agreement"));
    eulaModal.eula().shouldHave(text("READ THIS AGREEMENT CAREFULLY"));
    eulaModal.acceptBtn().shouldBe(visible).click();

    FormMask.seeAndWaitForDismissal();
  }
}
