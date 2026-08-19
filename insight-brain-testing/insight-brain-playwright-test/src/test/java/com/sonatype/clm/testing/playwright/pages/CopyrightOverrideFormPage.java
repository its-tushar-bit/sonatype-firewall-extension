/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class CopyrightOverrideFormPage
    extends BasePage
{
  private static final String COPYRIGHT_TILE = "#copyright-statements-tile";

  private static final Locator.GetByRoleOptions ADD_COPYRIGHT_OPTS =
      new Locator.GetByRoleOptions().setName("Add Copyright");

  public CopyrightOverrideFormPage() {
    super();
  }

  public Locator copyrightTile() {
    return locator(COPYRIGHT_TILE);
  }

  public Locator editCopyrightsButton() {
    return locator("#edit-copyrights");
  }

  public Locator modal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator modalHeader() {
    return modal().getByRole(AriaRole.HEADING);
  }

  public Locator copyrightTextInputs() {
    return modal().getByRole(AriaRole.TEXTBOX);
  }

  public Locator copyrightTextInputAt(int index) {
    return copyrightTextInputs().nth(index);
  }

  public Locator copyrightToggleAt(int index) {
    return modal().locator(".copyright-override-status-toggle").nth(index);
  }

  public Locator validationAlert() {
    return modal().locator(".nx-form__validation-errors");
  }

  public Locator addCopyrightButton() {
    return modal().getByRole(AriaRole.BUTTON, ADD_COPYRIGHT_OPTS);
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

  public Locator copyrightRows() {
    return modal().getByRole(AriaRole.ROW);
  }

  public void openCopyrightModal() {
    editCopyrightsButton().click();
  }

  public void clickAddCopyright() {
    addCopyrightButton().click();
  }

  public void fillCopyrightText(int index, String text) {
    copyrightTextInputAt(index).fill(text);
  }

  public void clickToggle(int index) {
    copyrightToggleAt(index).click();
  }

  public void selectScope(String value) {
    scopeDropdown().selectOption(value);
  }

  public void clickSave() {
    saveButton().click();
  }

  public void clickCancel() {
    cancelButton().click();
  }
}
