/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Page

class LoginPage extends Page {
  static url = "login-assets/login.html"
  
  static at = {
    title == "CLM Login"
  }
  
  static content = {
    usernameInput { $(id: "login-username") }
    passwordInput { $(id: "login-password") }
    loginAction { $(id: "login-action") }
    errorMessage(required: false, wait: true) { $(id: "login-error") }
  }

  void loginAsAdmin() {
    login("admin","admin123")
  }
  
  void login(username, password) {
    usernameInput.value(username)
    passwordInput.value(password)
    loginAction.click()
  }
}
