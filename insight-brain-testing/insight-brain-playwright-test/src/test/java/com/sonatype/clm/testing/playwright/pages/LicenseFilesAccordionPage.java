/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the License Files accordion tile and modal.
 * Root element is {@code #license-texts-tile}.
 */
public class LicenseFilesAccordionPage
    extends BasePage
{
  private static final String TILE = "#license-texts-tile";

  private static final Locator.GetByRoleOptions ADD_LICENSE_OPTS =
      new Locator.GetByRoleOptions().setName("Add License");

  public LicenseFilesAccordionPage() {
    super();
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator editLicenseFilesButton() {
    return locator("#edit-license-files");
  }

  public Locator noneFoundText() {
    return tile().getByText("None found");
  }

  public Locator editButtonIcon() {
    return editLicenseFilesButton().locator("svg");
  }

  public Locator modal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator modalHeader() {
    return modal().getByRole(AriaRole.HEADING);
  }

  public Locator addLicenseButton() {
    return modal().getByRole(AriaRole.BUTTON, ADD_LICENSE_OPTS);
  }

  public Locator licenseRows() {
    return modal().getByRole(AriaRole.ROW);
  }

  public Locator scopeDropdown() {
    return modal().getByLabel("Scope");
  }

  public Locator saveButton() {
    return modal().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator cancelButton() {
    return modal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public void openLicenseFilesModal() {
    editLicenseFilesButton().click();
  }

  public void clickAddLicense() {
    addLicenseButton().click();
  }

  public void clickCancel() {
    cancelButton().click();
  }

  public void clickSave() {
    saveButton().click();
  }
}
