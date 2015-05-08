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

    PermissionCategory policyCategory = roleManagementPage.permissionCategory('Policy')
    policyCategory.permissions.size() == 4

    Permission evaluateAppPermission = policyCategory.permission('Evaluate Application')
    !evaluateAppPermission.toggleSwitch.isOn()
    !evaluateAppPermission.toggleSwitch.isEnabled()

    Permission evaluateComponentPermission = policyCategory.permission('Evaluate Component')
    evaluateComponentPermission.toggleSwitch.isOn()
    !evaluateComponentPermission.toggleSwitch.isEnabled()

    Permission viewPermission = policyCategory.permission('View')
    viewPermission.toggleSwitch.isOn()
    !viewPermission.toggleSwitch.isEnabled()

    Permission writePermission = policyCategory.permission('Write')
    !writePermission.toggleSwitch.isOn()
    !writePermission.toggleSwitch.isEnabled()

    PermissionCategory systemCategory = roleManagementPage.permissionCategory('System Configuration')
    systemCategory.permissions.size() == 1

    Permission administratorPermission = systemCategory.permission('Administrator')
    !administratorPermission.toggleSwitch.isOn()
    !administratorPermission.toggleSwitch.isEnabled()
  }
}
