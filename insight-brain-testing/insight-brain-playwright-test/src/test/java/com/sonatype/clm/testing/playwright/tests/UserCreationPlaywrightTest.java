/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AccessEditorPage;
import com.sonatype.clm.testing.playwright.pages.AccessEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerDetailSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.pages.UserManagementPageAssertions;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for user creation and management.
 */
public class UserCreationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Logger log = LoggerFactory.getLogger(UserCreationPlaywrightTest.class);

  private static final String BUILTIN_ADMIN_USERNAME = "admin";

  private static final String NEW_USERNAME = "Automationdeveloper1";

  private static final String NEW_FIRST_NAME = "Automation";

  private static final String NEW_LAST_NAME = "User";

  private static final String NEW_EMAIL = "Automation.User@company.com";

  private static final String NEW_PASSWORD = "automation@123";

  private static final String NEW_USER_LIST_LABEL = "Automationdeveloper1 (Automation User)";

  private static final String EDITED_FIRST_NAME = "AutomationEdited";

  private static final String EDITED_LAST_NAME = "UserEdited";

  private static final String EDITED_EMAIL = "Automation.Edited@company.com";

  private static final String EDITED_FULL_NAME = EDITED_FIRST_NAME + " " + EDITED_LAST_NAME;

  private static final String EDITED_USER_LIST_LABEL =
      "Automationdeveloper1 (" + EDITED_FULL_NAME + ")";

  private static final String DEVELOPER_ROLE_NAME = "Developer";

  private String accessOrgEditUrl;

  @BeforeEach
  public void openUserManagementAsAdmin() {
    playwrightRefreshOrOpen(UserManagementPage.url());
    playwrightLogin();
  }

  /**
   * The test body finishes logged in as the new {@code developer1} user, who can't delete users.
   * Re-establish an admin session via cookie clear + admin login, then delete the UI-created user.
   */
  @AfterEach
  public void deleteCreatedUser() {
    try {
      playwrightHardreset();
      playwrightLoginAdminAt(UserManagementPage.url());
      new UserManagementPage().deleteUserIfPresent(NEW_USERNAME);
    }
    catch (Exception e) {
      log.warn("deleteCreatedUser: best-effort delete failed: {}", e.getMessage());
    }
  }

  /* this is the test that creates the user and assigns the developer role to the user */
  @Test
  @Tag("sanity")
  public void createDeveloperUser_editDetails_andAssignAccess() {
    createEditAndGrantAccess();
  }

  /* this is the test that creates the user and assigns the developer role to the user */
  @Test
  @Tag("sanity")
  public void loginAsEditedDeveloperUser() {
    createEditAndGrantAccess();
    verifyNewUserCanLoginAndSeeDashboard();
  }

  private void createEditAndGrantAccess() {
    UserManagementPage usersPage = new UserManagementPage();
    UserManagementPageAssertions usersPageAssertions = new UserManagementPageAssertions(usersPage);
    usersPageAssertions.shouldShowList();
    usersPageAssertions.shouldListUser(BUILTIN_ADMIN_USERNAME);

    usersPage.clickAddUser();
    usersPageAssertions.shouldShowEmptyAddUserForm();

    usersPage.fillUserDetails(NEW_USERNAME, NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL, NEW_PASSWORD, NEW_PASSWORD);
    usersPage.submitUserForm();
    waitForSubmitMask();
    usersPage.waitForUsersListRoute();

    usersPageAssertions.shouldShowList();
    usersPageAssertions.shouldListUser(NEW_USERNAME);
    assertThat(usersPage.userListItem(NEW_USERNAME)).containsText(NEW_USER_LIST_LABEL);

    editCreatedUserDetails(usersPage);
    assignDeveloperRoleViaUi();
  }

  private void editCreatedUserDetails(UserManagementPage usersPage) {
    usersPage.openUserForEdit(NEW_USERNAME);
    usersPage.fillEditUserDetails(EDITED_FIRST_NAME, EDITED_LAST_NAME, EDITED_EMAIL);
    usersPage.submitEditUserForm();
    waitForSubmitMask();
    usersPage.waitForUsersListRoute();

    UserManagementPageAssertions usersPageAssertions = new UserManagementPageAssertions(usersPage);
    usersPageAssertions.shouldShowList();
    usersPageAssertions.shouldListUser(NEW_USERNAME);
    assertThat(usersPage.userListItem(NEW_USERNAME)).containsText(EDITED_USER_LIST_LABEL);
  }

  private void assignDeveloperRoleViaUi() {
    Organization accessOrg = tempEntity.newOrganization("PW Access Assignment Org");
    accessOrgEditUrl = OwnerSummaryPage.editUrl(accessOrg.getId());
    playwrightNavigateTo(accessOrgEditUrl + "/access");

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    new AccessEditorPageAssertions(accessEditorPage).shouldBeVisible();
    accessEditorPage.selectRole(DEVELOPER_ROLE_NAME);
    accessEditorPage.searchAndSelectUser(EDITED_FULL_NAME);
    new AccessEditorPageAssertions(accessEditorPage).shouldHaveAssociatedMember(EDITED_FULL_NAME);
    accessEditorPage.submit();
    waitForSubmitMask();
    new AccessEditorPageAssertions(accessEditorPage).shouldNotShowSubmitError();
  }

  private void verifyNewUserCanLoginAndSeeDashboard() {
    playwrightLogout();

    LoginPage loginPage = new LoginPage();
    new LoginPageAssertions(loginPage).shouldBeVisible();

    playwrightLoginAt(DashboardPage.url(), NEW_USERNAME, NEW_PASSWORD);

    DashboardPage dashboardPage = new DashboardPage();
    DashboardPageAssertions dashboardAssertions = new DashboardPageAssertions(dashboardPage);
    dashboardAssertions.shouldBeLoaded();
    dashboardAssertions.shouldNotShowDashboardDisabledMessage();

    playwrightNavigateTo(accessOrgEditUrl);
    OwnerDetailSidebarComponent sidebar = new OwnerDetailSidebarComponent();
    assertThat(sidebar.container()).isVisible();

    HeaderComponent header = new HeaderComponent();
    HeaderComponentAssertions headerAssertions = new HeaderComponentAssertions(header);
    headerAssertions.shouldBeLoggedIn();
    header.openUserMenu();
    headerAssertions.shouldShowUserName(EDITED_FULL_NAME);
  }
}
