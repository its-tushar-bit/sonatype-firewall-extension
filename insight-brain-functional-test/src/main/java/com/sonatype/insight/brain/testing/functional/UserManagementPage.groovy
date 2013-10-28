/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ValidationModule
import geb.Page

import static com.sonatype.insight.brain.testing.functional.utils.ValidationConstants.*

class UserManagementPage
    extends Page
{
  static url = "assets/index.html#/management/security/users"

  static at = { newUserButton.displayed }

  static content = {
    newUserButton(wait: true) { $('.new-user-button') }
    userForm(required: false) { $('form', name: 'userForm') }
    controls(required: false) { index -> userForm.find('div.controls', index) }

    firstNameControl(required: false) { controls(0) }
    firstNameInput(required: false) { firstNameControl.find('input') }
    firstNameValidations(required: false) { module ValidationModule, firstNameControl }

    lastNameControl(required: false) { controls(1) }
    lastNameInput(required: false) { lastNameControl.find('input') }
    lastNameValidations(required: false) { module ValidationModule, lastNameControl }

    emailControl(required: false) { controls(2) }
    emailInput(required: false) { emailControl.find('input') }
    emailValidations(required: false){ module ValidationModule, emailControl}

    usernameControl(required: false) { controls(3) }
    usernameInput(required: false) { usernameControl.find('input') }
    usernameValidations(required:false){module ValidationModule, usernameControl}

    passwordControl(required: false) { controls(4) }
    passwordInput(required: false) { passwordControl.find('input') }
    passwordValidations(required:false) { module ValidationModule, passwordControl }

    passwordValidateControl(required: false) { controls(5) }
    passwordValidateInput(required: false) { passwordValidateControl.find('input') }
    passwordValidateValidations(required: false) { module ValidationModule, passwordValidateControl }

    save(required: false) { $('button', 'ng-click': 'saveClick(user)') }
    cancel(required: false) { $('button', 'ng-click': 'cancelClick(user)') }
    header(required: false) { $('a.accordion-toggle') }
    summarySection(required: false) { $('div.accordion-inner') }
  }
}
