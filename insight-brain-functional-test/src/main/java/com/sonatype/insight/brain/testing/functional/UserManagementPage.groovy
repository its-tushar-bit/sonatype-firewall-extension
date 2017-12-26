/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ClmModalModule

import geb.module.FormElement

class UserManagementPage
    extends BasePage
{
  static url = "assets/index.html#/users"

  static at = { newUserButton.displayed }

  static content = {
    newUserButton(wait: true) { $('#user-new') }
    userForm(required: false) { $('form[id$="user-form"]') }

    firstNameInput(required: false) { userForm.firstName() }
    lastNameInput(required: false) { userForm.lastName() }
    emailInput(required: false) { userForm.email() }
    usernameInput(required: false) { userForm.username() }
    passwordInput(required: false) { userForm.password() }
    passwordValidateInput(required: false) { userForm.passwordValidate() }

    save(required: false) { $('button[id$="user-form-save"]').module(FormElement) }
    cancel(required: false) { $('button[id$="user-form-cancel"]').module(FormElement) }
    headers(required: false) { $('a.accordion-toggle') }
    header(required: false) { index -> $('a.accordion-toggle', index) }
    currentUsers(required: false) { $('span[ng-if="isCurrentUser(user)"]').parent().find('h4') }
    editUserButton(required: false) { index -> header(index).parent().find('button[ng-click="editClick(user)"]') }
    deleteUserButton(required: false) { index -> header(index).parent().find('button[ng-click="removeClick(user)"]') }
    resetUserButton(required: false) {
      index -> header(index).parent().find('button[ng-click="resetPasswordClick(user)"]')
    }

    deleteModal { module(new ClmModalModule(title: 'Delete User')) }

    resetModal { module(new ClmModalModule(title: 'Reset Password', confirmText: 'Reset')) }
    newPasswordField(required: false) { $('#generatedPassword') }

    summarySection { index -> $('div.accordion-inner', index) }

    editPanelForm(required: false) { index -> $('.accordion-body', index).find('form') }
    editFirstNameInput(required: false) { index -> editPanelForm(index).find('input[name=firstName]') }
    editLastNameInput(required: false) { index -> editPanelForm(index).find('input[name=lastName]') }
    editEmailInput(required: false) { index -> editPanelForm(index).find('input[name=email]') }
    editSave(required: false) { index -> editPanelForm(index).find('button[ng-click="vm.saveClick(vm.user)"]') }
  }
}
