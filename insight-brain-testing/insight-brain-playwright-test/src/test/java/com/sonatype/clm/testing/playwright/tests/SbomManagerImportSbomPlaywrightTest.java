/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPage;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomManagerOwnerSummaryPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the SBOM Manager Import SBOM modal.
 * The Import button is only visible when the selected owner is an application.
 */
public class SbomManagerImportSbomPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private Application seedApp;

  private Organization seedOrg;

  @BeforeEach
  public void seedAndOpenOwnerSummaryAsAdmin() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);

    seedOrg = tempEntity.newOrganization();
    seedApp = tempEntity.newApplication(seedOrg.getId());

    playwrightRefreshOrOpen(SbomApplicationsPage.url());
    playwrightLogin();
    new SbomApplicationsPageAssertions(new SbomApplicationsPage()).shouldBeLoaded();

    String ownerSummaryFragment = "#/sbomManager/management/view/application/" + seedApp.getPublicId();
    playwrightSpaNavigateToHashFragment(ownerSummaryFragment);
    playwrightWaitUntilUrlContains("/management/view/application/");
    // Wait for the Import button to confirm SPA navigation has settled before each test.
    new SbomManagerOwnerSummaryPage().importButton()
        .waitFor(new Locator.WaitForOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /**
   * Import button opens ImportSbomModal with file upload control. "Can Be Automated: No" overridden.
   * Divergence: manual says "Open Actions &gt; Import SBOM"; live UI shows a direct "Import" NxButton.
   */
  @Test
  @Tag("regression")
  public void testImportSbomModal_opensWithFileUploadControl() {
    SbomManagerOwnerSummaryPage summaryPage = new SbomManagerOwnerSummaryPage();

    summaryPage.clickImportButton();

    assertThat(summaryPage.importSbomModal()).isVisible(VISIBLE_OPTS);
    assertThat(summaryPage.importSbomModalHeader())
        .containsText("Import File for Application");
    assertThat(summaryPage.fileUploadInput())
        .isAttached();
  }
}
