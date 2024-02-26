/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SbomManagerDashboardPage;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class SbomManagerDashboardPageTest extends AbstractFunctionalTest
{
  private final SbomManagerDashboardPage sbomManagerPage = new SbomManagerDashboardPage();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFeatureEnabled_Success() {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(SbomManagerDashboardPage.url());
    sbomManagerPage.dashboard().shouldBe(visible);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testFeatureDisabled_Error() {
    setMissingFeature(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(SbomManagerDashboardPage.url());
    sbomManagerPage.sbomManagerNotEnabledError().shouldBe(visible);
  }
}
