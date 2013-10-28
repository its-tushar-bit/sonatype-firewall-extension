/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ValidationModule
import geb.Page

class UserManagementPage
    extends Page
{
  static url = "assets/index.html#/management/security/users"

  static at = { newUserButton.displayed }

  static content = {
    newUserButton(wait: true) { $('.new-user-button') }
    userForm(required: false) { $('form', name: 'userForm') }
    controls(required: false) { index -> userForm.find('div.controls', index) }

    firstNameInput(required: false) { controls(0).find('input') }
    firstNameValidations(required: false) { module ValidationModule, firstNameInput.parent() }

    lastNameInput(required: false) { controls(1).find('input') }
    lastNameValidations(required: false) { module ValidationModule, lastNameInput.parent() }

    emailInput(required: false) { controls(2).find('input') }
    emailValidations(required: false) { module ValidationModule, emailInput.parent() }

    usernameInput(required: false) { controls(3).find('input') }
    usernameValidations(required: false) { module ValidationModule, usernameInput.parent() }

    passwordInput(required: false) { controls(4).find('input') }
    passwordValidations(required: false) { module ValidationModule, passwordInput.parent() }

    passwordValidateInput(required: false) { controls(5).find('input') }
    passwordValidateValidations(required: false) { module ValidationModule, passwordValidateInput.parent() }

    save(required: false) { $('button', 'ng-click': 'saveClick(user)') }
    cancel(required: false) { $('button', 'ng-click': 'cancelClick(user)') }
    header(required: false) { $('a.accordion-toggle') }
    summarySection(required: false) { $('div.accordion-inner') }
  }
}
