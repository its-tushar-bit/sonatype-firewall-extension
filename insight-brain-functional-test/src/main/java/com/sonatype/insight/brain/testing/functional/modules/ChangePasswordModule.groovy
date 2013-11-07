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
    oldPassword(required: false) { dialog.find('input', 'id': 'originalPassword') }
    newPassword(required: false) { dialog.find('input', 'id': 'newPassword') }
    newPasswordValidate(required: false) { dialog.find('input', 'id': 'confirmPassword') }
    newPasswordValidateDoesntMatch(required: false) { dialog.find('span', text: 'Does not match') }
    invalidCredentialsError(required: false) { dialog.find('span', 'ng-show': 'error') }
    ok(required: false) { dialog.find('button', text: 'Change') }
    cancel(required: false) { dialog.find('button', text: 'Cancel') }
  }
}
