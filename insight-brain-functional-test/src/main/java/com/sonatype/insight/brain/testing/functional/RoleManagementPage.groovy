/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module

class RoleManagementPage
    extends BasePage
{
  static url = "assets/index.html#/roles"

  static at = { roleItems.size() > 0 }

  static content = {
    pageTitle { $('h1.page-title') }
    roleItems(wait: true) { $('.role-item') }
    roleName { index -> roleItems.getAt(index).find('.role-summary > h3').text() }
    
    permissionCategories(required: false) {
      moduleList PermissionCategory, $('tbody[ng-repeat="permissionCategory in permissionCategories"]')
    }
    permissionCategory { String name -> module PermissionCategory, permissionCategories.find { it.groupName.text() == name } }
  }
}

class PermissionCategory
    extends Module
{
  static content = {
    groupName { $('h3') }
    permissions { moduleList Permission, $('tr[ng-repeat="permission in permissionCategory.permissions"]') }
    permission { index -> module Permission, permissions.getAt(index) }
  }
}

class Permission
    extends Module
{
  static content = {
    toggleSwitch { module ToggleSwitch, $('.toggle-checkbox') }
    name { $('label > span') }
    description { $('td', 1) }
  }
}

class ToggleSwitch
    extends Module
{
  boolean isOn() {
    return toggleCheckbox.value() == "on"
  }

  boolean isEnabled() {
    return toggleCheckbox.isEnabled()
  }

  static content = {
    toggle { $('.toggle') }
    label { $('span:not(.toggle-handle)') }
    toggleCheckbox { toggle.find('input') }
  }
}
