/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.brain.EmailConfigurationPageTest;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerEmailConfigurationPage;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerEmailConfigurationPageTest
    extends EmailConfigurationPageTest
{
  private final SbomManagerEmailConfigurationPage emailConfigurationPage = new SbomManagerEmailConfigurationPage();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.NOTIFICATIONS);
    refreshOrOpen(IndexPage.url());
  }

  @Test
  @Override
  public void testUnsavedChangesModal() {
    refreshOrOpen(SbomManagerEmailConfigurationPage.url());
    emailConfigurationPage.hostName().setValue("localhost");
    emailConfigurationPage.port().setValue("465");

    testUnsavedChangesModal_Cancel();
    testUnsavedChangesModal_Continue();
  }

  private void testUnsavedChangesModal_Cancel() {
    SbomManagerDashboardPage sbomDashboardPage = new SbomManagerDashboardPage();
    refreshOrOpen(SbomManagerDashboardPage.url());

    sbomDashboardPage.container().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    sbomDashboardPage.container().shouldNotBe(visible);

    emailConfigurationPage.title().shouldBe(visible).shouldHave(text("Email"));
  }

  private void testUnsavedChangesModal_Continue() {
    SbomManagerDashboardPage sbomDashboardPage = new SbomManagerDashboardPage();
    refreshOrOpen(SbomManagerDashboardPage.url());

    sbomDashboardPage.container().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    sbomDashboardPage.container().shouldBe(visible);
  }
}
