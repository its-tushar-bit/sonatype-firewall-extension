/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.io.File;
import com.sonatype.clm.testing.functional.elements.NxToast;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.ImportSbomModal;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerApplicationSummaryPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import static com.codeborne.selenide.Condition.*;

public class SbomManagerApplicationSummaryPageImportSbomModalTest
        extends AbstractMtiqFunctionalTest
{
  private final SbomManagerApplicationSummaryPage sbomSummaryPage = new SbomManagerApplicationSummaryPage();

  private Organization organization;

  private Application application;

  private String testFilesPath;

  @Before
  public void init() throws Exception {
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());
    testFilesPath = "src/test/resources/SbomManagerApplicationSummaryPageTest/ImportSbomModalTest/";

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testImportSbomModal_RenderSuccessful() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.shouldBe(visible);
    sbomsTile.importButton().shouldBe(visible);
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();
    importSbomModal.shouldBe(visible);
    importSbomModal.title().shouldHave(text("Import SBOM for Application Test Application"));
  }

  @Test
  public void testImportSbomModal_CancelCloseButton() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    importSbomModal.cancelCloseButton().shouldBe(visible).click();
    importSbomModal.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_fileUploadAndCommitSuccessful() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.fileSelected()
        .shouldBe(visible)
        .shouldHave(text("valid-bom.json"));
    importSbomModal.importSbomButton()
        .shouldBe(enabled)
        .click();

    importSbomModal.progressBar().shouldBe(visible);
    importSbomModal.summaryApplicationName()
        .shouldHave(text("Test Application"));
    importSbomModal.summaryInputVersionId()
        .shouldHave(value("9.1.1"));
    importSbomModal.summaryTotalComponents()
        .shouldHave(text("2"));
    importSbomModal.summaryTotalVulnerabilities()
        .shouldHave(text("0"));
  }

  @Test
  public void testImportSbomModal_fileUploadFail() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "invalid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().shouldBe(enabled).click();
    importSbomModal.errorAlert().shouldBe(visible);
    importSbomModal.fileSelected().shouldBe(visible).shouldHave(text("invalid-bom.json"));
  }

  @Test
  public void testImportSbomModal_toastMessage() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.fileSelected().shouldBe(visible).shouldHave(text("valid-bom.json"));
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    importSbomModal.progressBar().shouldBe(visible);
    importSbomModal.summaryApplicationName()
            .shouldHave(text("Test Application"));
    importSbomModal.summaryInputVersionId()
            .shouldHave(value("9.1.1"));
    importSbomModal.summaryTotalComponents()
            .shouldHave(text("2"));
    importSbomModal.summaryTotalVulnerabilities()
            .shouldHave(text("0"));
    importSbomModal.cancelCloseButton().shouldBe(visible).click();
    importSbomModal.shouldNotBe(visible);

    NxToast toast = new NxToast("info");

    toast.shouldBe(visible);
    toast.shouldHave(text(
        "SBOM is currently being evaluated and will be available in the SBOM table shortly." +
                " Please refresh the page after few minutes to see newly imported SBOM."
    ));
  }
}

