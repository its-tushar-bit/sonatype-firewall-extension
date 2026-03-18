/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class AttributionReportFormPage
    extends BasicElement<AttributionReportFormPage>
{
  public static final String ROOT = "#attribution-report-form-container";

  public AttributionReportFormPage() {
    super(ROOT);
  }

  public static String url(String applicationPublicId, String stageTypeId) {
    return BaseUrl.resolvePageUrl(
        String.format("/legal/application/%s/stage/%s/attributionReport", applicationPublicId, stageTypeId));
  }

  public static String sbomManagerUrl(String applicationPublicId) {
    return BaseUrl.resolvePageUrl(String.format("/sbomManager/legal/application/%s/stage/%s/attributionReport",
        applicationPublicId, StageTypes.COMPLIANCE.getId()));
  }

  public SelenideElement getTitleInput() {
    return child("input[name='title']");
  }

  public SelenideElement getHeaderInput() {
    return child("input[name='header']");
  }

  public SelenideElement getFooterInput() {
    return child("input[name='footer']");
  }

  public ElementsCollection getFileInputs() {
    return children("input[type='file']");
  }

  public SelenideElement getFormSubmitBtn() {
    return child("form .nx-footer button");
  }

  public ElementsCollection getUploadedFileListItems() {
    return children(".nx-list .nx-list__item");
  }

  public ElementsCollection getUploadedFilesListItemButton() {
    return children(".nx-list button");
  }

  public SelenideElement getDeleteFileModalConfirmationButton() {
    return child(".nx-modal footer .nx-btn--primary");
  }

  public SelenideElement getSubmitMask() {
    return child(".nx-submit-mask__message");
  }

  public SelenideElement getTableOfContentsCheck() {
    return child("#table-of-contents-checkbox");
  }

  public SelenideElement getTableOfContentsHiddenInput() {
    return child("input[name='includeToc']");
  }

  public SelenideElement getIncludeStandardLicenseTextsCheck() {
    return child("#include-standard-license-checkbox");
  }

  public SelenideElement getIncludeStandardLicenseTextsHiddenInput() {
    return child("input[name='includeStandardLicenseTexts']");
  }

  public SelenideElement getAppendixCheck() {
    return child("#appendix-checkbox");
  }

  public SelenideElement getAppendixNativeCheck() {
    return child("#appendix-checkbox > input");
  }

  public SelenideElement getAppendixHiddenInput() {
    return child("input[name='includeAppendix']");
  }

  public SelenideElement getIncludeInnerSourceCheckboxInput() {
    return child("input[name='includeInnerSource']");
  }

  public SelenideElement getIncludeSonatypeSpecialLicensesCheckboxInput() {
    return child("input[name='includeSonatypeSpecialLicenses']");
  }

  public SelenideElement getTemplatesDropdown() {
    return child(".nx-dropdown__toggle");
  }

  public ElementsCollection getTemplatesDropdownItems() {
    return children(".nx-dropdown-button");
  }

  public static class UnsavedChangesDialog
      extends BasicElement<ManageTemplatesPage.UnsavedChangesDialog>
  {
    private static final String UNSAVED_DIALOG_SELECTOR = "#attribution-report-unsaved-dialog";

    public UnsavedChangesDialog() {
      super(UNSAVED_DIALOG_SELECTOR);
    }

    public SelenideElement element() {
      return getElement();
    }

    public SelenideElement cancelButton() {
      return $("footer > div > button.nx-btn.nx-btn--secondary");
    }

    public SelenideElement continueButton() {
      return $("footer > div > button.nx-btn.nx-btn--primary");
    }
  }
}
