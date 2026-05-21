/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;

/**
 * Playwright page object for owner/application Access editor (Add Role flow).
 */
public class AccessEditorPage
    extends BasePage
{
  private static final String ROOT = "#create-edit-access-page";

  private static final String FORM = "#access-add-members-form";

  public AccessEditorPage() {
    super();
  }

  public Locator root() {
    return locator(ROOT);
  }

  public Locator form() {
    return locator(FORM);
  }

  public Locator roleSelect() {
    return locator(FORM + " select");
  }

  public Locator searchInput() {
    return locator(FORM + " .nx-search-dropdown__input .nx-text-input__input");
  }

  public Locator searchResults() {
    return locator(FORM + " .nx-search-dropdown__menu .nx-dropdown-button");
  }

  public Locator associatedMembers() {
    return locator(FORM + " .nx-transfer-list__item");
  }

  public Locator submitError() {
    return locator(FORM + " .nx-form__submit-error");
  }

  public Locator submitButton() {
    // NxStatefulForm may render its footer outside the <form> element — scope to the form root
    // container rather than the <form> element itself to reliably find the submit button.
    return locator(FORM + " .nx-form__submit-btn");
  }

  public void selectRole(String roleName) {
    roleSelect().selectOption(new SelectOption().setLabel(roleName));
  }

  public void searchAndSelectUser(String query) {
    searchInput().fill(query);
    searchInput().click();
    searchResults().filter(new Locator.FilterOptions().setHasText(query)).first().click();
  }

  public void submit() {
    submitButton().click();
  }
}
