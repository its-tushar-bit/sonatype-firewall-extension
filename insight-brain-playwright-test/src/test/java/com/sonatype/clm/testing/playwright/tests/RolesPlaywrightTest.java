/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.RolesPage;
import com.sonatype.clm.testing.playwright.pages.RolesPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;

import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright regression tests for the Roles management page (Administration → Roles),
 * Create Role form, Edit Role form, and Delete Role modal.
 */
public class RolesPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ROLE_NAME_PREFIX = "pw-reg-role";

  private static final String ROLE_DESCRIPTION = "Playwright test role";

  private static final String BUILTIN_SYSTEM_ADMIN_NAME = "System Administrator";

  private static final String BUILTIN_POLICY_ADMIN_NAME = "Policy Administrator";

  private static final String RESTRICTED_USER_PREFIX = "pw-roles-viewer";

  private static final String RESTRICTED_USER_EMAIL_DOMAIN = "test.local";

  private static final String PERMISSION_CATEGORY_IQ = "IQ";

  private static final String INFO_ALERT_PROMPT = "Looking for how to assign a user to a role?";

  private static final String INFO_ALERT_DOCS_LINK_TEXT = "Docs";

  private static final String CUSTOM_ROLES_EMPTY_MESSAGE =
      "No custom roles defined. Click \"Create Role\" in the upper right to add one.";

  /** Combined message for missing-name OR missing-description (RoleEditor.jsx:60-66). */
  private static final String VALIDATION_ERROR_INVALID_OR_MISSING =
      "Unable to submit: fields with invalid or missing data.";

  private RolesPage rolesPage;

  private RolesPageAssertions assertions;

  /** Always flip via {@link #switchToRestrictedUser} — setting after login leaks the session if login throws. */
  private boolean switchedToRestrictedUser;

  @Before
  public void openRolesPage() {
    playwrightRefreshOrOpen(RolesPage.url());
    playwrightLogin();

    rolesPage = new RolesPage();
    assertions = new RolesPageAssertions(rolesPage);
  }

  /** {@code @Rule} (not {@code @After}) runs deterministically vs the parent class teardown. */
  @Rule
  public final ExternalResource restoreAdminSession = new ExternalResource()
  {
    @Override
    protected void after() {
      if (switchedToRestrictedUser) {
        playwrightHardreset();
        playwrightLoginAdminAt(RolesPage.url());
      }
    }
  };

  @Test
  @Category(RegressionTest.class)
  public void testRolesPage_renders() {
    assertions.shouldShowContainer();
    assertions.shouldShowPageTitle("Roles");
    assertions.shouldShowCreateRoleButton();
    assertions.shouldShowBuiltInRoles();
    assertions.shouldListRole(BUILTIN_SYSTEM_ADMIN_NAME);
    assertions.shouldShowRoleDescription(BUILTIN_SYSTEM_ADMIN_NAME);
    assertions.shouldListRole(BUILTIN_POLICY_ADMIN_NAME);
    assertions.shouldShowRoleDescription(BUILTIN_POLICY_ADMIN_NAME);
    assertions.shouldShowCustomRolesEmpty();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRolesPage_navigateToCreateRole() {

    assertions.shouldShowContainer();
    rolesPage.clickCreateRole();

    assertions.shouldShowRoleEditor();
    assertions.shouldShowRoleEditorTitle("Create a Role");
    assertions.shouldShowRoleNameInput();
    assertions.shouldShowRoleDescriptionInput();
    assertions.shouldHaveEmptyRoleNameInput();
    assertions.shouldHaveEmptyRoleDescriptionInput();
    assertions.shouldShowPermissionsHeading();
    assertions.shouldShowPermissionCategory(PERMISSION_CATEGORY_IQ);
    assertions.shouldHavePermissionToggles(PERMISSION_CATEGORY_IQ);
  }

  @Test
  @Category(RegressionTest.class)
  public void testCreateRole_fieldValidation() {
    playwrightRefreshOrOpen(RolesPage.urlToCreateRole());

    assertions.shouldShowRoleEditor();
    assertions.shouldShowRoleNameInput();

    rolesPage.roleDescriptionInput().fill("Some description");
    rolesPage.clickSave();
    assertions.shouldShowRoleFormValidationErrorsContaining(VALIDATION_ERROR_INVALID_OR_MISSING);
    assertions.shouldShowRoleEditor();

    String uniqueRoleName = ROLE_NAME_PREFIX + TemporaryEntity.uuid();
    rolesPage.roleNameInput().fill(uniqueRoleName);
    rolesPage.roleDescriptionInput().fill("");
    rolesPage.clickSave();
    assertions.shouldShowRoleFormValidationErrorsContaining(VALIDATION_ERROR_INVALID_OR_MISSING);
    assertions.shouldShowRoleEditor();

    playwrightRefreshOrOpen(RolesPage.url());
    assertions.shouldNotListRole(uniqueRoleName);
  }

  @Test
  @Category(RegressionTest.class)
  public void testCreateRole_permissionCategories() {
    playwrightRefreshOrOpen(RolesPage.urlToCreateRole());

    assertions.shouldShowRoleEditor();
    assertions.shouldShowPermissionsHeading();
    assertions.shouldShowPermissionCategory("Administrator");
    assertions.shouldHavePermissionToggles("Administrator");
    assertions.shouldShowPermissionCategory("Remediation");
    assertions.shouldHavePermissionToggles("Remediation");
    assertions.shouldShowPermissionCategory(PERMISSION_CATEGORY_IQ);
    assertions.shouldHavePermissionToggles(PERMISSION_CATEGORY_IQ);

    rolesPage.firstPermissionToggle(PERMISSION_CATEGORY_IQ).click();
    assertThat(rolesPage.firstPermissionToggle(PERMISSION_CATEGORY_IQ).locator("input"))
        .isChecked();
  }

  @Test
  @Category(RegressionTest.class)
  public void testCreateRole_saveSuccessfully() {
    String uniqueRoleName = ROLE_NAME_PREFIX + TemporaryEntity.uuid();

    assertions.shouldShowContainer();
    rolesPage.clickCreateRole();
    assertions.shouldShowRoleEditor();

    rolesPage.fillRoleDetails(uniqueRoleName, ROLE_DESCRIPTION);
    rolesPage.firstPermissionToggle(PERMISSION_CATEGORY_IQ).click();
    rolesPage.clickSave();
    waitForSubmitMask();
    rolesPage.waitForRolesListRoute();

    assertions.shouldShowContainer();
    assertions.shouldListRole(uniqueRoleName);
  }

  /** {@code NxStatefulForm} only renders the (mount-time) read-only error after a submit attempt. */
  @Test
  @Category(RegressionTest.class)
  public void testEditRole_builtInRoleIsReadOnly() {
    assertions.shouldShowContainer();
    rolesPage.openRoleForEdit(BUILTIN_SYSTEM_ADMIN_NAME);

    assertions.shouldShowRoleEditor();
    assertions.shouldShowRoleNameInput();
    assertions.shouldShowRoleEditorTitle("Edit a Role");
    assertions.shouldHaveRoleNameDisabled();
    assertions.shouldHaveRoleDescriptionDisabled();
    assertions.shouldHaveDeleteButtonDisabled();
    assertions.shouldHaveFirstPermissionToggleDisabled(PERMISSION_CATEGORY_IQ);

    rolesPage.clickSave();
    assertions.shouldShowRoleFormValidationErrorsContaining("This role cannot be edited");
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditRole_insufficientPermissionsReadOnly() {
    String customRoleName = ROLE_NAME_PREFIX + TemporaryEntity.uuid();
    Role customRole = tempEntity.newRole(customRoleName, ROLE_DESCRIPTION, false);

    String restrictedUser = RESTRICTED_USER_PREFIX + "-" + TemporaryEntity.uuid();
    Role viewOnlyRole = tempEntity.newRole(true, Permission.VIEW_ROLES);
    tempEntity.newUser(restrictedUser, "View", "Only", restrictedUser + "@" + RESTRICTED_USER_EMAIL_DOMAIN);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, viewOnlyRole.getId(), restrictedUser);

    playwrightLogout();
    switchToRestrictedUser(restrictedUser, TemporaryEntity.USER_PASSWORD_CLEAR);
    playwrightRefreshOrOpen(RolesPage.urlToEditRole(customRole.getId()));

    assertions.shouldShowRoleEditor();
    assertions.shouldShowRoleNameInput();
    assertions.shouldHaveRoleNameDisabled();
    assertions.shouldHaveRoleDescriptionDisabled();
    assertions.shouldHaveDeleteButtonDisabled();
    assertions.shouldHaveFirstPermissionToggleDisabled(PERMISSION_CATEGORY_IQ);

    rolesPage.clickSave();
    assertions.shouldShowRoleFormValidationErrorsContaining(
        "You have insufficient permissions to edit this role");
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteRole_confirmAndDelete() {
    String customRoleName = ROLE_NAME_PREFIX + TemporaryEntity.uuid();
    Role customRole = tempEntity.newRole(customRoleName, ROLE_DESCRIPTION, false);

    playwrightRefreshOrOpen(RolesPage.urlToEditRole(customRole.getId()));

    assertions.shouldShowRoleEditor();

    rolesPage.openDeleteModal();
    assertions.shouldShowDeleteModal();
    assertions.shouldShowDeleteWarning();
    assertions.shouldShowDeleteWarningContaining("permanently remove");

    rolesPage.confirmDelete();
    waitForSubmitMask();
    rolesPage.waitForRolesListRoute();

    assertions.shouldShowContainer();
    assertions.shouldNotListRole(customRoleName);
  }

  @Test
  @Category(RegressionTest.class)
  public void testRolesList_infoAlertWithDocsLink() {
    assertions.shouldShowContainer();
    assertions.shouldShowInfoAlertWithDocsLink(INFO_ALERT_PROMPT, INFO_ALERT_DOCS_LINK_TEXT);
  }

  /**
   * The read-only page branch is reached by logging in as a user with {@code VIEW_ROLES} but no
   * {@code EDIT_ROLES}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testRolesList_createRoleDisabledForReadOnlyUser() {
    String restrictedUser = RESTRICTED_USER_PREFIX + "-" + TemporaryEntity.uuid();
    Role viewOnlyRole = tempEntity.newRole(true, Permission.VIEW_ROLES);
    tempEntity.newUser(restrictedUser, "View", "Only", restrictedUser + "@" + RESTRICTED_USER_EMAIL_DOMAIN);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, viewOnlyRole.getId(), restrictedUser);

    playwrightLogout();
    switchToRestrictedUser(restrictedUser, TemporaryEntity.USER_PASSWORD_CLEAR);
    playwrightRefreshOrOpen(RolesPage.url());

    assertions.shouldShowContainer();
    assertions.shouldHaveCreateRoleButtonDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRolesList_builtInAndCustomSubtitles() {
    assertions.shouldShowContainer();
    assertions.shouldShowBuiltInAndCustomSubtitles();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRolesList_customRolesEmptyMessageFullText() {
    assertions.shouldShowContainer();
    assertions.shouldShowCustomRolesEmptyMessage(CUSTOM_ROLES_EMPTY_MESSAGE);
  }

  @Test
  @Category(RegressionTest.class)
  public void testRolesList_roleItemsAreAnchorLinks() {
    String customRoleName = ROLE_NAME_PREFIX + TemporaryEntity.uuid();
    Role customRole = tempEntity.newRole(customRoleName, ROLE_DESCRIPTION, false);

    playwrightRefreshOrOpen(RolesPage.url());

    assertions.shouldShowContainer();
    assertions.shouldRenderRoleItemAsAnchorLink(customRoleName);

    rolesPage.roleItemAnchor(rolesPage.roleItem(customRoleName).first()).click();
    assertThat(page).hasURL(RolesPage.editRoleUrlPattern(customRole.getId()));
  }

  /**
   * Simulates a roles-list 500 via {@code page.route} because the embedded IQ server has no
   * supported hook to force this endpoint into a 500; the test's purpose is specifically the
   * {@code NxLoadWrapper} error chrome and Retry flow.
   */
  @Test
  @Category(RegressionTest.class)
  public void testRolesList_loadErrorWithRetry() {
    Pattern rolesEndpoint = Pattern.compile(".*/api/v2/roles.*");
    page.route(rolesEndpoint, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType("application/json")
        .setBody("{\"message\":\"Simulated server error\"}")));

    playwrightRefresh();
    assertThat(rolesPage.loadError()).isVisible();
    assertThat(rolesPage.retryButton()).isVisible();

    page.unroute(rolesEndpoint);
    rolesPage.retryButton().click();

    assertThat(rolesPage.loadError()).isHidden();
    assertions.shouldShowContainer();
    assertions.shouldShowBuiltInRoles();
  }

  /** Flag set before login so a login throw still triggers cleanup in {@link #restoreAdminSession}. */
  private void switchToRestrictedUser(String username, String password) {
    switchedToRestrictedUser = true;
    playwrightLogin(username, password);
  }
}
