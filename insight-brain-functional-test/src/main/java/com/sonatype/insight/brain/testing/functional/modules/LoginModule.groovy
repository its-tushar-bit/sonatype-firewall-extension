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
class LoginModule extends Module {
  static content = {
    modal ( required: false ) { $('div', id: 'loginModalHeader').parent() }
    usernameInput { $(id: "login-username") }
    passwordInput { $(id: "login-password") }
    loginAction { $(id: "login-action") }
    errorMessage(required: false, wait: true) { $(id: "login-error") }
  }

  void loginAsAdmin() {
    login("admin", "admin123")
  }

  void login(username, password) {
    login(username, password, false)
  }

  void login(username, password, expectedFail) {
    waitFor { modal.present }
    usernameInput.value(username)
    passwordInput.value(password)
    waitFor { loginAction.@disabled != 'disabled' }
    loginAction.click()
    if (!expectedFail) {
      waitFor { !modal.present }
    }
  }
  
  void isDisplayed() {
    modal.present
  }
}
