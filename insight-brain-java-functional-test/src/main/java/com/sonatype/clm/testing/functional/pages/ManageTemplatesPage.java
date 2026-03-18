/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class ManageTemplatesPage
{
  public static final String ROOT = "#attribution-report-template-form-container";

  private ManageTemplatesPage() {
  }

  public static String url(Owner owner, String stage) {
    return BaseUrl.resolvePageUrl(
        String.format("/legal/%s/%s/stage/%s/attributionReportTemplate", owner.getType().toString(),
            owner.getPublicId(), stage));
  }

  public static String urlToApplicationScope(String publicAppId, String stage) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/stage/%s/attributionReportTemplate",
        publicAppId, stage));
  }

  private static SelenideElement getRoot() {
    return $(ROOT);
  }

  public static SelenideElement formTitle() {
    return getRoot().$("header h2#attribution-report-form-title");
  }

  public static SelenideElement createNewTemplateButton() {
    return getRoot().$("div.nx-btn-bar > button#attribution-report-create-new-template");
  }

  public static SelenideElement templateNameInput() {
    return getRoot().$("input[name=\"templateName\"]");
  }

  public static SelenideElement templateNameErrorPrompt() {
    return templateNameInput().parent().parent().$(".nx-field-validation-message");
  }

  public static SelenideElement reportTitleInput() {
    return getRoot().$("input[name=\"title\"]");
  }

  public static SelenideElement reportTitleErrorPrompt() {
    return reportTitleInput().parent().parent().$(".nx-field-validation-message");
  }

  public static SelenideElement documentHeaderInput() {
    return getRoot().$("input[name=\"header\"]");
  }

  public static SelenideElement documentFooterInput() {
    return getRoot().$("input[name=\"footer\"]");
  }

  public static SelenideElement tableOfContentsCheckbox() {
    return getRoot().$("#table-of-contents-checkbox > input");
  }

  public static SelenideElement standardLicenseTextsCheckbox() {
    return getRoot().$("#include-standard-license-checkbox > input");
  }

  public static SelenideElement appendixCheckbox() {
    return getRoot().$("#appendix-checkbox > input");
  }

  public static TemplateList templateList() {
    return new TemplateList();
  }

  public static SelenideElement saveTemplateButton() {
    return getRoot().$("footer > div > button.nx-form__submit-btn");
  }

  public static SelenideElement deleteTemplateButton() {
    return getRoot().$("#attribution-report-delete-template");
  }

  public static UnsavedChangesDialog unsavedChangesDialog() {
    return new UnsavedChangesDialog();
  }

  public static DeleteDialog deleteDialog() {
    return new DeleteDialog();
  }

  public static class UnsavedChangesDialog
      extends BasicElement<UnsavedChangesDialog>
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

  public static class DeleteDialog
      extends BasicElement<UnsavedChangesDialog>
  {
    private static final String DELETE_DIALOG_SELECTOR = "#attribution-report-delete-confirmation-dialog";

    public DeleteDialog() {
      super(DELETE_DIALOG_SELECTOR);
    }

    public SelenideElement element() {
      return getElement();
    }

    public SelenideElement cancelButton() {
      return $("footer > div > button.nx-btn.nx-btn--secondary");
    }

    public SelenideElement deleteButton() {
      return $("footer > div > button.nx-btn.nx-btn--primary");
    }
  }

  public static class TemplateList
      extends BasicElement<TemplateList>
  {
    private static final String TEMPLATE_LIST_SELECTOR = "ul#attribution-report-template-list";

    public TemplateList() {
      super(TEMPLATE_LIST_SELECTOR);
    }

    public SelenideElement itemAt(final int index) {
      return $(String.format("li:nth-child(%d) a", index));
    }

    public ElementsCollection items() {
      return this.getElement().findAll("li");
    }
  }
}
