/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ModalModule
import com.sonatype.insight.brain.testing.functional.modules.ValidationModule


class UserManagementPage
    extends BasePage
{
  static url = "assets/index.html#/users"

  static at = { newUserButton.displayed }

  static content = {
    newUserButton(wait: true) { $('#user-new') }
    userForm(required: false) { $('#user-form') }

    firstNameInput(required: false) { userForm.firstName() }
    lastNameInput(required: false) { userForm.lastName() }
    emailInput(required: false) { userForm.email() }
    usernameInput(required: false) { userForm.username() }
    passwordInput(required: false) { userForm.password() }
    passwordValidateInput(required: false) { userForm.passwordValidate() }

    save(required: false) { $('#user-form-save') }
    cancel(required: false) { $('#user-form-cancel') }
    headers(required: false) { $('a.accordion-toggle') }
    header(required: false) { index -> $('a.accordion-toggle', index) }
    currentUsers(required: false) { $('span[ng-if="isCurrentUser(user)"]').parent().find('h4') }
    deleteUserButton(required: false) { index -> header(index).parent().find('button[ng-click="removeClick(user)"]') }
    resetUserButton(required: false) {
      index -> header(index).parent().find('button[ng-click="resetPasswordClick(user)"]')
    }

    deleteModal { module ModalModule, title: 'Delete User' }

    resetModal { module ModalModule, title: 'Reset Password', confirmText: 'Reset' }
    newPasswordField(required: false) { $('#generatedPassword') }

    summarySection { index -> $('div.accordion-inner', index) }
  }
}
