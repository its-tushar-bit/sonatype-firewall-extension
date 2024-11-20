/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.File;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxToast;
import com.sonatype.clm.testing.functional.elements.sbom.ImportSbomModal;
import com.sonatype.clm.testing.functional.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerApplicationSummaryPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.license.model.LicensedFeature.POLICY_MONITORING;

public class SbomManagerApplicationSummaryPageImportSbomModalTest
    extends AbstractFunctionalTest
{
  private static SbomManagerApplicationSummaryPage sbomSummaryPage;

  private Organization organization;

  private Application application;

  private String testFilesPath;

  @BeforeClass
  public static void beforeClass() {
    sbomSummaryPage = new SbomManagerApplicationSummaryPage();
    Selenide.open("/#");
    loginAsAdmin();
  }

  @Before
  public void init() throws Exception {
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());
    testFilesPath = "src/test/resources/ImportSbomModalTest/";

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SUCCESS_METRICS, POLICY_MONITORING);

    refreshOrOpen(IndexPage.url());
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
    importSbomModal.title().shouldHave(text("Import File for Application " + application.getName()));
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
  public void testImportSbomModal_fileUploadAndCommitSuccessful_SBOM() {
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

    importSbomModal.title().shouldHave(text("Import in progress…"));
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
  public void testImportSbomModal_fileUploadAndCommitSuccessful_BINARY() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "xml-apis-1.4.01.jar");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.fileSelected()
        .shouldBe(visible)
        .shouldHave(text("xml-apis-1.4.01.jar"));
    importSbomModal.importSbomButton()
        .shouldBe(enabled)
        .click();

    importSbomModal.title().shouldHave(text("Import in progress…"));
    importSbomModal.progressBar().shouldBe(visible);
    importSbomModal.binaryFilename().shouldBe(visible).shouldHave(text("xml-apis-1.4.01.jar"));
    importSbomModal.summaryApplicationName().shouldBe(visible).shouldHave(text(application.getName()));
  }

  @Test
  public void testImportSbomModal_fileUploadFail_shouldBeTreatedAsBinary() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "text.txt");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.fileSelected()
        .shouldBe(visible)
        .shouldHave(text("text.txt"));
    importSbomModal.importSbomButton()
        .shouldBe(enabled)
        .click();

    importSbomModal.title().shouldHave(text("Import in progress…"));
    importSbomModal.binaryFilename().shouldBe(visible).shouldHave(text("text.txt"));
    importSbomModal.summaryApplicationName().shouldBe(visible).shouldHave(text(application.getName()));
  }

  @Test
  public void testImportSbomModal_fileUploadFail_validationError() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "invalid-bom.json");

    importSbomModal.title().shouldHave(text("Import File for Application " + application.getName()));
    importSbomModal.fileUpload().shouldBe(visible).uploadFile(file);
    importSbomModal.fileSelected().shouldBe(visible).shouldHave(text("invalid-bom.json"));
    importSbomModal.cancelCloseButton().shouldBe(visible);

    importSbomModal.importSbomButton().shouldBe(visible, enabled).click();

    importSbomModal.title().shouldHave(text("Your SBOM failed validation"));
    importSbomModal.fileUpload().shouldNotBe(visible);
    importSbomModal.fileSelected().shouldNotBe(visible);
    importSbomModal.errorAlert().shouldNotBe(visible);
    importSbomModal.warnAlert().shouldBe(visible);
    importSbomModal.copyToClipboardButton().shouldBe(visible);
    String expectedErrors = """
        • Line: 21, Column: 20, Path: $.components[2].bom-ref, Error: must be at least 1 characters long
        • Line: 6, Column: 18, Path: $.components, Error: must have only unique items in the array""";
    importSbomModal.validationErrors().shouldBe(visible).shouldHave(text(expectedErrors));
    importSbomModal.importSbomButton().shouldBe(visible, disabled);
    importSbomModal.cancelCloseButton().shouldBe(visible);
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
        "The file you uploaded is currently being evaluated and will be available on this page shortly. " +
            "Please refresh the page after few minutes to see it."
    ));
  }
}
