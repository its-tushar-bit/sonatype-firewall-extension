/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.File;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxProgressBar;
import com.sonatype.clm.testing.functional.elements.NxToast;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.sbom.ImportSbomModal;
import com.sonatype.clm.testing.functional.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerApplicationSummaryPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class SbomManagerApplicationSummaryPageImportSbomModalTest
    extends AbstractFunctionalTest
{
  private static SbomManagerApplicationSummaryPage sbomSummaryPage;

  private Organization organization;

  private Application application;

  private String testFilesPath;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

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
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SUCCESS_METRICS, LicensedFeature.POLICY_MONITORING,
        LicensedFeature.APPLICATION_EVALUATION);

    testCLMServer.getHdsServer()
        .respondWith("{\"scanId\": \"SCAN-ID\", \"timeToReport\": 0}")
        .atUri("rest/application/analysis");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/ImportSbomModalTest/valid-bom-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID");

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
    NxProgressBar.seeProgressBarAndWaitForDismissal();
    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible).shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // sbom versions table is updated
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("9.1.1"));

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
    importSbomModal.summaryApplicationName()
        .shouldHave(text("Test Application"));
    importSbomModal.summaryVersionId()
        .shouldHave(text("9.1.1"));
    importSbomModal.summaryTotalComponents()
        .shouldHave(text("2"));
    importSbomModal.summaryTotalVulnerabilities()
        .shouldHave(text("0"));

    importSbomModal.viewSbomButton()
        .shouldBe(visible)
        .click();

    // switch to new tab
    Selenide.switchTo().window(1);

    // sbom version summary page
    waitUntilUrl(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), "9.1.1"));
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
    NxProgressBar.seeProgressBarAndWaitForDismissal();

    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible);
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
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
    NxProgressBar.seeProgressBarAndWaitForDismissal();

    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible);
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
  }

  @Test
  public void testImportSbomModal_fileUploadFail_ignorableValidationErrorButImportAnyway() {
    assertThat(thirdPartySbomMetadataDAO.getAll()).isEmpty();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "invalid-bom-ignorable-error.json");

    importSbomModal.title().shouldHave(text("Import File for Application " + application.getName()));
    importSbomModal.fileUpload().shouldBe(visible).uploadFile(file);
    importSbomModal.fileSelected().shouldBe(visible).shouldHave(text("invalid-bom-ignorable-error.json"));
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

    assertThat(thirdPartySbomMetadataDAO.getAll()).hasSize(1);

    importSbomModal.skipValidationCheckbox().shouldBe(visible).shouldNotBe(selected);
    importSbomModal.skipValidationCheckbox().click();

    importSbomModal.importSbomButton().shouldBe(visible, enabled).shouldHave(Condition.text("Import Anyway"));
    importSbomModal.importSbomButton().click();

    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible);
    assertThat(thirdPartySbomMetadataDAO.getAll()).hasSize(1);
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
    importSbomModal.summaryApplicationName()
        .shouldHave(text(application.getName()));
    importSbomModal.summaryVersionId()
        .shouldHave(value(""));
    importSbomModal.summaryTotalComponents()
        .shouldHave(text("3"));
    importSbomModal.summaryTotalVulnerabilities()
        .shouldHave(text("0"));
    importSbomModal.cancelCloseButton()
        .shouldBe(visible)
        .click();
    importSbomModal.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_fileUploadFail_nonIgnorableValidationError() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "invalid-bom-non-ignorable-error.json");

    importSbomModal.title().shouldHave(text("Import File for Application " + application.getName()));
    importSbomModal.fileUpload().shouldBe(visible).uploadFile(file);
    importSbomModal.fileSelected().shouldBe(visible).shouldHave(text("invalid-bom-non-ignorable-error.json"));
    importSbomModal.cancelCloseButton().shouldBe(visible);

    importSbomModal.importSbomButton().shouldBe(visible, enabled).click();

    importSbomModal.title().shouldHave(text("Your SBOM failed validation"));
    importSbomModal.fileUpload().shouldNotBe(visible);
    importSbomModal.fileSelected().shouldNotBe(visible);
    importSbomModal.errorAlert().shouldBe(visible);
    importSbomModal.warnAlert().shouldNotBe(visible);
    importSbomModal.copyToClipboardButton().shouldBe(visible);
    String expectedErrors = "• Error: Missing document namespace";
    importSbomModal.validationErrors().shouldBe(visible).shouldHave(text(expectedErrors));

    // Since we are disabling via css class we also check that when clicking it does nothing
    importSbomModal.importSbomButton()
        .shouldBe(visible)
        .shouldHave(Condition.cssClass("disabled"))
        .shouldHave(Condition.text("Import"))
        .click();
    importSbomModal.shouldBe(visible);
    importSbomModal.cancelCloseButton().shouldBe(visible).click();
    importSbomModal.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_versionOverride() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    importSbomModal.shouldBe(visible);

    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // version confirmation page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput()
        .shouldBe(visible)
        .shouldHave(value("9.1.1"))
        .setValue("2.0.0"); // override version
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
    importSbomModal.summaryApplicationName()
        .shouldHave(text("Test Application"));
    importSbomModal.summaryVersionId()
        .shouldHave(text("2.0.0"));
    importSbomModal.summaryTotalComponents()
        .shouldHave(text("2"));
    importSbomModal.summaryTotalVulnerabilities()
        .shouldHave(text("0"));

    ThirdPartySbomMetadata sbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(), "2.0.0");
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getSbomVersion()).isEqualTo("2.0.0");
    assertThat(sbomMetadata.getApplicationId()).isEqualTo(application.getId());

    sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(), "9.1.1");
    assertThat(sbomMetadata).isNull();
  }

  @Test
  public void testImportSbomModal_versionConflict_validSbom() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput()
        .shouldBe(visible)
        .shouldHave(value("9.1.1"))
        .setValue("1.2.3");
    importSbomModal.importSbomButton().click();

    importSbomModal.cancelCloseButton().click();
    importSbomModal.shouldNotBe(visible);

    sbomsTile.importButton().click();
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput()
        .shouldBe(visible)
        .shouldHave(value("9.1.1"))
        .setValue("1.2.3"); // use existing version
    importSbomModal.importSbomButton().click();

    importSbomModal.errorAlert().shouldHave(text("Version 1.2.3 already exists"));
    importSbomModal.versionInput().shouldBe(visible);
    importSbomModal.importSbomButton().shouldBe(enabled);

    testCLMServer.getHdsServer()
        .respondWith("{\"scanId\": \"SCAN-ID2\", \"timeToReport\": 0}")
        .atUri("rest/application/analysis");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/ImportSbomModalTest/valid-bom-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID2");

    importSbomModal.versionInput().setValue("1.2.4");
    importSbomModal.importSbomButton().click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
    importSbomModal.summaryApplicationName()
        .shouldHave(text("Test Application"));
    importSbomModal.summaryVersionId()
        .shouldHave(text("1.2.4"));
  }

  @Test
  public void testImportSbomModal_versionConflict_invalidSbom() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    File file = new File(testFilesPath + "invalid-bom-ignorable-error.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    importSbomModal.skipValidationCheckbox().click();
    importSbomModal.importSbomButton().click();

    importSbomModal.versionInput()
        .shouldBe(visible)
        .setValue("1.2.3");
    importSbomModal.importSbomButton().click();

    importSbomModal.cancelCloseButton().click();
    importSbomModal.shouldNotBe(visible);

    sbomsTile.importButton().click();
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    importSbomModal.skipValidationCheckbox().click();
    importSbomModal.importSbomButton().click();

    importSbomModal.versionInput()
        .shouldBe(visible)
        .setValue("1.2.3"); // use existing version
    importSbomModal.importSbomButton().click();

    importSbomModal.errorAlert().shouldHave(text("Version 1.2.3 already exists"));
    importSbomModal.versionInput().shouldBe(visible);
    importSbomModal.importSbomButton().shouldBe(enabled);

    testCLMServer.getHdsServer()
        .respondWith("{\"scanId\": \"SCAN-ID2\", \"timeToReport\": 0}")
        .atUri("rest/application/analysis");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/ImportSbomModalTest/valid-bom-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID2");

    importSbomModal.versionInput().setValue("1.2.4");
    importSbomModal.importSbomButton().click();

    // evaluation in progress page
    importSbomModal.seeEvaluationInProgressPageAndWaitForDismissal();

    // evaluation complete page
    importSbomModal.seeEvaluationCompletePageAndWaitForDismissal();

    // summary page
    importSbomModal.title().should(appear).shouldHave(text("Import Complete"));
    importSbomModal.summaryApplicationName()
        .shouldHave(text("Test Application"));
    importSbomModal.summaryVersionId()
        .shouldHave(text("1.2.4"));
  }

  @Test
  public void testImportSbomModal_importSuccessfulResultIsShownInToast() {
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
    NxProgressBar.seeProgressBarAndWaitForDismissal();

    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible).shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();

    // result toast
    NxToast toast = new NxToast("success");
    toast.shouldBe(visible);
    toast.shouldHave(text("SBOM 9.1.1 from application test-application is now ready for review in the SBOM table."));

    // sbom versions table is updated
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("9.1.1"));

    // dismissing the toast
    toast.closeButton().shouldBe(enabled).click();
    toast.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_importErrorResultIsShownInToast() {
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
    NxProgressBar.seeProgressBarAndWaitForDismissal();

    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible).shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/ImportSbomModalTest/invalid-bom-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID");

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();

    // result toast
    NxToast toast = new NxToast("error");
    toast.shouldBe(visible);
    toast.shouldHave(text("SBOM 9.1.1 evaluation from application test-application failed:"));

    // sbom versions table is not updated
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldNotHave(text("9.1.1"));

    // dismissing the toast
    toast.closeButton().shouldBe(enabled).click();
    toast.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_multipleImportResultsAreShownAsToasts() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    // file upload page
    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    // version confirm page
    importSbomModal.versionInput()
        .shouldBe(visible)
        .shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().click();

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();
    importSbomModal.shouldNotBe(visible);

    // file upload page
    sbomsTile.importButton().click();
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    // version confirm page
    importSbomModal.versionInput()
        .shouldBe(visible)
        .setValue("1.2.3"); // use another version
    importSbomModal.importSbomButton().click();

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();
    importSbomModal.shouldNotBe(visible);

    // result toast for version 9.1.1
    NxToast toast911 = new NxToast();
    toast911.shouldBe(visible);
    toast911.shouldHave(text("SBOM 9.1.1"));

    // result toast for version 1.2.3
    NxToast toast123 = new NxToast();
    toast123.shouldBe(visible);
    toast911.shouldHave(text("SBOM 1.2.3"));
  }

  @Test
  public void testImportSbomModal_importResultToastShownInOtherUIRoute() {
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
    NxProgressBar.seeProgressBarAndWaitForDismissal();

    // version confirm page
    importSbomModal.title().shouldHave(text("File Uploaded. Import in Progress…"));
    importSbomModal.versionInput().shouldBe(visible).shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().shouldBe(enabled).click();

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();

    // navigate to dashboard
    SidebarNavigation.sbomManagerDashboardNavigationButton().click();

    // result toast
    NxToast toast = new NxToast("success");
    toast.shouldBe(visible);
    toast.shouldHave(text("SBOM 9.1.1 from application test-application is now ready for review in the SBOM table."));

    // dismissing the toast
    toast.closeButton().shouldBe(enabled).click();
    toast.shouldNotBe(visible);
  }

  @Test
  public void testImportSbomModal_resultToastDoesNotAffectModal() {
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.importButton().click();
    ImportSbomModal importSbomModal = sbomSummaryPage.importSbomModal();

    // file upload page
    File file = new File(testFilesPath + "valid-bom.json");
    importSbomModal.fileUpload().uploadFile(file);
    importSbomModal.importSbomButton().click();

    // version confirm page
    importSbomModal.versionInput()
        .shouldBe(visible)
        .shouldHave(value("9.1.1"));
    importSbomModal.importSbomButton().click();

    // evaluation in progress page is closed
    importSbomModal.cancelCloseButton().shouldBe(enabled).click();
    importSbomModal.shouldNotBe(visible);

    // file upload page
    sbomsTile.importButton().click();
    importSbomModal.fileUpload().uploadFile(file);

    // wait for result toast for version 9.1.1 without closing the modal
    NxToast toast911 = new NxToast();
    toast911.shouldBe(visible);
    toast911.shouldHave(text("SBOM 9.1.1"));

    // file upload page should be the same
    importSbomModal.fileSelected()
        .shouldBe(visible)
        .shouldHave(text("valid-bom.json"));
  }
}
