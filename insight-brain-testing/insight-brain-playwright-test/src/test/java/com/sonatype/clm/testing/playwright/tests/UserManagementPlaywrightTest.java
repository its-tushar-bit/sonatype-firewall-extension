/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.pages.UserManagementPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Playwright regression tests for the User Management page (Administration → Users),
 * Add User form, and Edit User form.
 */
public class UserManagementPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String BUILTIN_ADMIN_USERNAME = "admin";

  private static final String NEW_USERNAME_PREFIX = "pw-reg-user";

  private static final String NEW_FIRST_NAME = "Playwright";

  private static final String NEW_LAST_NAME = "Regression";

  private static final String NEW_EMAIL = "pw-reg@test.local";

  private static final String NEW_PASSWORD = "TestPass123!";

  private UserManagementPage usersPage;

  private UserManagementPageAssertions assertions;

  @BeforeEach
  public void openUserManagement() {
    playwrightRefreshOrOpen(UserManagementPage.url());
    playwrightLogin();

    usersPage = new UserManagementPage();
    assertions = new UserManagementPageAssertions(usersPage);
  }

  @AfterEach
  public void resetUserActivityTracking() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_renders() {
    assertions.shouldShowList();
    assertions.shouldShowCreateUserButton();
    assertions.shouldListUser(BUILTIN_ADMIN_USERNAME);
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_tabsVisible() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    playwrightRefreshOrOpen(UserManagementPage.url());

    assertions.shouldShowActivityTrackingTabs();
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_noTabsWhenActivityDisabled() {
    assertions.shouldHideTabs();
    assertions.shouldShowList();
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_activityTabNavigation() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    playwrightRefreshOrOpen(UserManagementPage.url());

    assertions.shouldShowActivityTab();

    usersPage.clickActivityTab();

    assertions.shouldShowActivityView();
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_deleteUser() {
    User testUser = tempEntity.newUser(NEW_USERNAME_PREFIX + TemporaryEntity.uuid());
    String username = testUser.getUsername();

    playwrightRefreshOrOpen(UserManagementPage.url());

    assertions.shouldShowList();
    assertions.shouldListUser(username);

    usersPage.openUserForEdit(username);
    assertions.shouldShowEditForm();

    usersPage.deleteUserButton().click();
    usersPage.deleteConfirmModalSubmit().click();
    usersPage.waitForUsersListRoute();

    assertions.shouldShowList();
    assertions.shouldNotListUser(username);
  }

  @Test
  @Tag("regression")
  public void testUserManagementPage_cannotDeleteCurrentUser() {

    assertions.shouldShowList();
    assertions.shouldListUser(BUILTIN_ADMIN_USERNAME);

    usersPage.openUserForEdit(BUILTIN_ADMIN_USERNAME);
    assertions.shouldShowEditForm();

    usersPage.deleteUserButton().click();
    usersPage.deleteConfirmModalSubmit().click();

    assertions.shouldShowDeleteError();
  }

  @Test
  @Tag("regression")
  public void testAddUserPage_rendersEmptyForm() {

    assertions.shouldShowList();
    usersPage.clickAddUser();

    assertions.shouldShowEmptyAddUserForm();
    assertions.shouldShowUserFormSubmitButton();
    assertions.shouldShowPageTitle("Add New User");
  }

  @Test
  @Tag("regression")
  public void testAddUserPage_emptyFormValidation() {
    playwrightRefreshOrOpen(UserManagementPage.urlToCreateUser());

    assertions.shouldShowEmptyAddUserForm();

    // Empty form has validation errors (required fields) — submit is blocked
    usersPage.submitUserForm();
    assertions.shouldShowValidationErrors();
  }

  @Test
  @Tag("regression")
  public void testAddUserPage_passwordMismatchValidation() {
    // Navigate via the Create User button rather than a direct hash URL — a same-document hash
    // change immediately after login can race the SPA's auth hydration and leave the page on
    // #/users instead of #/users/_new_ (see AbstractIqUiTest#waitForAuthenticatedHeader).
    usersPage.clickAddUser();

    assertions.shouldShowEmptyAddUserForm();

    String uniqueUsername = NEW_USERNAME_PREFIX + TemporaryEntity.uuid();
    usersPage.fillUserDetails(uniqueUsername,
        NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL,
        NEW_PASSWORD, "WrongPassword!");
    usersPage.submitUserForm();
    assertions.shouldShowValidationErrors();

    playwrightRefreshOrOpen(UserManagementPage.url());
    assertions.shouldNotListUser(uniqueUsername);
  }

  @Test
  @Tag("regression")
  public void testAddUserPage_successfulSave() {
    String uniqueUsername = NEW_USERNAME_PREFIX + TemporaryEntity.uuid();

    assertions.shouldShowList();
    usersPage.clickAddUser();
    assertions.shouldShowEmptyAddUserForm();

    usersPage.fillUserDetails(uniqueUsername, NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL,
        NEW_PASSWORD, NEW_PASSWORD);
    usersPage.submitUserForm();
    waitForSubmitMask();
    usersPage.waitForUsersListRoute();

    assertions.shouldShowList();
    assertions.shouldListUser(uniqueUsername);
  }

  @Test
  @Tag("regression")
  public void testAddUserPage_cancelNavigatesBack() {

    assertions.shouldShowList();
    usersPage.clickAddUser();
    assertions.shouldShowEmptyAddUserForm();

    usersPage.userFormCancelButton().click();
    usersPage.waitForUsersListRoute();

    assertions.shouldShowList();
  }

  @Test
  @Tag("regression")
  public void testEditUser_prePopulatedAndSaveDisabledWhenUnchanged() {
    User testUser = tempEntity.newUser();
    String username = testUser.getUsername();

    playwrightRefreshOrOpen(UserManagementPage.url());

    assertions.shouldShowList();
    assertions.shouldListUser(username);

    usersPage.openUserForEdit(username);
    assertions.shouldShowEditForm();
    assertions.shouldHaveEditFirstName(testUser.getFirstName());
    assertions.shouldHaveEditLastName(testUser.getLastName());
    assertions.shouldHaveEditEmail(testUser.getEmail());
    // While the form is pristine, the UI surfaces a "No changes to update" message via NxForm's
    // validationErrors prop. NxForm sets nx-form--has-validation-errors whenever validationErrors
    // is non-empty, regardless of cause. So pristine valid form → has the class; after typing a
    // real change → validationErrors clears → class is removed.
    assertions.shouldHaveEditFormValidationErrors();

    usersPage.editFirstNameInput().fill("ChangedName");
    assertions.shouldNotHaveEditFormValidationErrors();

    usersPage.submitEditUserForm();
    waitForSubmitMask();
    usersPage.waitForUsersListRoute();

    assertions.shouldShowList();
    assertions.shouldHaveUserListItemContaining(username, "ChangedName");
  }
}
