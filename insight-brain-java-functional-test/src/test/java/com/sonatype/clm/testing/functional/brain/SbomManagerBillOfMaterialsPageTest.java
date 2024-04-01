/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class SbomManagerBillOfMaterialsPageTest
    extends AbstractFunctionalTest
{
  private final SbomManagerBillOfMaterialsPage sbomManagerBillOfMaterialsPage = new SbomManagerBillOfMaterialsPage();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url("mockAppId", "mockVersionId"));
    loginAsAdmin();
  }

  @Test
  public void testFeatureDisabled_Error() {
    setMissingFeature(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url("mockAppId", "mockVersionId"));
    sbomManagerBillOfMaterialsPage.pageTitle().shouldNotBe(visible);
    sbomManagerBillOfMaterialsPage.sbomManagerNotEnabledError().shouldBe(visible);
    eyesWatcher.eyesCheck("Sbom Manager Bill of Materials page not enabled");
  }
}
