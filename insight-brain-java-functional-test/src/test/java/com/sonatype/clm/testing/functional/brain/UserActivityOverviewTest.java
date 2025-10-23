/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.UserActivityOverviewPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.USER_PASSWORD_CLEAR;
import static org.assertj.core.api.Assertions.assertThat;

public class UserActivityOverviewTest
    extends AbstractFunctionalTest
{
  private UserActivityOverviewPage userActivityPage;

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(UserActivityOverviewPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    userActivityPage = new UserActivityOverviewPage();

    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    grantPermissions("admin", Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);
    createTestUsersWithActivity();
    refreshOrOpen(UserActivityOverviewPage.url());
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
  }

  @Test
  public void testAdminWithFeatureEnabledCanAccessUserActivity() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    refreshOrOpen(UserActivityOverviewPage.url());

    userActivityPage.waitForPageLoad();
    userActivityPage.pageTitle().shouldBe(visible);
    userActivityPage.userActivityTable().table().shouldBe(visible);
  }

  @Test
  public void testFeatureDisabledShowsAccessDenied() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
    refreshOrOpen(UserActivityOverviewPage.url());

    userActivityPage.hasFeatureDisabledError();
  }

  @Test
  public void testNonAdminUserCannotAccessUserActivity() {
    try {
      tempEntity.newUser("regularUser", "Regular", "User", "regular@example.com");

      logout();
      login("regularUser", USER_PASSWORD_CLEAR);

      refreshOrOpen(UserActivityOverviewPage.url());

      userActivityPage.hasPermissionError();
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testAdminWithoutAuditLogPermissionsShowsError() {
    try {
      tempEntity.newUser("adminNoAudit", "Admin", "NoAudit", "admin@example.com");
      grantPermissions("adminNoAudit", Organization.ROOT_ORGANIZATION_ID, Permission.READ);

      logout();
      login("adminNoAudit", USER_PASSWORD_CLEAR);

      refreshOrOpen(UserActivityOverviewPage.url());

      userActivityPage.hasPermissionError();
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testPageLoadsWithCorrectElements() {
    userActivityPage.waitForPageLoad();

    userActivityPage.pageTitle().shouldBe(visible);
    userActivityPage.searchInput().shouldBe(visible);
    userActivityPage.exportButton().shouldBe(visible);
    userActivityPage.filterButton().shouldBe(visible);
    userActivityPage.userActivityTable().table().shouldBe(visible);
  }

  @Test
  public void testUserTableDisplaysUsers() {
    userActivityPage.waitForUsersToLoad();

    // Should have at least one user (admin)
    userActivityPage.userActivityTable().userRows().shouldHave(sizeGreaterThan(0));

    // Check table headers are present
    userActivityPage.userActivityTable().usernameHeader().shouldBe(visible);
    userActivityPage.userActivityTable().loginCountHeader().shouldBe(visible);
    userActivityPage.userActivityTable().lastActiveHeader().shouldBe(visible);
  }

  @Test
  public void testSearchFunctionality() {
    userActivityPage.waitForUsersToLoad();

    // Search for admin user
    userActivityPage.searchForUser("admin");

    // Should filter to show only admin user
    userActivityPage.userActivityTable().userRows().shouldHave(size(1));
    assertThat(userActivityPage.userActivityTable().getUsernameText(0)).isEqualTo("admin");

    // Clear search
    userActivityPage.clearSearch();
    userActivityPage.userActivityTable().userRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testSortingByUsername() {
    userActivityPage.waitForUsersToLoad();

    userActivityPage.userActivityTable().clickUsernameHeader();

    userActivityPage.userActivityTable().usernameHeader().shouldBe(visible);
  }

  @Test
  public void testSortingByLoginCount() {
    userActivityPage.waitForUsersToLoad();

    userActivityPage.userActivityTable().clickLoginCountHeader();

    userActivityPage.userActivityTable().loginCountHeader().shouldBe(visible);
    userActivityPage.userActivityTable().userRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testSortingByLastActive() {
    userActivityPage.waitForUsersToLoad();

    userActivityPage.userActivityTable().clickLastActiveHeader();

    userActivityPage.userActivityTable().lastActiveHeader().shouldBe(visible);
    userActivityPage.userActivityTable().userRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testFilterDrawerOpenAndClose() {
    userActivityPage.waitForPageLoad();
    userActivityPage.filterButton().shouldBe(visible, enabled);

    userActivityPage.filterButton().click();

    $(".nx-drawer").shouldBe(visible);
    $(".nx-drawer button[aria-label*='Close']").shouldBe(visible).click();
  }

  @Test
  public void testExportFunctionality() {
    userActivityPage.waitForUsersToLoad();

    userActivityPage.exportButton().shouldBe(enabled);
    userActivityPage.clickExportButton();
    userActivityPage.exportButton().shouldBe(enabled);
  }

  @Test
  public void testNavigationToUserDetails() {
    userActivityPage.waitForUsersToLoad();

    userActivityPage.clickFirstUser();

    waitUntilNotUrl(UserActivityOverviewPage.url());
  }

  @Test
  public void testEmptyStateWhenNoUsers() {
    userActivityPage.userActivityTable().emptyMessage().shouldBe(visible);
  }

  private void createTestUsersWithActivity() {
    tempEntity.newUser("testuser1", "Test", "User1", "test1@example.com");
    tempEntity.newUser("testuser2", "Test", "User2", "test2@example.com");
  }
}
