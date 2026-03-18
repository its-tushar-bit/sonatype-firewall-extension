/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AttributionReportFormPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;

public class AttributionReportFormTest
    extends AbstractFunctionalTest
{
  private static Application app;

  private final String emptyValueValidationErrorMsg = "Report Title cannot be empty";

  private static final String UNSAVED_CHANGES_PROMPT =
      "This template has unsaved changes. Are you sure you want to continue? All unsaved changes will be lost.";

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    app = tempEntity.newApplicationWithParent(AttributionReportFormTest.class.getSimpleName(), "app", "org");
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474", componentId);
    tempEntity.createNewAttributionReportTemplate(
        "Template 1",
        "Report Title 1",
        "Header 1",
        "Footer 1",
        false,
        false,
        false,
        true,
        false);
    tempEntity.createNewAttributionReportTemplate(
        "Template 2",
        "Report Title 2",
        "Header 2",
        "Footer 2",
        true,
        false,
        false,
        false,
        true);
  }

  @Test
  public void testAttributionReportFormCheckBoxes() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));

    WebElementCondition valueTrue = value("true");
    WebElementCondition valueFalse = value("false");

    attrReportFormPage.getTableOfContentsHiddenInput().shouldHave(valueTrue);
    attrReportFormPage.getTableOfContentsCheck().click();
    attrReportFormPage.getTableOfContentsHiddenInput().shouldHave(valueFalse);

    attrReportFormPage.getAppendixHiddenInput().shouldHave(valueTrue);
    attrReportFormPage.getAppendixCheck().click();
    attrReportFormPage.getAppendixHiddenInput().shouldHave(valueFalse);

    attrReportFormPage.getIncludeStandardLicenseTextsHiddenInput().shouldHave(valueTrue);
    attrReportFormPage.getIncludeStandardLicenseTextsCheck().click();
    attrReportFormPage.getIncludeStandardLicenseTextsHiddenInput().shouldHave(valueFalse);
  }

  @Test
  public void testTextInputEmptyValidation() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));

    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    SelenideElement titleInput = attrReportFormPage.getTitleInput();
    titleInput.setValue("a");
    titleInput.sendKeys(Keys.BACK_SPACE);
    titleInput.parent()
        .parent()
        .$(".nx-field-validation-message")
        .shouldHave(text(emptyValueValidationErrorMsg));
  }

  @Test
  public void testFormValidation() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    SelenideElement titleInput = attrReportFormPage.getTitleInput();
    SelenideElement submitButton = attrReportFormPage.getFormSubmitBtn();

    titleInput.setValue("a");
    titleInput.sendKeys(Keys.BACK_SPACE);

    submitButton.click();
    FormUtils.getAlertElement(attrReportFormPage)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Report Title cannot be empty"));
  }

  private void testFilesUpload(AttributionReportFormPage attrReportFormPage, boolean testDelete) throws IOException {
    Path file1 = Files.createTempFile("file1", ".txt");
    file1.toFile().deleteOnExit();
    Path file2 = Files.createTempFile("file2", ".txt");
    file2.toFile().deleteOnExit();
    Path file3 = Files.createTempFile("file3", ".txt");
    file3.toFile().deleteOnExit();

    WebElementCondition innerText1 = text(file1.getFileName().toString());
    WebElementCondition innerText2 = text(file2.getFileName().toString());
    WebElementCondition innerText3 = text(file3.getFileName().toString());

    SelenideElement deleteFileModalConfirmationButton = attrReportFormPage.getDeleteFileModalConfirmationButton();

    attrReportFormPage.getFileInputs().get(0).uploadFile(file1.toFile());
    SelenideElement firstListItem = attrReportFormPage.getUploadedFileListItems().get(0);
    SelenideElement firstDeleteFileButtonItem = attrReportFormPage.getUploadedFilesListItemButton().get(0);
    firstListItem.has(innerText1);
    attrReportFormPage.getFileInputs().shouldHave(size(2));

    attrReportFormPage.getFileInputs().get(1).uploadFile(file2.toFile());
    attrReportFormPage.getUploadedFileListItems().get(1).has(innerText2);
    attrReportFormPage.getFileInputs().shouldHave(size(3));

    attrReportFormPage.getFileInputs().get(2).uploadFile(file3.toFile());
    attrReportFormPage.getUploadedFileListItems().get(2).has(innerText3);
    attrReportFormPage.getFileInputs().shouldHave(size(4));

    if (testDelete) {
      firstDeleteFileButtonItem.click();
      deleteFileModalConfirmationButton.click();
      attrReportFormPage.getFileInputs().shouldHave(size(3));
      attrReportFormPage.getUploadedFileListItems().shouldHave(size(2));
      firstListItem.has(innerText2);

      firstDeleteFileButtonItem.click();
      deleteFileModalConfirmationButton.click();
      attrReportFormPage.getFileInputs().shouldHave(size(2));
      attrReportFormPage.getUploadedFileListItems().shouldHave(size(1));
      firstListItem.has(innerText3);

      firstDeleteFileButtonItem.click();
      deleteFileModalConfirmationButton.click();
      attrReportFormPage.getFileInputs().shouldHave(size(1));
      attrReportFormPage.getUploadedFileListItems().shouldHave(size(0));
    }
  }

  @Test
  public void testAddAndRemoveNoticeFiles() throws IOException {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    this.testFilesUpload(attrReportFormPage, true);
  }

  @Test
  public void testAddNoticeFilesAndGoBack() throws IOException {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    Path file1 = Files.createTempFile("file1", ".txt");
    file1.toFile().deleteOnExit();
    WebElementCondition innerText1 = text(file1.getFileName().toString());
    attrReportFormPage.getFileInputs().get(0).uploadFile(file1.toFile());
    SelenideElement firstListItem = attrReportFormPage.getUploadedFileListItems().get(0);
    firstListItem.has(innerText1);
    MainHeader.backButton().click();
    UnsavedModal unsavedModal = new UnsavedModal();
    unsavedModal.getElement().should(exist);
  }

  private void triggerFormSubmit(AttributionReportFormPage attrReportFormPage) {
    Wait<WebDriver> wait = new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(5))
        .ignoring(NoSuchElementException.class);

    attrReportFormPage.getFormSubmitBtn().click();

    wait.until(ExpectedConditions.numberOfWindowsToBe(2));
    switchTo().window(1);
    Assert.assertTrue(getWebDriver().getCurrentUrl()
        .matches(".*licenseLegalMetadata/application/AttributionReportFormTest/stage/build/report"));
  }

  @Test
  public void testFormSubmit() throws IOException {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    String headerText = "My Header";
    String footerText = "My Footer";

    attrReportFormPage.getHeaderInput().setValue(headerText);
    attrReportFormPage.getFooterInput().setValue(footerText);
    attrReportFormPage.getTableOfContentsCheck().click();
    Path file1 = Files.createTempFile("file1", ".txt");
    file1.toFile().deleteOnExit();
    attrReportFormPage.getFileInputs().get(0).uploadFile(file1.toFile());
    SelenideElement firstListItem = attrReportFormPage.getUploadedFileListItems().get(0);
    firstListItem.has(text("file1"));
    eyesWatcher.eyesCheck();
    this.triggerFormSubmit(attrReportFormPage);
  }

  @Test
  public void testFormEditAndGoBack() throws IOException {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    String headerText = "My Header";
    String footerText = "My Footer";

    attrReportFormPage.getHeaderInput().setValue(headerText);
    attrReportFormPage.getFooterInput().setValue(footerText);
    attrReportFormPage.getTableOfContentsCheck().click();
    Path file1 = Files.createTempFile("file1", ".txt");
    file1.toFile().deleteOnExit();
    attrReportFormPage.getFileInputs().get(0).uploadFile(file1.toFile());
    SelenideElement firstListItem = attrReportFormPage.getUploadedFileListItems().get(0);
    firstListItem.has(text("file1"));
    MainHeader.backButton().click();
    UnsavedModal unsavedModal = new UnsavedModal();
    unsavedModal.getElement().should(exist);
  }

  private void testCustomValuesToTemplate(AttributionReportFormPage attrReportFormPage) {
    attrReportFormPage.getTemplatesDropdown().click();
    attrReportFormPage.getTemplatesDropdownItems().first().click();
    attrReportFormPage.getTitleInput().shouldHave(value("Report Title 1"));
    attrReportFormPage.getHeaderInput().shouldHave(value("Header 1"));
    attrReportFormPage.getFooterInput().shouldHave(value("Footer 1"));
    attrReportFormPage.getTableOfContentsHiddenInput().shouldHave(value("false"));
    attrReportFormPage.getIncludeStandardLicenseTextsHiddenInput().shouldHave(value("false"));
    attrReportFormPage.getAppendixHiddenInput().shouldHave(value("false"));
    attrReportFormPage.getIncludeInnerSourceCheckboxInput().shouldHave(value("true"));
    attrReportFormPage.getIncludeSonatypeSpecialLicensesCheckboxInput().shouldHave(value("false"));
  }

  @Test
  public void testTemplateChange() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    this.testCustomValuesToTemplate(attrReportFormPage);
  }

  @Test
  public void saveUneditedTemplate() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    this.testCustomValuesToTemplate(attrReportFormPage);
    this.testUnsavedChangesModal();
  }

  @Test
  public void testUnsavedChangesModal() {
    AttributionReportFormPage.UnsavedChangesDialog unsavedChangesDialog =
        new AttributionReportFormPage.UnsavedChangesDialog();
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    attrReportFormPage.getTitleInput().setValue("Another text");
    attrReportFormPage.getTemplatesDropdown().click();
    attrReportFormPage.getTemplatesDropdownItems().first().click();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));

    unsavedChangesDialog.cancelButton().click();
    attrReportFormPage.getTemplatesDropdown().click();

    attrReportFormPage.getTemplatesDropdownItems().first().click();
    unsavedChangesDialog.continueButton().click();

    attrReportFormPage.getTitleInput().shouldHave(value("Report Title 1"));
  }

  @Test
  public void testEditedTemplates() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    attrReportFormPage.getTitleInput().shouldHave(value("Attribution Report for AttributionReportFormTest"));
    attrReportFormPage.getHeaderInput().shouldHave(value(""));
    attrReportFormPage.getFooterInput().shouldHave(value(""));
    attrReportFormPage.getTableOfContentsHiddenInput().shouldHave(value("true"));
    attrReportFormPage.getIncludeStandardLicenseTextsHiddenInput().shouldHave(value("true"));
    attrReportFormPage.getAppendixHiddenInput().shouldHave(value("true"));
    attrReportFormPage.getIncludeInnerSourceCheckboxInput().shouldHave(value("false"));
    attrReportFormPage.getIncludeSonatypeSpecialLicensesCheckboxInput().shouldHave(value("false"));
    this.testCustomValuesToTemplate(attrReportFormPage);
  }

  @Test
  public void testFormSubmitWithEditedTemplateData() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    this.testCustomValuesToTemplate(attrReportFormPage);
    this.triggerFormSubmit(attrReportFormPage);
  }

  @Test
  public void testReplaceEditedTemplateDataWithOtherTemplateData() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage.UnsavedChangesDialog unsavedChangesDialog =
        new AttributionReportFormPage.UnsavedChangesDialog();
    this.testCustomValuesToTemplate(attrReportFormPage);

    // selecting the same template should replace the existing changes with the default template values
    attrReportFormPage.getTemplatesDropdown().click();
    attrReportFormPage.getTemplatesDropdownItems().first().click();
    unsavedChangesDialog.continueButton().click();
    attrReportFormPage.getTitleInput().shouldHave(value("Report Title 1"));
    attrReportFormPage.getTemplatesDropdown().shouldHave(text("Template 1"));

    attrReportFormPage.getTitleInput().setValue("Another text");
    attrReportFormPage.getTemplatesDropdown().shouldHave(text("Template 1 (edited)"));

    // selecting the another template should replace the existing changes with the default template values
    attrReportFormPage.getTemplatesDropdown().click();
    attrReportFormPage.getTemplatesDropdownItems().get(1).click();
    unsavedChangesDialog.continueButton().click();
    attrReportFormPage.getTitleInput().shouldHave(value("Report Title 2"));
    attrReportFormPage.getTemplatesDropdown().shouldHave(text("Template 2"));
  }

  @Test
  public void testAppendixCheckBoxState() {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    attrReportFormPage.getIncludeStandardLicenseTextsCheck().click();
    attrReportFormPage.getAppendixHiddenInput().shouldHave(value("false"));
    attrReportFormPage.getAppendixNativeCheck().shouldBe(disabled);
    attrReportFormPage.getIncludeStandardLicenseTextsCheck().click();
    attrReportFormPage.getAppendixHiddenInput().shouldHave(value("false"));
    attrReportFormPage.getAppendixNativeCheck().shouldNotBe(disabled);
  }

  @Test
  public void testReportSubmitWithFilesAndTemplateData() throws IOException {
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));

    this.testCustomValuesToTemplate(attrReportFormPage);
    this.testFilesUpload(attrReportFormPage, false);
    this.triggerFormSubmit(attrReportFormPage);
  }
}
