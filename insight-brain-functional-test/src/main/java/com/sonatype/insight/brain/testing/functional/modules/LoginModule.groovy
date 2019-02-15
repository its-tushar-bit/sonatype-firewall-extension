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
class LoginModule
    extends Module
{
  static content = {
    modal(required: false) { $('#login-modal') }
    usernameInput { $('#login-username') }
    passwordInput { $('#login-password') }
    loginAction { $('#login-action').module(FormElement) }
    errorMessage(required: false, wait: true) { $('#login-error') }
  }

  def login(username, password) {
    login(username, password, false)
  }

  def login(username, password, expectedFail) {
    waitFor { usernameInput.displayed }
    usernameInput.value(username)
    passwordInput.value(password)
    waitFor { !loginAction.disabled }
    loginAction.click()
    if (!expectedFail) {
      waitFor { !modal.displayed }
    }
  }

  @Override
  boolean isDisplayed() {
    modal.displayed
  }
}
