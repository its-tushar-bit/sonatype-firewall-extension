/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing.functional

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
    firstNameRequiredError(required: false) { firstNameControl.find('div', text: REQUIRED) }
    firstNameAlphaNumericError(required: false) { firstNameControl.find('div', text: ALPHA_NUMERIC) }
    firstNameSpacesError(required: false) { firstNameControl.find('div', text: startsWith('No leading')) }

    lastNameControl(required: false) { controls(1) }
    lastNameInput(required: false) { lastNameControl.find('input') }
    lastNameRequiredError(required: false) { lastNameControl.find('div', text: REQUIRED) }
    lastNameAlphaNumericError(required: false) { lastNameControl.find('div', text: ALPHA_NUMERIC) }
    lastNameSpacesError(required: false) { lastNameControl.find('div', text: startsWith('No leading')) }

    emailControl(required: false) { controls(2) }
    emailInput(required: false) { emailControl.find('input') }
    emailRequiredError(required: false) { emailControl.find('div', text: REQUIRED) }
    emailFormatError(required: false) { emailControl.find('div', text: INVALID_EMAIL) }

    usernameControl(required: false) { controls(3) }
    usernameInput(required: false) { usernameControl.find('input') }
    usernameRequiredError(required: false) { usernameControl.find('div', text: REQUIRED) }
    usernameAlphaNumericError(required: false) { usernameControl.find('div', text: ALPHA_NUMERIC) }
    usernamePatternError(required: false) { usernameControl.find('div', text: NO_SPACES) }

    passwordControl(required: false) { controls(4) }
    passwordInput(required: false) { passwordControl.find('input') }
    passwordRequiredError(required: false) { passwordControl.find('div', text: REQUIRED) }

    passwordValidateControl(required: false) { controls(5) }
    passwordValidateInput(required: false) { passwordValidateControl.find('input') }
    passwordValidateRequiredError(required: false) { passwordValidateControl.find('div', text: REQUIRED) }
    passwordValidateMatchError(required: false) { passwordValidateControl.find('div', text: PASSWORDS_MUST_MATCH) }

    save(required: false) { $('button', 'ng-click': 'saveClick(user)') }
    cancel(required: false) { $('button', 'ng-click': 'cancelClick(user)') }
    header(required: false) { $('a.accordion-toggle') }
    summarySection(required: false) { $('div.accordion-inner') }
  }
}
