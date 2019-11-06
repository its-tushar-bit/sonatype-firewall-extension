/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.RoleDAO
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO
import com.sonatype.insight.brain.model.security.MembershipMapping
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.PermissionCategory
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.model.security.User

import spock.lang.Stepwise

@Stepwise
class RoleManagementSpec
extends BaseSpec {
  static final boolean ON = true
  static final boolean ENABLED = true

  @Override
  def setupSpec() {
    loginAsAdminVia(RoleManagementPage)
  }

  def "Arriving at role management page we should see the list of roles."() {
    when: 'first viewing the page'
    RoleManagementPage roleManagementPage = at RoleManagementPage

    then: 'the list of roles is present'
    roleManagementPage.builtinRoles.size() > 0

    and: 'the list of roles is sorted properly'
    roleManagementPage.builtinRoles[0].name.text() == 'System Administrator'
    roleManagementPage.builtinRoles[1].name.text() == 'Policy Administrator'
    roleManagementPage.builtinRoles[2].name.text() == 'Owner'
    roleManagementPage.builtinRoles[3].name.text() == 'Developer'
    roleManagementPage.builtinRoles[4].name.text() == 'Application Evaluator'
    roleManagementPage.builtinRoles[5].name.text() == 'Component Evaluator'
  }

  def 'Clicking on a role should display the role editor.'() {
    when: 'clicking on the developer role'
    RoleManagementPage roleManagementPage = at RoleManagementPage
    roleManagementPage.builtinRoles[3].click()

    RoleEditorPage roleEditorPage = at RoleEditorPage
    DisplayedPermissionCategory policyCategory = roleEditorPage.permissionCategory(PermissionCategory.IQ.displayName) as DisplayedPermissionCategory
    interact { moveToElement(policyCategory.permission(7)) }

    then: 'the read only role editor is shown'
    waitFor {
      roleEditorPage.permissionCategories.size() == 2
    }

    roleEditorPage.pageTitle.text() == 'Developer'

    policyCategory.permissions.size() == 9
    assertPermission(policyCategory.permission(0), !ON, !ENABLED, Permission.MANAGE_PROPRIETARY)
    assertPermission(policyCategory.permission(1), !ON, !ENABLED, Permission.CLAIM_COMPONENT)
    assertPermission(policyCategory.permission(2), !ON, !ENABLED, Permission.WRITE)
    assertPermission(policyCategory.permission(3), ON, !ENABLED, Permission.READ)
    assertPermission(policyCategory.permission(4), !ON, !ENABLED, Permission.EVALUATE_APPLICATION)
    assertPermission(policyCategory.permission(5), ON, !ENABLED, Permission.EVALUATE_COMPONENT)
    assertPermission(policyCategory.permission(6), !ON, !ENABLED, Permission.ADD_APPLICATION)
    assertPermission(policyCategory.permission(7), !ON, !ENABLED, Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)

    DisplayedPermissionCategory systemCategory = roleEditorPage.permissionCategory(PermissionCategory.ADMINISTRATOR.displayName) as DisplayedPermissionCategory
    systemCategory.permissions.size() == 3
    assertPermission(systemCategory.permission(0), !ON, !ENABLED, Permission.CONFIGURE_SYSTEM)
  }

  def 'Custom Roles'() {
    when: 'lingering on the role page'
    RoleManagementPage roleManagementPage = to RoleManagementPage

    and: 'clicking create role'
    roleManagementPage.createRole.click()

    then: 'on new role editor'
    RoleEditorPage roleEditorPage = at RoleEditorPage
    waitFor {
      roleEditorPage.pageTitle.text() == 'New Role'
    }
    roleEditorPage.save.disabled
    !roleEditorPage.deleteRole.present

    when: 'setting permission for claiming'
    String permissionDescription = roleEditorPage.permissionCategory(PermissionCategory.IQ.displayName).permission(0).description.text()
    roleEditorPage.permissionCategory(PermissionCategory.IQ.displayName).permission(0).toggleSwitch.toggle.click()

    and: 'enters a duplicate name'
    roleEditorPage.nameEditor << 'Owner'

    then: 'error is shown'
    roleEditorPage.namePopover.displayed
    roleEditorPage.namePopover.text() == 'Name is already in use'

    when: 'enter the role name'
    roleEditorPage.nameEditor = 'peon'

    then: 'save button is still disabled'
    roleEditorPage.save.disabled

    when: 'enter the role description'
    roleEditorPage.descriptionEditor << 'bottom rung'
    interact {
      moveToElement(roleEditorPage.save)
    }

    then: 'save button is enabled'
    roleEditorPage.save.enabled

    when: 'clicking save'
    roleEditorPage.save.click()

    then: 'role is saved'
    at RoleManagementPage
    waitFor { roleManagementPage.customRoles.size() == 1 }
    roleManagementPage.customRoles[0].name.text() == 'peon'
    roleManagementPage.customRoles[0].description.text() == 'bottom rung'
    roleHasPermission('peon', permissionDescription)


    when: 'click on peon role'
    roleManagementPage.customRoles[0].click();

    then: 'opens role editor'
    at RoleEditorPage
    waitFor {
      roleEditorPage.pageTitle.text() == 'peon'
    }
    roleEditorPage.save.disabled

    when: 'update fields'
    roleEditorPage.nameEditor = 'peons'
    roleEditorPage.descriptionEditor = 'not even on the ladder'

    then: 'save is enabled'
    waitFor { roleEditorPage.save.enabled }

    when: 'save is clicked'
    interact {
      moveToElement(roleEditorPage.save)
    }
    roleEditorPage.save.click()

    then: 'updated role is visible'
    at RoleManagementPage
    roleManagementPage.customRoles.size() == 1
    waitFor { roleManagementPage.customRoles[0].name.text() == 'peons' }
    roleManagementPage.customRoles[0].description.text() == 'not even on the ladder'

    when: 'click on peon role'
    roleManagementPage.customRoles[0].click();

    then: 'editor opens'
    at RoleEditorPage

    when: 'clicking delete'
    roleEditorPage.deleteRole.click();

    then: 'confirmation dialog is shown'
    waitFor { roleEditorPage.deleteConfirm.displayed }

    when: 'delete role'
    roleEditorPage.deleteConfirm.click()

    then: 'role is removed'
    at RoleManagementPage
    roleManagementPage.customRoles.size() == 0
  }

  def 'underprivileged user sees readOnly custom roles'() {
    given:
    Role role = temporaryEntity.newRole(false, com.sonatype.insight.brain.model.security.Permission.VIEW_ROLES)
    User user = temporaryEntity.newUser()
    temporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername())
    userOptions.logoutClick()
    loginAsUserVia(user.getUsername(), user.getPassword(), RoleManagementPage)
    RoleManagementPage roleManagementPage = to RoleManagementPage

    expect: 'button "Create Role" to be disabled'
    roleManagementPage.createRole.disabled

    when: 'user clicks on a custom role'
    customRoles[0].click()

    then: 'user is presented with read only view of the role'
    at RoleEditorPage
    pageTitle.text() == role.getName()
    save.disabled
    nameEditor.disabled
    deleteRole.disabled

    cleanup:
    userOptions.logoutClick()
    new RoleDAO().delete(role)
  }

  def 'user presented with unsaved changes dialog'() {
    given:
    Role role = temporaryEntity.newRole(false, com.sonatype.insight.brain.model.security.Permission.VIEW_ROLES)
    loginAsAdminVia(RoleManagementPage)
    RoleManagementPage roleManagementPage = to RoleManagementPage

    when: 'user clicks on a custom role'
    customRoles[0].click()

    then: 'user is presented with editable view of the role'
    at RoleEditorPage
    pageTitle.text().endsWith(role.getName())
    waitFor { !nameEditor.disabled }

    when: 'user changes the name and attempts to leave page'
    nameEditor = 'newname'
    systemConfig.dropdown.click()
    waitFor { systemConfig.manageUsers.present }
    systemConfig.manageUsers.click()

    then: 'user is presented with the unsaved changes dialog'
    waitFor { unsavedModal.displayed }

    cleanup:
    new RoleDAO().delete(role)
  }

  boolean roleHasPermission(String roleName, String permission) {
    Role role = new RoleDAO().getByName(roleName);
    if (role != null) {
      for (com.sonatype.insight.brain.model.security.Permission perm : new RolePermissionDAO().getPermissionsForRole(role.getId())) {
        if (perm.getDescription().equals(permission)) {
          return true;
        }
      }
    }
    return false;
  }

  void assertPermission(displayedPermission, boolean isOn, boolean isEnabled, Permission permission) {
    assert displayedPermission.toggleSwitch.isOn() == isOn
    assert displayedPermission.toggleSwitch.isEnabled() == isEnabled
    assert displayedPermission.name.text() == permission.displayName
    assert displayedPermission.description.text() == permission.description
  }
}
