/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal.application;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ManageTemplatesPage;
import com.sonatype.clm.testing.functional.pages.ManageTemplatesPage.DeleteDialog;
import com.sonatype.clm.testing.functional.pages.ManageTemplatesPage.TemplateList;
import com.sonatype.clm.testing.functional.pages.ManageTemplatesPage.UnsavedChangesDialog;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;

public class AttributionReportManageTemplateTest
    extends AbstractFunctionalTest
{
  private static final String UNSAVED_CHANGES_PROMPT =
      "This template has unsaved changes. Are you sure you want to continue? All unsaved changes will be lost.";

  private static final String DELETE_PROMPT = "You are about to delete \"%s\". This action cannot be undone.";

  private static final String TEMPLATE_EXISTS_MSG = "Template name already exists";

  private static final String EMPTY_REPORT_TITLE_MSG = "Report Title cannot be empty";

  private static final String EMPTY_TEMPLATE_NAME_MSG = "Template Name cannot be empty";

  private static final Condition SELECTED_CLASS = cssClass("selected");

  private static Application app;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    app = tempEntity.newApplicationWithParent(AttributionReportManageTemplateTest.class.getSimpleName(), "app", "org");
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474", componentId);
  }

  @Test
  public void testCreateTemplate() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    ManageTemplatesPage.formTitle().shouldHave(text("Create Template"));
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.items().shouldHave(size(0));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    templateList.items().shouldHave(texts("Template 1"));
  }

  @Test
  public void testCreateMultipleTemplates() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateAndSaveTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.items().shouldHave(texts("Template 1", "Template 2"));
    templateList.itemAt(1).click();
    verifyFormData("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    eyesWatcher.eyesCheck("Attribution Report Template Management - Create multiple templates");
  }

  @Test
  public void testSwitchToAnotherTemplateAndCancel() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(1).click();
    final UnsavedChangesDialog unsavedChangesDialog = ManageTemplatesPage.unsavedChangesDialog();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));
    unsavedChangesDialog.cancelButton().click();
    verifyFormData("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    templateList.itemAt(1).shouldNotHave(SELECTED_CLASS);
  }

  @Test
  public void testSwitchToAnotherTemplateAndOverwrite() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(1).click();
    final UnsavedChangesDialog unsavedChangesDialog = ManageTemplatesPage.unsavedChangesDialog();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));
    unsavedChangesDialog.continueButton().click();
    verifyFormData("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    templateList.itemAt(1).shouldHave(SELECTED_CLASS);
  }

  @Test
  public void testEditExistingTemplateThenSwitch_expectPrompt() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateAndSaveTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(2).click();

    final UnsavedChangesDialog unsavedChangesDialog = ManageTemplatesPage.unsavedChangesDialog();

    unsavedChangesDialog.shouldNot(exist);
    ManageTemplatesPage.reportTitleInput().setValue("Modified Template 2");
    templateList.itemAt(1).click();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));
  }

  @Test
  public void testEditExistingTemplateThenCreateNew_expectPrompt() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateAndSaveTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(2).click();

    final UnsavedChangesDialog unsavedChangesDialog = ManageTemplatesPage.unsavedChangesDialog();
    unsavedChangesDialog.shouldNot(exist);
    ManageTemplatesPage.reportTitleInput().setValue("Modified Template 2");
    ManageTemplatesPage.createNewTemplateButton().click();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));
  }

  @Test
  public void testDeleteExistingTemplateConfirm() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateAndSaveTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(1).click();
    ManageTemplatesPage.deleteTemplateButton().click();
    final DeleteDialog deleteDialog = ManageTemplatesPage.deleteDialog();
    deleteDialog.should(exist);
    deleteDialog.shouldHave(text(String.format(DELETE_PROMPT, "Template 1")));
    deleteDialog.deleteButton().click();
    templateList.items().shouldHave(size(1));
    templateList.items().shouldHave(texts("Template 2"));
  }

  @Test
  public void testDeleteExistingTemplateCancel() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateAndSaveTemplate("Template 2", "Report 2", "Header 2", "Footer 2", false, true, true);
    final TemplateList templateList = ManageTemplatesPage.templateList();
    templateList.itemAt(1).click();
    ManageTemplatesPage.deleteTemplateButton().click();
    final DeleteDialog deleteDialog = ManageTemplatesPage.deleteDialog();
    deleteDialog.should(exist);
    deleteDialog.shouldHave(text(String.format(DELETE_PROMPT, "Template 1")));
    deleteDialog.cancelButton().click();
    templateList.items().shouldHave(size(2));
    templateList.items().shouldHave(texts("Template 1", "Template 2"));
  }

  @Test
  public void testSaveDuplicateTemplateName() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateTemplate("Template 1", "Report 2", "Header 2", "Footer 2", false, true, true);
    ManageTemplatesPage.templateNameErrorPrompt().shouldHave(text(TEMPLATE_EXISTS_MSG));
    ManageTemplatesPage.saveTemplateButton().has(cssClass(".disabled.nx-btn"));
  }

  @Test
  public void testSaveEmptyReportTitle() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateTemplate("Template 2", "   ", "Header 2", "Footer 2", false, true, true);
    ManageTemplatesPage.reportTitleErrorPrompt().shouldHave(text(EMPTY_REPORT_TITLE_MSG));
    ManageTemplatesPage.saveTemplateButton().has(cssClass(".disabled.nx-btn"));
  }

  @Test
  public void testSaveEmptyTemplateName() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateAndSaveTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    populateTemplate("   ", "Report 2", "Header 2", "Footer 2", false, true, true);
    ManageTemplatesPage.templateNameErrorPrompt().shouldHave(text(EMPTY_TEMPLATE_NAME_MSG));
    ManageTemplatesPage.saveTemplateButton().has(cssClass(".disabled.nx-btn"));
  }

  @Test
  public void testDiscardingUnsavedChangesShouldPrompt() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    populateTemplate("Template 1", "Report 1", "Header 1", "Footer 1", true, false, false);
    ManageTemplatesPage.createNewTemplateButton().click();
    final UnsavedChangesDialog unsavedChangesDialog = ManageTemplatesPage.unsavedChangesDialog();
    unsavedChangesDialog.should(exist);
    unsavedChangesDialog.shouldHave(text(UNSAVED_CHANGES_PROMPT));
  }

  private void verifyFormData(
      final String templateName,
      final String reportTitle,
      final String header,
      final String footer,
      final boolean tableOfContents,
      final boolean standardLicenses,
      final boolean appendix)
  {
    ManageTemplatesPage.templateNameInput().shouldHave(value(templateName));
    ManageTemplatesPage.reportTitleInput().shouldHave(value(reportTitle));
    ManageTemplatesPage.documentHeaderInput().shouldHave(value(header));
    ManageTemplatesPage.documentFooterInput().shouldHave(value(footer));
    verifyCheckBox(ManageTemplatesPage.tableOfContentsCheckbox(), tableOfContents);
    verifyCheckBox(ManageTemplatesPage.standardLicenseTextsCheckbox(), standardLicenses);
    verifyCheckBox(ManageTemplatesPage.appendixCheckbox(), appendix);
  }

  private void verifyCheckBox(final SelenideElement checkBox, final boolean expectedValue) {
    if (expectedValue) {
      checkBox.shouldBe(checked);
    }
    else {
      checkBox.shouldNotBe(checked);
    }
  }

  private void populateAndSaveTemplate(
      final String templateName,
      final String reportTitle,
      final String header,
      final String footer,
      final boolean tableOfContents,
      final boolean standardLicenses,
      final boolean appendix)
  {
    populateTemplate(templateName, reportTitle, header, footer, tableOfContents, standardLicenses, appendix);
    ManageTemplatesPage.saveTemplateButton().click();
  }

  private void populateTemplate(
      final String templateName,
      final String reportTitle,
      final String header,
      final String footer,
      final boolean tableOfContents,
      final boolean standardLicenses,
      final boolean appendix)
  {
    ManageTemplatesPage.templateNameInput().setValue(templateName);
    ManageTemplatesPage.reportTitleInput().setValue(reportTitle);
    ManageTemplatesPage.documentHeaderInput().setValue(header);
    ManageTemplatesPage.documentFooterInput().setValue(footer);
    setCheckBox(ManageTemplatesPage.tableOfContentsCheckbox(), tableOfContents);
    setCheckBox(ManageTemplatesPage.standardLicenseTextsCheckbox(), standardLicenses);
    setCheckBox(ManageTemplatesPage.appendixCheckbox(), appendix);
  }

  private void setCheckBox(final SelenideElement checkBox, final boolean value) {
    if (checkBox.isSelected() == value) { // skip if the actual state equals the desired one
      return;
    }
    checkBox.parent().click();
  }

  @Test
  public void testAppendixCheckBoxState() {
    refreshOrOpen(ManageTemplatesPage.urlToApplicationScope(app.getPublicId(), Stage.ID_BUILD));
    ManageTemplatesPage.standardLicenseTextsCheckbox().parent().click();
    ManageTemplatesPage.standardLicenseTextsCheckbox().shouldNotBe(checked);
    ManageTemplatesPage.appendixCheckbox().shouldNotBe(checked);
    ManageTemplatesPage.appendixCheckbox().shouldBe(disabled);
    ManageTemplatesPage.standardLicenseTextsCheckbox().parent().click();
    ManageTemplatesPage.appendixCheckbox().shouldNotBe(checked);
    ManageTemplatesPage.appendixCheckbox().shouldNotBe(disabled);
  }
}
