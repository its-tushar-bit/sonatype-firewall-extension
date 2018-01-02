/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module
import geb.module.FormElement

/**
 * @since 1.7
 */
class ChangePasswordModule
    extends Module
{
  static content = {
    //this content is all in the popup dialog
    dialog(required: false) { $('form[name="passwordForm"]') }
    oldPassword(required: false) { $('#original-password') }
    newPassword(required: false) { $('#new-password') }
    newPasswordValidate(required: false) { $('#confirm-password') }
    invalidCredentialsError(required: false) { $('#change-password-error') }
    ok(required: false) { $('#change-password-submit').module(FormElement) }
    cancel(required: false) { $('#change-password-cancel').module(FormElement) }
  }
}
