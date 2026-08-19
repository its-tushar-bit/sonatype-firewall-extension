/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.AdministratorsEditPageAssertions;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Coverage for the System Administrators screen
 * ({@code configuration/administrators}, {@link AdministratorsPage} +
 * {@link AdministratorsEditPage}).
 *
 * <p>
 * Authoring rules: see {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 *
 * <ul>
 * <li>DB seed (extra non-admin users) is performed directly by {@link #seedNonAdminUsers()}.</li>
 * <li>Selectors live on the page objects; no raw CSS in this file (§4).</li>
 * <li>The transfer-list label format ({@code "<displayName> (<internalName>)"} for users,
 * {@code "<name> (Group)"} for groups) is owned by {@code formatGroupUsers.js}; the expected
 * formatted strings are encoded as constants so the contract is greppable across the codebase.</li>
 * </ul>
 */
public class AdministratorsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Logger log = LoggerFactory.getLogger(AdministratorsPlaywrightTest.class);

  private static final String POLICY_ADMIN_ROLE_ID = "b9646757e98e486da7d730025f5245f8";

  private static final String POLICY_ADMIN_ROLE_NAME = "Policy Administrator";

  private static final String POLICY_ADMIN_ROLE_DESCRIPTION =
      "Manages all organizations, applications, policies, and policy violations.";

  private static final String SYSTEM_ADMIN_ROLE_NAME = "System Administrator";

  private static final int EXPECTED_DEFAULT_ROLE_COUNT = 2;

  private static final String BUILTIN_ADMIN_LIST_LABEL = "Admin BuiltIn";

  private static final String BUILTIN_ADMIN_MEMBER_ITEM = "Admin BuiltIn (admin)";

  private static final String SEED_USER_A_USERNAME = "test-a";

  private static final String SEED_USER_A_PASSWORD = "secret";

  private static final String SEED_USER_A_FIRST_NAME = "John";

  private static final String SEED_USER_A_LAST_NAME = "Doe";

  private static final String SEED_USER_A_EMAIL = "john@doe.net";

  private static final String SEED_USER_B_USERNAME = "test-b";

  private static final String SEED_USER_B_PASSWORD = "secret";

  private static final String SEED_USER_B_FIRST_NAME = "Jane";

  private static final String SEED_USER_B_LAST_NAME = "Doe";

  private static final String SEED_USER_B_EMAIL = "jane@doe.net";

  private static final String ADDED_USER_MEMBER_ITEM = "Jane Doe (test-b)";

  private static final String AUTHENTICATED_USERS_GROUP_ITEM = "Authenticated Users (Group)";

  private static final String EXPECTED_ROLE_MEMBERS_AFTER_ADD =
      "Authenticated Users, Admin BuiltIn, Jane Doe";

  private static final String SEARCH_WILDCARD = "*";

  private static final Pattern ROLE_MEMBERSHIPS_URL =
      Pattern.compile(".*/api/v2/roleMemberships/global/roles.*");

  private static final String EMPTY_RESPONSE = "{\"membersByRole\":[]}";

  private static final String ERROR_RESPONSE = "{\"statusCode\":500,\"message\":\"Internal Server Error\"}";

  private static final String EMPTY_MESSAGE = "No data found.";

  private static final String TABLE_HEADER_ROLE = "Role";

  private static final String TABLE_HEADER_MEMBERS = "Members";

  private AdministratorsPage adminsPage;

  private AdministratorsPageAssertions adminsAssertions;

  private AdministratorsEditPage editPage;

  private AdministratorsEditPageAssertions editAssertions;

  @BeforeEach
  public void seedUsersAndOpenAsAdmin() {
    seedNonAdminUsers();

    playwrightRefreshOrOpen(AdministratorsPage.url());
    playwrightLogin();

    adminsPage = new AdministratorsPage();
    adminsAssertions = new AdministratorsPageAssertions(adminsPage);
    editPage = new AdministratorsEditPage();
    editAssertions = new AdministratorsEditPageAssertions(editPage);
  }

  private void seedNonAdminUsers() {
    tempEntity.newUser(SEED_USER_A_USERNAME, SEED_USER_A_PASSWORD,
        SEED_USER_A_FIRST_NAME, SEED_USER_A_LAST_NAME, SEED_USER_A_EMAIL);
    tempEntity.newUser(SEED_USER_B_USERNAME, SEED_USER_B_PASSWORD,
        SEED_USER_B_FIRST_NAME, SEED_USER_B_LAST_NAME, SEED_USER_B_EMAIL);
  }

  /**
   * Best-effort cleanup: if a test bailed mid-edit, the SPA's global Unsaved Changes modal may
   * still be open. Dismiss via the page object so we don't leak raw modal CSS into the test
   * (§4). The next {@code @Before} re-navigates regardless, so this is purely defensive.
   */
  @AfterEach
  public void dismissUnsavedChangesIfOpen() {
    try {
      new UnsavedChangesModalComponent().continueIfOpen();
    }
    catch (Exception e) {
      log.warn("dismissUnsavedChangesIfOpen: best-effort dismissal failed: {}", e.getMessage());
    }
  }

  @Test
  @Tag("sanity")
  public void testDefaultRolesAndBuiltinUsers() {
    adminsAssertions.shouldShowContainer();
    adminsAssertions.shouldShowPageTitle();
    adminsAssertions.shouldShowTileHeader();
    adminsAssertions.shouldShowTableHeaderRoleColumn(TABLE_HEADER_ROLE);
    adminsAssertions.shouldShowTableHeaderMembersColumn(TABLE_HEADER_MEMBERS);
    adminsAssertions.shouldHaveRowCount(EXPECTED_DEFAULT_ROLE_COUNT);

    assertThat(adminsPage.row(0)).isVisible();
    adminsAssertions.rowShouldHaveRole(0, POLICY_ADMIN_ROLE_NAME);
    adminsAssertions.rowShouldHaveMembers(0, BUILTIN_ADMIN_LIST_LABEL);
    adminsAssertions.shouldShowChevron(0);

    assertThat(adminsPage.row(1)).isVisible();
    adminsAssertions.rowShouldHaveRole(1, SYSTEM_ADMIN_ROLE_NAME);
    adminsAssertions.rowShouldHaveMembers(1, BUILTIN_ADMIN_LIST_LABEL);
    adminsAssertions.shouldShowChevron(1);
  }

  @Test
  @Tag("sanity")
  public void testClickEdit() {
    assertThat(adminsPage.row(0)).isVisible();
    PlaywrightWaitUtils.clickAndWaitForUrlContains(page, adminsPage.row(0),
        AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    editAssertions.shouldBeVisible();
    editAssertions.shouldShowRoleName(POLICY_ADMIN_ROLE_NAME);
    editAssertions.shouldShowRoleDescription(POLICY_ADMIN_ROLE_DESCRIPTION);
    assertThat(editPage.addMembersForm()).isVisible();
    editAssertions.shouldHaveAddedItemCount(1);
    editAssertions.shouldHaveAddedItemTexts(BUILTIN_ADMIN_MEMBER_ITEM);
  }

  @Test
  @Tag("sanity")
  public void testSubmitAddMembersForm() {
    assertThat(adminsPage.row(0)).isVisible();
    adminsAssertions.rowShouldHaveRole(0, POLICY_ADMIN_ROLE_NAME);
    adminsAssertions.rowShouldHaveMembers(0, BUILTIN_ADMIN_LIST_LABEL);

    PlaywrightWaitUtils.clickAndWaitForUrlContains(page, adminsPage.row(0),
        AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    editPage.searchAndAddByText(SEARCH_WILDCARD, ADDED_USER_MEMBER_ITEM);
    editAssertions.shouldHaveAddedItemCount(2);
    editAssertions.shouldHaveAddedItemTexts(BUILTIN_ADMIN_MEMBER_ITEM, ADDED_USER_MEMBER_ITEM);

    editPage.searchAndAddByText(SEARCH_WILDCARD, AUTHENTICATED_USERS_GROUP_ITEM);
    editAssertions.shouldHaveAddedItemCount(3);
    editAssertions.shouldHaveAddedItemTexts(
        BUILTIN_ADMIN_MEMBER_ITEM,
        AUTHENTICATED_USERS_GROUP_ITEM,
        ADDED_USER_MEMBER_ITEM);

    editPage.submit();
    PlaywrightWaitUtils.waitForUrl(page, AdministratorsPage.url());
    assertThat(adminsPage.row(0)).isVisible();
    adminsAssertions.rowShouldHaveRole(0, POLICY_ADMIN_ROLE_NAME);
    adminsAssertions.rowShouldHaveMembers(0, EXPECTED_ROLE_MEMBERS_AFTER_ADD);

    PlaywrightWaitUtils.clickAndWaitForUrlContains(page, adminsPage.row(0),
        AdministratorsEditPage.url(POLICY_ADMIN_ROLE_ID));

    editPage.removeAddedItem(AUTHENTICATED_USERS_GROUP_ITEM);
    editPage.removeAddedItem(ADDED_USER_MEMBER_ITEM);
    editAssertions.shouldHaveAddedItemTexts(BUILTIN_ADMIN_MEMBER_ITEM);

    editPage.submit();
    PlaywrightWaitUtils.waitForUrl(page, AdministratorsPage.url());
    assertThat(adminsPage.row(0)).isVisible();
    adminsAssertions.rowShouldHaveRole(0, POLICY_ADMIN_ROLE_NAME);
    adminsAssertions.rowShouldHaveMembers(0, BUILTIN_ADMIN_LIST_LABEL);
  }

  @Test
  @Tag("regression")
  public void testAdministratorsTable_emptyState() {
    page.route(ROLE_MEMBERSHIPS_URL, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody(EMPTY_RESPONSE)));

    playwrightRefreshOrOpen(AdministratorsPage.url());

    adminsAssertions.shouldShowContainer();
    adminsAssertions.shouldShowEmptyMessage(EMPTY_MESSAGE);
  }

  @Test
  @Tag("regression")
  public void testAdministratorsTable_loadErrorWithRetryButton() {
    page.route(ROLE_MEMBERSHIPS_URL, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType("application/json")
        .setBody(ERROR_RESPONSE)));

    playwrightRefreshOrOpen(AdministratorsPage.url());

    adminsAssertions.shouldShowContainer();
    adminsAssertions.shouldShowErrorWithRetryButton();
  }
}
