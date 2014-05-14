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
  static url = "assets/index.html#/management/security/users"

  static at = { newUserButton.displayed }

  static content = {
    newUserButton(wait: true) { $('#user-new') }
    userForm(required: false) { $('#user-form') }

    firstNameInput(required: false) { userForm.firstName() }
    firstNameValidations(required: false) { module ValidationModule, firstNameInput.parent() }

    lastNameInput(required: false) { userForm.lastName() }
    lastNameValidations(required: false) { module ValidationModule, lastNameInput.parent() }

    emailInput(required: false) { userForm.email() }
    emailValidations(required: false) { module ValidationModule, emailInput.parent() }

    usernameInput(required: false) { userForm.username() }
    usernameValidations(required: false) { module ValidationModule, usernameInput.parent() }

    passwordInput(required: false) { userForm.password() }
    passwordValidations(required: false) { module ValidationModule, passwordInput.parent() }

    passwordValidateInput(required: false) { userForm.passwordValidate() }
    passwordValidateValidations(required: false) { module ValidationModule, passwordValidateInput.parent() }

    uniqueUserValidation(required: false) { usernameInput.parent().find('div', text: 'Enter a unique username') }

    validations(required: false) {
      [firstNameValidations, lastNameValidations, emailValidations, usernameValidations, passwordValidations,
          passwordValidateValidations]
    }

    errorFree(required: false) { !validations.any { !it.errorFree } && !uniqueUserValidation?.displayed }
    save(required: false) { $('#user-form-save') }
    cancel(required: false) { $('#user-form-cancel') }
    headers(required: false) { $('a.accordion-toggle') }
    header(required: false) { index -> $('a.accordion-toggle', index) }
    currentUsers(required: false) { $('span[ng-if="isCurrentUser(user)"]').parent().find('h4') }
    deleteUserButton(required: false) { index -> header(index).parent().find('button[ng-click="removeClick(user)"]') }
    resetUserButton(required: false) { index -> header(index).parent().find('button[ng-click="resetPasswordClick(user)"]') }

    deleteModal { module ModalModule, title: 'Delete User'}

    resetModal { module ModalModule, title: 'Reset Password', confirmText: 'Reset'}
    newPasswordField(required: false) { $('#generatedPassword') }

    summarySection { index -> $('div.accordion-inner', index) }
  }
}
