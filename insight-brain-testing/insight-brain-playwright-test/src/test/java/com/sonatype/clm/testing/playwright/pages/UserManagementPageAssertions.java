/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link UserManagementPage}.
 */
public class UserManagementPageAssertions
{
  private final UserManagementPage page;

  public UserManagementPageAssertions(UserManagementPage page) {
    this.page = page;
  }

  public void shouldShowList() {
    assertThat(page.container()).isVisible();
    assertThat(page.userList()).isVisible();
  }

  public void shouldHaveUserCount(int expected) {
    assertThat(page.userListItems()).hasCount(expected);
  }

  public void shouldListUser(String username) {
    assertThat(page.userListItem(username)).isVisible();
  }

  public void shouldNotListUser(String username) {
    assertThat(page.userListItem(username)).hasCount(0);
  }

  public void shouldShowEmptyAddUserForm() {
    assertThat(page.userForm()).isVisible();
    assertThat(page.usernameInput()).hasValue("");
    assertThat(page.firstNameInput()).hasValue("");
    assertThat(page.lastNameInput()).hasValue("");
    assertThat(page.emailInput()).hasValue("");
    assertThat(page.passwordInput()).hasValue("");
    assertThat(page.passwordValidateInput()).hasValue("");
  }

  public void shouldShowCreateUserButton() {
    assertThat(page.createUserButton()).isVisible();
  }

  public void shouldShowEditForm() {
    assertThat(page.editForm()).isVisible();
  }

  public void shouldHaveEditFirstName(String expected) {
    assertThat(page.editFirstNameInput()).hasValue(expected);
  }

  public void shouldHaveEditLastName(String expected) {
    assertThat(page.editLastNameInput()).hasValue(expected);
  }

  public void shouldHaveEditEmail(String expected) {
    assertThat(page.editEmailInput()).hasValue(expected);
  }

  public void shouldHaveEditFormValidationErrors() {
    assertThat(page.editForm()).hasClass(BasePage.cssClassPattern("nx-form--has-validation-errors"));
  }

  public void shouldNotHaveEditFormValidationErrors() {
    assertThat(page.editForm()).not().hasClass(BasePage.cssClassPattern("nx-form--has-validation-errors"));
  }

  public void shouldShowValidationErrors() {
    assertThat(page.userFormValidationError()).isVisible();
  }

  public void shouldShowUserFormSubmitButton() {
    assertThat(page.userFormSubmitButton()).isVisible();
  }

  public void shouldShowPageTitle(String expectedTitle) {
    assertThat(page.pageTitle()).hasText(expectedTitle);
  }

  public void shouldShowDeleteError() {
    assertThat(page.deleteSubmitError()).isVisible();
  }

  public void shouldShowActivityTrackingTabs() {
    assertThat(page.tabList()).isVisible();
    assertThat(page.usersTab()).isVisible();
    assertThat(page.activityTab()).isVisible();
  }

  public void shouldHideTabs() {
    assertThat(page.tabList()).isHidden();
  }

  public void shouldShowActivityTab() {
    assertThat(page.activityTab()).isVisible();
  }

  public void shouldShowActivityView() {
    assertThat(page.userList()).isHidden();
    assertThat(page.activityTable()).isVisible();
  }

  public void shouldHaveUserListItemContaining(String username, String expectedText) {
    assertThat(page.userListItem(username)).containsText(expectedText);
  }
}
