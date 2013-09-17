package com.sonatype.insight.brain.testing.functional

import geb.Page

class LoginPage extends Page {
  static url = "login-assets/login.html"
  
  static at = {
    title == "CLM Login"
  }
  
  static content = {
    usernameInput { $("input", id: "user") }
    passwordInput { $("input", id: "password") }
    loginButton { $("button", text: "Sign in") }
    // Ideally this would be identified by something other than the text content.  Then the message can be compared
    // against what is expected in the test.
    errorMessage(required: false, wait: true) { $(text: contains("Invalid credentials")) }
  }

  void loginAsAdmin() {
    login("admin","admin123")
  }
  
  void login(username, password) {
    usernameInput.value(username)
    passwordInput.value(password)
    loginButton.click()
  }
}
