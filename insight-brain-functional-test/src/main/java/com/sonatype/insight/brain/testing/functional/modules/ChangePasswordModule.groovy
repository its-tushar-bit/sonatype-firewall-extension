/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.7
 */
class ChangePasswordModule extends Module
{
  static content = {
    //the main link from the top right of the page
    open { $('a', text: 'Change Password') }
    //this content is all in the popup dialog
    dialog(required: false) { $('form', name: 'passwordForm') }
    oldPassword(required: false) { dialog.find('input', 'name': 'currentPassword') }
    newPassword(required: false) { dialog.find('input', 'name': 'newPassword') }
    newPasswordValidate(required: false) { dialog.find('input', 'name': 'validatePassword') }
    newPasswordValidateDoesntMatch(required: false) { dialog.find('div', text: 'Passwords must match!') }
    invalidCredentialsError(required: false) { dialog.find('div', text: 'Invalid credentials supplied.') }
    ok(required: false) { dialog.find('button', text: 'Change') }
    cancel(required: false) { dialog.find('button', text: 'Cancel') }
  }
}
