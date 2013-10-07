/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Page

class UserManagementPage extends Page {
  static url = "assets/index.html#/management/security/users"
  
  static at = { 
    !newUserButton.empty() || !userForm.empty() 
  }
  
  static content = {
    newUserButton(required: false) { $('.new-user-button') }
    userForm(required: false) { $('form', name: 'userForm') }
    firstNameControl(required: false) { userForm.find('div.controls', 0) }
    firstNameInput(required: false) { firstNameControl.find('input') }
    firstNameRequiredError(required: false) { firstNameControl.find('div', text: 'Field is required.') }
    firstNameAlphaNumericError(required: false) { firstNameControl.find('div', text: 'Must be alpha numeric.') }
    firstNameSpacesError(required: false) { firstNameControl.find('div', text: startsWith('No leading') ) }
    lastNameControl(required: false) { userForm.find('div.controls', 1) }
    lastNameInput(required: false) { lastNameControl.find('input') }
    lastNameRequiredError(required: false) { lastNameControl.find('div', text: 'Field is required.') }
    lastNameAlphaNumericError(required: false) { lastNameControl.find('div', text: 'Must be alpha numeric.') }
    lastNameSpacesError(required: false) { lastNameControl.find('div', text: startsWith('No leading') ) }
    emailControl(required: false) { userForm.find('div.controls', 2) }
    emailInput(required: false) { emailControl.find('input') }
    emailRequiredError(required: false) { emailControl.find('div', text: 'Field is required.') }
    emailFormatError(required: false) { emailControl.find('div', text: 'Use valid format: abc@xyz.com') }
    usernameControl(required: false) { userForm.find('div.controls', 3) }
    usernameInput(required: false) { usernameControl.find('input') }
    usernameRequiredError(required: false) { usernameControl.find('div', text: 'Field is required.') }
    usernameAlphaNumericError(required: false) { usernameControl.find('div', text: 'Must be alpha numeric.') }
    usernameSpacesError(required: false) { usernameControl.find('div', text: startsWith('No leading') ) }
    passwordControl(required: false) { userForm.find('div.controls', 4) }
    passwordInput(required: false) { passwordControl.find('input') }
    passwordRequiredError(required: false) { passwordControl.find('div', text: 'Field is required.') }
    passwordValidateControl(required: false) { userForm.find('div.controls', 5) }
    passwordValidateInput(required: false) { passwordValidateControl.find('input') }
    passwordValidateRequiredError(required: false) { passwordValidateControl.find('div', text: 'Field is required.') }
    passwordValidateMatchError(required: false) { passwordValidateControl.find('div', text: 'Passwords must match!') }
    save(required: false) { $('button', 'ng-click': 'saveClick(user)')}
    cancel(required: false) { $('button', 'ng-click': 'cancelClick(user)')}
  }
}
