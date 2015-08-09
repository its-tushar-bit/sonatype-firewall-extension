/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.security.InternalRealm


class ChangePasswordSpec
extends BaseSpec {
  //simple setup, login as this new user and go to the management page
  def setupSpec() {
    temporaryEntity.newUser("testchangepass", new InternalRealm().encryptPassword("secret"), "John", "Doe", "john@doe.net")
    via ReportViolationsPage
    login.login("testchangepass", "secret")
    verifyAt()
  }

  def "can change password"() {
    when: "User clicks the change password link"
    userOptions.changePasswordClick()

    then: "User sees the change password dialog and save is disabled"
    changePassword.dialog.displayed
    changePassword.ok.disabled

    when: "User enters an invalid old password"
    changePassword.oldPassword.value('unsecret')

    then: "Save button stays disabled"
    changePassword.ok.disabled

    when: "User enters a new password"
    changePassword.newPassword.value('newsecret')

    then: "Save button stays disabled"
    changePassword.ok.disabled

    when: "User enters a validate password that doesn't match"
    changePassword.newPasswordValidate.value('newsecretdoesntmatch')

    then: "Save button stays disabled and validation error shown"
    waitFor { popoverText(changePassword.newPasswordValidate) == 'Passwords must match!' }
    changePassword.ok.disabled

    when: "User enters proper validation password"
    changePassword.newPasswordValidate.value('newsecret')

    then: "Save button becomes enabled"
    waitFor { popoverViolations(changePassword.newPasswordValidate.parent()).size() == 0 }
    !changePassword.ok.disabled

    when: "User clicks save button"
    changePassword.ok.click()

    then: "User sees error stating credentials are invalid"
    waitFor { changePassword.invalidCredentialsError.displayed }

    when: "User enters valid old password and clicks save"
    changePassword.oldPassword.value('secret')
    changePassword.ok.click()

    then: "User should no longer see the change password dialog"
    waitFor { !changePassword.dialog.displayed }

    when: "User attempts to login with new password"
    userOptions.logoutClick()
    login.login("testchangepass", "newsecret")

    then: "Application is loaded"
    to ManagementPage
  }
}
