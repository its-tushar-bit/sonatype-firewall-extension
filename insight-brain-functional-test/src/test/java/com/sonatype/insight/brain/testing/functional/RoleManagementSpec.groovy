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
    at RoleManagementPage

    then: 'the list of roles is present'
    roleItems.size() > 0

    and: 'the list of roles is sorted properly'
    roleName(0) == 'System Administrator'
    roleName(1) == 'CLM Administrator'
    roleName(2) == 'Owner'
    roleName(3) == 'Developer'
    roleName(4) == 'Application Evaluator'
    roleName(5) == 'Component Evaluator'
  }

  def 'Clicking on a role should display the role editor.'() {
    when: 'clicking on the developer role'
    RoleManagementPage roleManagementPage = at RoleManagementPage
    roleManagementPage.roleItems[3].click()

    then: 'the read only role editor is shown'
    waitFor {
      roleManagementPage.permissionCategories.size() == 2
    }

    roleManagementPage.pageTitle.text() == 'Developer'

    PermissionCategory policyCategory = roleManagementPage.permissionCategory('CLM')
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

    PermissionCategory systemCategory = roleManagementPage.permissionCategory('Administrator')
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
}
