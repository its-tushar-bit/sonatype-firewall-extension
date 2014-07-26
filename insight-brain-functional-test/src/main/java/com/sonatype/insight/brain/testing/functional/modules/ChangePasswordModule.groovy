/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.7
 */
class ChangePasswordModule
    extends Module
{
  static content = {
    //this content is all in the popup dialog
    dialog(required: false) { $('form[name="passwordForm"]') }
    oldPassword(required: false) { dialog.originalPassword() }
    newPassword(required: false) { dialog.newPassword() }
    newPasswordValidate(required: false) { dialog.confirmPassword() }
    invalidCredentialsError(required: false) { dialog.find('div.alert') }
    ok(required: false) { $('form[name="passwordForm"] button.btn-primary') }
    cancel(required: false) { $('form[name="passwordForm"] button.btn-cancel)') }
  }
}
