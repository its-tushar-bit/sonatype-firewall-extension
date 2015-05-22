/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

@Stepwise
class RoleManagementSpec
extends BaseSpec {
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
    roleManagementPage.builtinRoles[1].name.text() == 'CLM Administrator'
    roleManagementPage.builtinRoles[2].name.text() == 'Owner'
    roleManagementPage.builtinRoles[3].name.text() == 'Developer'
    roleManagementPage.builtinRoles[4].name.text() == 'Application Evaluator'
    roleManagementPage.builtinRoles[5].name.text() == 'Component Evaluator'
  }

  def 'Clicking on a role should display the role editor.'() {
    when: 'clicking on the developer role'
    RoleManagementPage roleManagementPage = at RoleManagementPage
    roleManagementPage.builtinRoles[3].click()

    then: 'the read only role editor is shown'
    RoleEditorPage roleEditorPage = at RoleEditorPage
    waitFor {
      roleEditorPage.permissionCategories.size() == 2
    }

    roleEditorPage.pageTitle.text() == 'Developer'

    PermissionCategory policyCategory = roleEditorPage.permissionCategory('CLM')
    policyCategory.permissions.size() == 5

    Permission claimComponentPermission = policyCategory.permission(0)
    !claimComponentPermission.toggleSwitch.isOn()
    !claimComponentPermission.toggleSwitch.isEnabled()
    claimComponentPermission.name.text() == 'Claim'
    claimComponentPermission.description.text() == 'Components'

    Permission evaluateAppPermission = policyCategory.permission(3)
    !evaluateAppPermission.toggleSwitch.isOn()
    !evaluateAppPermission.toggleSwitch.isEnabled()
    evaluateAppPermission.name.text() == 'Evaluate'
    evaluateAppPermission.description.text() == 'Applications'

    Permission evaluateComponentPermission = policyCategory.permission(4)
    evaluateComponentPermission.toggleSwitch.isOn()
    !evaluateComponentPermission.toggleSwitch.isEnabled()
    evaluateComponentPermission.name.text() == 'Evaluate'
    evaluateComponentPermission.description.text() == 'Individual components'

    Permission viewPermission = policyCategory.permission(2)
    viewPermission.toggleSwitch.isOn()
    !viewPermission.toggleSwitch.isEnabled()
    viewPermission.name.text() == 'View'
    viewPermission.description.text() == 'CLM elements'

    Permission writePermission = policyCategory.permission(1)
    !writePermission.toggleSwitch.isOn()
    !writePermission.toggleSwitch.isEnabled()
    writePermission.name.text() == 'Edit'
    writePermission.description.text() == 'CLM elements'

    PermissionCategory systemCategory = roleEditorPage.permissionCategory('Administrator')
    systemCategory.permissions.size() == 4

    Permission administratorPermission = systemCategory.permission(0)
    !administratorPermission.toggleSwitch.isOn()
    !administratorPermission.toggleSwitch.isEnabled()
    administratorPermission.name.text() == 'Edit'
    administratorPermission.description.text() == 'System Configuration and Users'

    Permission permission = systemCategory.permission(3)
    !permission.toggleSwitch.isOn()
    !permission.toggleSwitch.isEnabled()
    permission.name.text() == 'Edit'
    permission.description.text() == 'Proprietary Components'
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

    when: 'enter the role name'
    roleEditorPage.nameEditor << 'peon'

    then: 'save button is still disabled'
    roleEditorPage.save.disabled

    when: 'enter the role description'
    roleEditorPage.descriptionEditor << 'bottom rung'

    and: 'clicking save'
    roleEditorPage.save.click()

    then: 'role is saved'
    at RoleManagementPage
    roleManagementPage.customRoles.size() == 1
    roleManagementPage.customRoles[0].name.text() == 'peon'
    roleManagementPage.customRoles[0].description.text() == 'bottom rung'

    when: 'click on peon role'
    roleManagementPage.customRoles[0].click();

    then: 'opens role editor'
    at RoleEditorPage
    waitFor {
      roleEditorPage.pageTitle.text() == 'peon'
    }

    when: 'update fields'
    roleEditorPage.nameEditor = 'peons'
    roleEditorPage.descriptionEditor = 'not even on the ladder'
    roleEditorPage.save.click()

    then: 'updated role is visible'
    at RoleManagementPage
    roleManagementPage.customRoles.size() == 1
    roleManagementPage.customRoles[0].name.text() == 'peons'
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
}
