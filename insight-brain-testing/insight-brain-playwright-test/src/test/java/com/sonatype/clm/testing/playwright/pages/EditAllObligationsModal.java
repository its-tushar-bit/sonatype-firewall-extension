/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Edit All Obligations modal.
 * Root element is {@code #all-obligations-modal}.
 */
public class EditAllObligationsModal
    extends BasePage
{
  private static final String MODAL = "#all-obligations-modal";

  public EditAllObligationsModal() {
    super();
  }

  public Locator modal() {
    return locator(MODAL);
  }

  public Locator statusDropdown() {
    return locator("#all-obligations-status-dropdown button span span");
  }

  public Locator commentInput() {
    return locator(MODAL + " .nx-text-input__input");
  }

  public Locator scopeSelectedOption() {
    return locator("#all-obligations-scope-selection option:checked");
  }

  public Locator scopeDropdown() {
    return locator("#all-obligations-scope-selection");
  }

  private Locator saveButton() {
    return locator(MODAL + " .nx-btn--primary");
  }

  public void fillComment(String comment) {
    commentInput().fill(comment);
  }

  public void save() {
    saveButton().click();
  }

}
