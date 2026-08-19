/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Original Sources form modal.
 * Root element is {@code #edit-original-sources-attribution-modal}.
 */
public class OriginalSourcesFormPage
    extends BasePage
{
  private static final String TILE = "#original-sources-tile";

  private static final Locator.GetByRoleOptions ADD_LINK_OPTS =
      new Locator.GetByRoleOptions().setName("Add Link");

  public OriginalSourcesFormPage() {
    super();
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator editOriginalSourcesButton() {
    return locator("#edit-original-sources");
  }

  public Locator modal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator modalHeader() {
    return modal().getByRole(AriaRole.HEADING);
  }

  public Locator addLinkButton() {
    return modal().getByRole(AriaRole.BUTTON, ADD_LINK_OPTS);
  }

  public Locator sourceUrlInputAt(int index) {
    return modal().getByRole(AriaRole.TEXTBOX).nth(index);
  }

  public Locator sourceToggleAt(int index) {
    return modal().locator(".original-sources-override-status-toggle").nth(index);
  }

  public Locator validationAlert() {
    return modal().locator(".nx-form__validation-errors");
  }

  public Locator sourceRows() {
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

  public void openOriginalSourcesModal() {
    editOriginalSourcesButton().click();
  }

  public void clickAddLink() {
    addLinkButton().click();
  }

  public void fillSourceUrl(int index, String url) {
    sourceUrlInputAt(index).fill(url);
  }

  public void clickToggle(int index) {
    sourceToggleAt(index).click();
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
