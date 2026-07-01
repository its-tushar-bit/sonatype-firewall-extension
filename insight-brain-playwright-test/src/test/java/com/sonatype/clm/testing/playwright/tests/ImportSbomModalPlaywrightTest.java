/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.net.URL;
import java.nio.file.Paths;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ImportSbomModalPage;
import com.sonatype.clm.testing.playwright.pages.ImportSbomModalPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertNotNull;

/** Import SBOM modal launched from an application's SBOMs tile (ImportSbomModal.jsx). */
public class ImportSbomModalPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "ImportSbomOrg";

  private static final String APP_NAME_PREFIX = "ImportSbomApp";

  private static final String CYCLONEDX_FIXTURE = "/sbom/ImportSbom/cyclonedx-sample.json";

  private static final String SPDX_FIXTURE = "/sbom/ImportSbom/spdx-sample.json";

  private static final String MALFORMED_FIXTURE = "/sbom/ImportSbom/malformed.json";

  private static final String UNSUPPORTED_FIXTURE = "/sbom/ImportSbom/unsupported.exe";

  private Application application;

  private OwnerSummaryPage ownerSummary;

  private ImportSbomModalPage modal;

  private ImportSbomModalPageAssertions modalAssertions;

  @Before
  public void seedAndOpen() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    // The eval pipeline gates on APPLICATION_EVALUATION etc. even though import only needs SBOM_MANAGER.
    setFeatures(LicensedFeature.values());
    testProductLicense.setMaxSbom(50);

    mockHdsForSbomImport();

    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + suffix);
    application = tempEntity.newApplication(
        APP_NAME_PREFIX + "-" + suffix, APP_NAME_PREFIX + "-" + suffix, org.getId());

    // SbomsTile only renders under the SBOM Manager route.
    playwrightRefreshOrOpen(OwnerSummaryPage.sbomManagerAppUrl(application.getPublicId()));
    playwrightLogin();

    ownerSummary = new OwnerSummaryPage();
    modal = new ImportSbomModalPage();
    modalAssertions = new ImportSbomModalPageAssertions(modal);
  }

  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_rendersWithFileInputAndButtons() {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();
    modalAssertions.shouldShowFileUploadAndButtons();
  }

  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_cancelDismissesModal() {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();
    modal.cancelButton().click();
    modalAssertions.shouldBeHidden();
  }

  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_cycloneDxUploadProgressesPastInitialPage() throws Exception {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();

    URL fixture = getClass().getResource(CYCLONEDX_FIXTURE);
    assertNotNull("Fixture missing on classpath: " + CYCLONEDX_FIXTURE, fixture);
    modal.uploadFile(Paths.get(fixture.toURI()));
    modalAssertions.shouldShowSelectedFile("cyclonedx-sample.json");
    modal.importButton().click();
    modalAssertions.shouldShowVersionConfirmPage();
    modal.confirmVersionAndSubmit();
    modalAssertions.shouldShowEvaluationInProgress();
    modalAssertions.shouldShowImportComplete();
  }

  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_spdxUploadImportsSuccessfully() throws Exception {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();

    URL fixture = getClass().getResource(SPDX_FIXTURE);
    assertNotNull("Fixture missing on classpath: " + SPDX_FIXTURE, fixture);
    modal.uploadFile(Paths.get(fixture.toURI()));
    modalAssertions.shouldShowSelectedFile("spdx-sample.json");
    modal.importButton().click();
    modalAssertions.shouldShowVersionConfirmPage();
    modal.confirmVersionAndSubmit();
    modalAssertions.shouldShowEvaluationInProgress();
    modalAssertions.shouldShowImportComplete();
  }

  /**
   * Upload endpoint accepts arbitrary bytes — malformed content uploads successfully and the
   * modal advances to VersionConfirmPage; rejection happens later during evaluation, not at
   * upload. None of the current fixtures trigger upload-time rejection.
   */
  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_malformedFileTransitionsPastUploadPage() throws Exception {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();

    URL fixture = getClass().getResource(MALFORMED_FIXTURE);
    assertNotNull("Fixture missing on classpath: " + MALFORMED_FIXTURE, fixture);
    modal.uploadFile(Paths.get(fixture.toURI()));
    modal.importButton().click();

    modalAssertions.shouldTransitionPastUploadPage();
  }

  /** NxFileUpload has no client-side allowlist — .exe is accepted at the dialog; rejection is server-side. */
  @Test
  @Category(RegressionTest.class)
  public void testImportSbomModal_unsupportedExtensionAcceptedClientSide() throws Exception {
    ownerSummary.importSbomButton().click();
    modalAssertions.shouldBeVisible();

    URL fixture = getClass().getResource(UNSUPPORTED_FIXTURE);
    assertNotNull("Fixture missing on classpath: " + UNSUPPORTED_FIXTURE, fixture);
    modal.uploadFile(Paths.get(fixture.toURI()));

    modalAssertions.shouldShowSelectedFile("unsupported.exe");
  }
}
