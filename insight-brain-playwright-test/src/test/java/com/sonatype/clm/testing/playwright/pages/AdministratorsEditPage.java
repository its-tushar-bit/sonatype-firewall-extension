/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the IQ Server Administrators edit page.
 * ({@code RoleDetails}, {@code AddMembersForm}).
 */
public class AdministratorsEditPage
    extends BasePage
{
  private static final String ROOT = ".nx-page-main.iq-administrators-edit";

  public AdministratorsEditPage() {
    super();
  }

  public static String url(String roleId) {
    return "/assets/index.html#/administrators/" + roleId;
  }

  public Locator root() {
    return locator(ROOT);
  }

  // --------------- Role Details ---------------

  public Locator roleDetailsSection() {
    return locator(ROOT + " .nx-read-only");
  }

  public Locator roleName() {
    return locator(ROOT + " .nx-read-only .nx-read-only__data").first();
  }

  public Locator roleDescription() {
    return locator(ROOT + " .nx-read-only .nx-read-only__data").last();
  }

  // --------------- Add Members Form ---------------

  private static final String FORM = ROOT + " #administrators-add-members-form";

  public Locator addMembersForm() {
    return locator(FORM);
  }

  public Locator searchInput() {
    return addMembersForm().getByPlaceholder("Search");
  }

  public Locator searchResults() {
    return locator(FORM + " .nx-search-dropdown__menu .nx-dropdown-button");
  }

  public Locator addedItems() {
    return locator(FORM + " .nx-transfer-list__item");
  }

  public Locator cancelButton() {
    return addMembersForm().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  public Locator submitButton() {
    // NxStatefulForm may render its footer outside the <form> element in some RSC versions,
    // which causes getByRole(BUTTON,"Save") scoped to the form id to time out.
    // Use the RSC submit-button class scoped to the page root instead.
    return locator(ROOT + " .nx-form__submit-btn");
  }

  public Locator removeAllButton() {
    return locator(FORM + " .nx-transfer-list__move-all");
  }

  // --------------- Actions ---------------

  public void searchAndSelectResult(String query, int resultIndex) {
    searchInput().fill(query);
    searchInput().click();
    searchResults().nth(resultIndex).click();
  }

  /**
   * Search by {@code query} and add the dropdown match whose visible text equals {@code matchText}.
   * Prefer this over {@link #searchAndSelectResult(String, int)} in test bodies — selecting by
   * displayed name is robust against reordering of the search results.
   */
  public void searchAndAddByText(String query, String matchText) {
    searchInput().fill(query);
    searchInput().click();
    searchResults().filter(new Locator.FilterOptions().setHasText(matchText)).first().click();
  }

  /**
   * Remove the previously added member whose visible text equals {@code itemText}. Clicking a
   * selected transfer-list item triggers {@code onRemoveMembers} ({@code AdministratorsEdit.jsx}).
   */
  public void removeAddedItem(String itemText) {
    addedItems().filter(new Locator.FilterOptions().setHasText(itemText)).first().click();
  }

  public void submit() {
    submitButton().click();
  }

  public void cancel() {
    cancelButton().click();
  }

  /** True when the edit form is currently rendered (used by best-effort {@code @After} cleanup). */
  public boolean isVisibleSafe() {
    return root().count() > 0 && root().first().isVisible();
  }

}
