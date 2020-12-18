/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.RoleEditorPage;
import com.sonatype.clm.testing.functional.pages.RoleManagementPage;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.PermissionCategory;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RoleManagementTest
    extends AbstractFunctionalTest
{
  private static final boolean ON = true;

  private static final boolean ENABLED = true;

  private static final String[] BUILTIN_ROLES = new String[] {
      "System Administrator",
      "Policy Administrator",
      "Owner",
      "Developer",
      "Application Evaluator",
      "Component Evaluator"
  };

  @Before
  public void initialLogin() {
    refreshOrOpen(RoleManagementPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testPageLoadHasTheRightRoles() {
    RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.pageTitle().shouldBe(visible).shouldHave(text("Roles"));
    roleManagementPage.builtinRoles().shouldHaveSize(7);

    // verify that the roles are sorted in the right order
    for (int i = 0; i < BUILTIN_ROLES.length; i++) {
      roleManagementPage.builtinRoles().get(i).shouldHave(text(BUILTIN_ROLES[i]));
    }

    // no custom roles on initial load
    roleManagementPage.customRoles().shouldHaveSize(0);
  }

  @Test
  public void testRoleClickDisplaysEditorWithTheRightPermissions() {
    int roleId = 3; // Developer role

    RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.builtinRole(roleId).shouldBe(visible).click();

    // Role editor page displays the right role
    RoleEditorPage roleEditorPage = new RoleEditorPage();
    roleEditorPage.pageTitle().shouldBe(visible).shouldHave(text(BUILTIN_ROLES[roleId]));

    // verify there are three permission categories
    roleEditorPage.permissionCategories().shouldHaveSize(3);

    // Administrator permission category
    String adminDisplayName = PermissionCategory.ADMINISTRATOR.getDisplayName();
    RoleEditorPage.PermissionCategory adminPermissionCategory = roleEditorPage.permissionCategory(adminDisplayName);
    adminPermissionCategory.shouldBe(visible).shouldHave(text(adminDisplayName));

    // verify permissions under Administrator category, and that they are in right order
    roleEditorPage.permissions(adminDisplayName).shouldHaveSize(3);

    assertPermission(roleEditorPage.permission(adminDisplayName, 0),
        !ON, !ENABLED, Permission.CONFIGURE_SYSTEM);
    assertPermission(roleEditorPage.permission(adminDisplayName, 1),
        !ON, !ENABLED, Permission.EDIT_ROLES);
    assertPermission(roleEditorPage.permission(adminDisplayName, 2),
        !ON, !ENABLED, Permission.VIEW_ROLES);

    // IQ permission category
    String iqDisplayName = PermissionCategory.IQ.getDisplayName();
    RoleEditorPage.PermissionCategory iqPermissionCategory = roleEditorPage.permissionCategory(
        iqDisplayName);
    iqPermissionCategory.shouldBe(visible).shouldHave(text(iqDisplayName));

    // verify permissions under IQ category, and that they are in right order
    roleEditorPage.permissions(iqDisplayName).shouldHaveSize(10);

    assertPermission(roleEditorPage.permission(iqDisplayName, 0), !ON, !ENABLED, Permission.MANAGE_PROPRIETARY);
    assertPermission(roleEditorPage.permission(iqDisplayName, 1), !ON, !ENABLED, Permission.CLAIM_COMPONENT);
    assertPermission(roleEditorPage.permission(iqDisplayName, 2), !ON, !ENABLED, Permission.WRITE);
    assertPermission(roleEditorPage.permission(iqDisplayName, 3), ON, !ENABLED, Permission.READ);
    assertPermission(roleEditorPage.permission(iqDisplayName, 4), !ON, !ENABLED, Permission.EDIT_ACCESS_CONTROL);
    assertPermission(roleEditorPage.permission(iqDisplayName, 5), !ON, !ENABLED, Permission.EVALUATE_APPLICATION);
    assertPermission(roleEditorPage.permission(iqDisplayName, 6), ON, !ENABLED, Permission.EVALUATE_COMPONENT);
    assertPermission(roleEditorPage.permission(iqDisplayName, 7), !ON, !ENABLED, Permission.ADD_APPLICATION);
    assertPermission(roleEditorPage.permission(iqDisplayName, 8),
        !ON, !ENABLED, Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);
    assertPermission(roleEditorPage.permission(iqDisplayName, 9),
        !ON, !ENABLED, Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);

    // Remediation permission category
    String remediationDisplayName = PermissionCategory.REMEDIATION.getDisplayName();
    RoleEditorPage.PermissionCategory remediationPermissionCategory =
        roleEditorPage.permissionCategory(remediationDisplayName);
    remediationPermissionCategory.shouldBe(visible).shouldHave(text(remediationDisplayName));

    // verify permissions under Remediation category, and that they are in the right order
    roleEditorPage.permissions(remediationDisplayName).shouldHaveSize(4);

    assertPermission(roleEditorPage.permission(remediationDisplayName, 0), !ON, !ENABLED,
        Permission.WAIVE_POLICY_VIOLATIONS);
    assertPermission(roleEditorPage.permission(remediationDisplayName, 1), !ON, !ENABLED, Permission.CHANGE_LICENSES);
    assertPermission(roleEditorPage.permission(remediationDisplayName, 2), !ON, !ENABLED,
        Permission.CHANGE_SECURITY_VULNERABILITIES);
    assertPermission(roleEditorPage.permission(remediationDisplayName, 3), !ON, !ENABLED, Permission.LEGAL_REVIEWER);
  }

  @Test
  public void testCreateCustomRole() {
    // navigate to role page and click create role
    RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.createRole().click();
    roleManagementPage.pageTitle().shouldBe(visible).shouldHave(text("New Role"));

    // on new role editor
    RoleEditorPage roleEditorPage = new RoleEditorPage();
    roleEditorPage.save().shouldBe(disabled);
    roleEditorPage.deleteRole().shouldNotBe(visible);

    // setting permission
    RoleEditorPage.Permission permission = roleEditorPage.permission(PermissionCategory.IQ.getDisplayName(), 0);
    String permissionDescription = permission.description().text();
    permission.toggleSwitch().toggle().click();

    // enters a duplicate name, error is shown
    roleEditorPage.nameEditor().val("Owner");
    roleEditorPage.namePopover().shouldBe(visible).shouldHave(text("Name is already in use"));

    // enter a new role name
    String newRoleName = "new-role";
    String newRoleDescription = "new-role description";
    roleEditorPage.nameEditor().val(newRoleName);
    // save button is still disabled
    roleEditorPage.save().shouldBe(disabled);
    // enter the role description
    roleEditorPage.descriptionEditor().val(newRoleDescription);
    // save button is enabled
    roleEditorPage.save().scrollIntoView(true).shouldBe(enabled);

    // clicking save, should save the role
    roleEditorPage.save().click();
    roleManagementPage.customRoles().shouldHaveSize(1);
    roleManagementPage.customRole(0).name().shouldHave(text(newRoleName));
    roleManagementPage.customRole(0).description().shouldHave(text(newRoleDescription));
    roleHasPermission(newRoleName, permissionDescription);

    // perform an update
    String updateSuffix = "-update";
    roleManagementPage.customRole(0).click();
    roleEditorPage.pageTitle().shouldHave(text(newRoleName));
    roleEditorPage.save().shouldBe(disabled);

    roleEditorPage.nameEditor().val(newRoleName + updateSuffix);
    roleEditorPage.save().scrollIntoView(true).shouldBe(enabled);
    roleEditorPage.descriptionEditor().scrollIntoView(true).val(newRoleDescription + updateSuffix);
    roleEditorPage.save().scrollIntoView(true).click();

    // verify update is saved
    roleManagementPage.customRoles().shouldHaveSize(1);
    roleManagementPage.customRole(0).name().shouldHave(text(newRoleName + updateSuffix));
    roleManagementPage.customRole(0).description().shouldHave(text(newRoleDescription + updateSuffix));
    roleHasPermission(newRoleName, permissionDescription);

    // verify deletion of role
    roleManagementPage.customRole(0).click();
    roleEditorPage.deleteRole().click();
    roleEditorPage.deleteConfirm().shouldBe(visible).click();
    roleManagementPage.customRoles().shouldHaveSize(0);
  }

  @Test
  public void testUnderPrivilegedUser() {
    // as an admin, create a new role and map it to a new temp user
    Role role = tempEntity.newRole(false, Permission.VIEW_ROLES);
    User user = tempEntity.newUser();
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());

    // logout and log back in as the new user
    logout();
    refreshOrOpen(RoleManagementPage.url());
    login(user.getUsername(), user.getPassword());
    RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.createRole().shouldBe(disabled);

    // user clicks on a custom role, and is presented with read only view of the role
    roleManagementPage.customRole(0).click();
    RoleEditorPage roleEditorPage = new RoleEditorPage();
    roleEditorPage.pageTitle().shouldHave(text(role.getName()));
    roleEditorPage.save().shouldBe(disabled);
    roleEditorPage.nameEditor().shouldBe(disabled);
    roleEditorPage.deleteRole().shouldBe(disabled);

    // cleanup
    new RoleDAO().delete(role);
  }

  @Test
  public void testUnsavedChangesDialog() {
    RoleManagementPage roleManagementPage = new RoleManagementPage();
    roleManagementPage.createRole().click();

    // user adds a new role
    RoleEditorPage roleEditorPage = new RoleEditorPage();
    roleEditorPage.nameEditor().val("new role name");

    // user attempts to leave page
    SystemConfigMenu systemConfig = new SystemConfigMenu();
    systemConfig.dropdownToggle().shouldBe(visible).click();
    systemConfig.roles().should(appear).click();

    // user is presented with the unsaved changes dialog
    UnsavedModal unsavedModal = new UnsavedModal();
    unsavedModal.shouldBe(visible);
    unsavedModal.continueButton().click();
  }

  private void assertPermission(RoleEditorPage.Permission displayedPermission, boolean isOn, boolean isEnabled,
                                Permission permission)
  {
    displayedPermission.name().scrollIntoView(true);
    displayedPermission.toggleSwitch().toggleCheckbox().shouldBe(isEnabled ? enabled : disabled);
    if (isOn)
    {
      displayedPermission.toggleSwitch().toggleCheckbox().shouldBe(selected);
    }
    else
    {
      displayedPermission.toggleSwitch().toggleCheckbox().shouldNotBe(selected);
    }
    displayedPermission.name().shouldHave(text(permission.getDisplayName()));
    displayedPermission.description().shouldHave(text(permission.getDescription()));
  }

  private boolean roleHasPermission(String roleName, String permission) {
    Role role = new RoleDAO().getByName(roleName);
    if (role != null) {
      for (com.sonatype.insight.brain.model.security.Permission perm :
          new RolePermissionDAO().getPermissionsForRole(role.getId()))
      {
        if (perm.getDescription().equals(permission))
        {
          return true;
        }
      }
    }
    return false;
  }
}
