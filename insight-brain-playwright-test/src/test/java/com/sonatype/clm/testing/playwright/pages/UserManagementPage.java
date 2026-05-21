/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the User Management page (Administration → Users).
 * <p>
 * Covers both the user list and the Add/Edit user form. The Add User form intentionally has
 * <em>no</em> Roles dropdown — role assignment is performed on the
 * {@link AdministratorsEditPage} (Administration → Roles).
 */
public class UserManagementPage
    extends BasePage
{
  private static final String ROOT = "#user-management";

  private static final String CREATE_USER_BTN = "#create-user";

  private static final String USER_FORM = "#user-form";

  private static final String USER_LIST = "#user-management-list";

  public UserManagementPage() {
    super();
  }

  // --------------- URLs ---------------

  /** Users list (Administration → Users). Matches ui-router state {@code users} → url {@code /users}. */
  public static String url() {
    return "/assets/index.html#/users";
  }

  /** Add User form. Matches ui-router state {@code createUser} → url {@code /users/_new_}. */
  public static String urlToCreateUser() {
    return "/assets/index.html#/users/_new_";
  }

  // --------------- List view locators ---------------

  public Locator container() {
    return locator(ROOT);
  }

  public Locator createUserButton() {
    return byRole(com.microsoft.playwright.options.AriaRole.BUTTON, "Create User");
  }

  public Locator userList() {
    return locator(USER_LIST);
  }

  public Locator userListItems() {
    return locator(USER_LIST + " .nx-list__item");
  }

  /** A list item whose visible text contains the given username (matches "username (First Last)"). */
  public Locator userListItem(String username) {
    return userListItems().filter(new Locator.FilterOptions().setHasText(username));
  }

  public Locator currentUserItem() {
    return locator(ROOT + " .iq-user-list-item-current");
  }

  public Locator loadError() {
    return locator(ROOT + " .nx-alert--load-error");
  }

  // --------------- Add User form locators ---------------

  public Locator userForm() {
    return locator(USER_FORM);
  }

  public Locator firstNameInput() {
    return byLabel("First Name");
  }

  public Locator lastNameInput() {
    return byLabel("Last Name");
  }

  public Locator emailInput() {
    return byLabel("Email");
  }

  public Locator usernameInput() {
    return byLabel("Username");
  }

  public Locator passwordInput() {
    // byLabel("Password") also matches "Validate Password" via substring — use the stable id.
    return locator("#password");
  }

  public Locator passwordValidateInput() {
    return byLabel("Validate Password");
  }

  public Locator userFormSubmitButton() {
    return userForm().getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Save"));
  }

  public Locator userFormCancelButton() {
    return userForm().getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel"));
  }

  public Locator userFormValidationError() {
    return locator(USER_FORM + " .nx-form__validation-errors");
  }

  public Locator userFormSubmitError() {
    return locator(USER_FORM + " .nx-form__submit-error");
  }

  // --------------- Edit User form (existing patterns) ---------------

  public Locator editForm() {
    return locator("#user-edit");
  }

  public Locator editFirstNameInput() {
    return editForm().getByLabel("First Name");
  }

  public Locator editLastNameInput() {
    return editForm().getByLabel("Last Name");
  }

  public Locator editEmailInput() {
    return editForm().getByLabel("Email");
  }

  public Locator editFormSubmitButton() {
    // NxStatefulForm may render its footer outside the <form> element in some RSC versions.
    // Use the RSC submit-button class scoped to the edit form root.
    return locator("#user-edit .nx-form__submit-btn");
  }

  public Locator deleteUserButton() {
    return byRole(com.microsoft.playwright.options.AriaRole.BUTTON, "Delete User");
  }

  public Locator resetPasswordButton() {
    return byRole(com.microsoft.playwright.options.AriaRole.BUTTON, "Reset Password");
  }

  // Copy Password Modal (Reset Password modal)
  public Locator copyPasswordModal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator("#copy-password-modal");
  }

  public Locator copyPasswordModalSubmit() {
    return locator("#copy-password-modal .nx-form__submit-btn");
  }

  public Locator copyPasswordInput() {
    return locator("#copy-password-modal .nx-text-input__input");
  }

  // --------------- Actions ---------------

  public void clickAddUser() {
    createUserButton().click();
    assertThat(userForm())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void fillUserDetails(
      String username,
      String firstName,
      String lastName,
      String email,
      String password,
      String confirmPassword)
  {
    firstNameInput().fill(firstName);
    lastNameInput().fill(lastName);
    emailInput().fill(email);
    usernameInput().fill(username);
    passwordInput().fill(password);
    passwordValidateInput().fill(confirmPassword);
  }

  public void submitUserForm() {
    userFormSubmitButton().click();
  }

  /**
   * Matches the users-list route {@code #/users} but not sub-routes like {@code #/users/_new_} or {@code #/users/{id}}.
   */
  private static final Pattern USERS_LIST_URL = Pattern.compile(".*/users(\\?.*)?$");

  /**
   * Wait for the SPA to land on the users-list route ({@code #/users}).
   *
   * <p>
   * Safe to call from any users sub-route (create {@code #/users/_new_}, edit {@code #/users/{id}})
   * because the regex anchors on {@code $} — it only matches when there is nothing after
   * {@code /users} except an optional query string.
   */
  public void waitForUsersListRoute() {
    page.waitForURL(USERS_LIST_URL,
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS));
  }

  public void openUserForEdit(String username) {
    userListItem(username).click();
    assertThat(editForm())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void fillEditUserDetails(String firstName, String lastName, String email) {
    editFirstNameInput().fill(firstName);
    editLastNameInput().fill(lastName);
    editEmailInput().fill(email);
  }

  public void submitEditUserForm() {
    editFormSubmitButton().click();
  }

  // --------------- Delete helpers (UI-driven cleanup) ---------------

  /** Trash-can delete button on a user list row. */
  public Locator deleteUserListItemButton(String username) {
    return userListItem(username).locator(".iq-user-list-item__delete-btn");
  }

  /** Confirmation modal shown after clicking a row's trash-can icon. */
  public Locator deleteConfirmModalSubmit() {
    // NxModal does not set aria-labelledby in this RSC version; byRole(DIALOG) is too broad.
    // The original pattern targets the submit button inside any open NxModal.
    return locator(".nx-modal .nx-form__submit-btn");
  }

  /**
   * Best-effort UI cleanup: delete the user if a row exists. Safe to call when the row is absent.
   */
  public void deleteUserIfPresent(String username) {
    Locator row = userListItem(username);
    if (row.count() == 0) {
      return;
    }
    deleteUserListItemButton(username).click();
    deleteConfirmModalSubmit().click();
    assertThat(row).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

}
