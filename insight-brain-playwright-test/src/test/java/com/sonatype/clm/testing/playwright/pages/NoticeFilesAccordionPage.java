/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Notice Files accordion tile and modal.
 * Root element is {@code #notice-texts-tile}.
 */
public class NoticeFilesAccordionPage
    extends BasePage
{
  private static final String TILE = "#notice-texts-tile";

  private static final Locator.GetByRoleOptions ADD_NOTICE_OPTS =
      new Locator.GetByRoleOptions().setName("Add Notice");

  public NoticeFilesAccordionPage() {
    super();
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator editNoticesButton() {
    return locator("#edit-notices");
  }

  public Locator noneFoundText() {
    return tile().getByText("None found");
  }

  public Locator editButtonIcon() {
    return editNoticesButton().locator("svg");
  }

  public Locator modal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator modalHeader() {
    return modal().getByRole(AriaRole.HEADING);
  }

  public Locator addNoticeButton() {
    return modal().getByRole(AriaRole.BUTTON, ADD_NOTICE_OPTS);
  }

  public Locator noticeTextInputAt(int index) {
    return modal().getByRole(AriaRole.TEXTBOX).nth(index);
  }

  public Locator noticeRows() {
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

  public void openNoticesModal() {
    editNoticesButton().click();
  }

  public void clickAddNotice() {
    addNoticeButton().click();
  }

  public void clickCancel() {
    cancelButton().click();
  }

  public void clickSave() {
    saveButton().click();
  }
}
