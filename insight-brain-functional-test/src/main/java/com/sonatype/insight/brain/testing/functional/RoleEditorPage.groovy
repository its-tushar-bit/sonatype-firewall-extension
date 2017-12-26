/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module
import geb.module.FormElement

class RoleEditorPage
extends BasePage {

  static url = "assets/index.html#/roles"

  static at = { permissionCategories.size() > 0 }

  static content = {

    rolesCrumb { $('.nav-crumb a') }

    pageTitle { $('#role-title') }

    deleteRole (required: false) { $('#delete-role').module(FormElement) }
    deleteConfirm (required : false) { $('.modal.in .btn-primary') }

    nameEditor { $('input[type=text]').module(FormElement) }
    namePopover (required : false) { $('#roleName-popover') }

    descriptionEditor { $('textarea') }

    cancel { $('button.btn-cancel').module(FormElement) }
    save { $('button.btn-primary').module(FormElement) }

    permissionCategories(required: false) {
      $('tbody[ng-repeat="permissionCategory in dirtyRole.permissionCategories"]').moduleList(DisplayedPermissionCategory)
    }
    permissionCategory { String name ->
      permissionCategories.find { it.groupName.text() == name }.module(DisplayedPermissionCategory)
    }
  }
}

class DisplayedPermissionCategory
extends Module {
  static content = {
    groupName { $('h3') }
    permissions { $('tr[ng-repeat="permission in permissionCategory.permissions"]').moduleList(DisplayedPermission) }
    permission { index -> permissions.getAt(index) }
  }
}

class DisplayedPermission
extends Module {
  static content = {
    toggleSwitch { $('.toggle-checkbox').module(ToggleSwitch) }
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
    toggleCheckbox { toggle.find('input').module(FormElement) }
  }
}
