/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module

class RoleEditorPage
extends BasePage {

  static url = "assets/index.html#/roles"

  static at = { permissionCategories.size() > 0 }

  static content = {

    rolesCrumb { $('.nav-crumb a') }

    pageTitle { $('#role-title') }

    deleteRole (required: false) { $('#delete-role') }
    deleteConfirm (required : false) { $('.modal.in .btn-danger') }

    nameEditor { $('input[type=text]') }

    descriptionEditor { $('textarea') }

    cancel { $('button.btn-cancel') }
    save { $('button.btn-primary') }

    permissionCategories(required: false) {
      moduleList DisplayedPermissionCategory, $('tbody[ng-repeat="permissionCategory in dirtyRole.permissionCategories"]')
    }
    permissionCategory { String name ->
      module DisplayedPermissionCategory, permissionCategories.find { it.groupName.text() == name }
    }
  }
}

class DisplayedPermissionCategory
extends Module {
  static content = {
    groupName { $('h3') }
    permissions { moduleList DisplayedPermission, $('tr[ng-repeat="permission in permissionCategory.permissions"]') }
    permission { index -> module DisplayedPermission, permissions.getAt(index) }
  }
}

class DisplayedPermission
extends Module {
  static content = {
    toggleSwitch { module ToggleSwitch, $('.toggle-checkbox') }
    name { $('label > span') }
    description { $('td', 1) }
  }
}

class ToggleSwitch
extends Module {
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
