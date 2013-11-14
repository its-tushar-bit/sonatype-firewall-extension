/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.security.CLMRealm


class ChangePasswordSpec extends BaseSpec {
  //simple setup, login as this new user and go to the management page
  def setupSpec() {
    UserDAO userDAO = new UserDAO()
    User user = new User(username: "testchangepass", password: new CLMRealm().encryptPassword("secret"), firstName: "John", lastName: "Doe", email: "john@doe.net")
    userDAO.insert(user);
    to ReportPage
    login.login("testchangepass", "secret")
  }
  
  //make sure to cleanup our mess!
  def cleanupSpec() {
    UserDAO userDAO = new UserDAO();
    userDAO.getAll().each { user ->
      if (user.username == "testchangepass") {
        userDAO.delete(user);
      }
    }
  }

  def "can change password"() {
    when: "User clicks the change password link"
      user.changePassword.open.click();
    
    then: "User sees the change password dialog and save is disabled"
      user.changePassword.dialog.displayed
      user.changePassword.ok.disabled
      
    when: "User enters an invalid old password"
      user.changePassword.oldPassword.value('unsecret')
    
    then: "Save button stays disabled"
      user.changePassword.ok.disabled
    
    when: "User enters a new password"
      user.changePassword.newPassword.value('newsecret')
    
    then: "Save button stays disabled"
      user.changePassword.ok.disabled
    
    when: "User enters a validate password that doesn't match"
      user.changePassword.newPasswordValidate.value('newsecretdoesntmatch')
    
    then: "Save button stays disabled and validation error shown"
      user.changePassword.newPasswordValidateDoesntMatch.displayed
      user.changePassword.ok.disabled
    
    when: "User enters proper validation password"
      user.changePassword.newPasswordValidate.value('newsecret')
    
    then: "Save button becomes enabled"
      !user.changePassword.newPasswordValidateDoesntMatch.displayed
      !user.changePassword.ok.disabled
    
    when: "User clicks save button"
      user.changePassword.ok.click()
    
    then: "User sees error stating credentials are invalid"
      waitFor { user.changePassword.invalidCredentialsError.displayed }
    
    when: "User enters valid old password and clicks save"
      user.changePassword.oldPassword.value('secret')
      user.changePassword.ok.click()
    
    then: "User should no longer see the change password dialog"
      waitFor { !user.changePassword.dialog.displayed }
      
    when: "User attempts to login with new password"
      user.logout.link.click()
      login.login("testchangepass", "newsecret")
      to ManagementPage
    
    then: "Application is loaded"
      at ManagementPage
  }
}
