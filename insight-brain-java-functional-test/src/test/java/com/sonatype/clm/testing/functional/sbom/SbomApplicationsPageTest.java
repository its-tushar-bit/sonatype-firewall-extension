/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomApplicationsPage;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class SbomApplicationsPageTest
    extends AbstractFunctionalTest
{
  private static SbomApplicationsPage sbomApplicationsPage;

  @BeforeClass
  public static void beforeClass() {
    sbomApplicationsPage = new SbomApplicationsPage();
  }

  @Before
  public void beforeEachMethod() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testApplicationsPageContent() {
    refreshOrOpen(sbomApplicationsPage.url());

    sbomApplicationsPage.title().shouldHave(text("Applications"));
  }
}
