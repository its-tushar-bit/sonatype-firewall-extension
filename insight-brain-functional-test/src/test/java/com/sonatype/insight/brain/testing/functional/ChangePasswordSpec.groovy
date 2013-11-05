/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig
import com.sonatype.insight.brain.testing.functional.modules.ChangePasswordModule;

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.navigator.Navigator
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared

class ChangePasswordSpec extends GebReportingSpec {
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())
  
  //simple setup, login as this new user and go to the management page
  def setupSpec() {
    UserDAO userDAO = new UserDAO()
    User user = new User(username: "test", password: "secret", firstName: "John", lastName: "Doe", email: "john@doe.net")
    userDAO.insert(user);
    to LoginPage
    login('test', 'secret')
    to ManagementPage
  }
  
  def cleanupSpec() {
    UserDAO userDAO = new UserDAO();
    userDAO.getAll().each { user ->
      if (user.username.startsWith("test")) {
        userDAO.delete(user);
      }
    }
  }

  def "can change password"() {
    when: "User clicks the change password link"
      changePassword.open.click();
    
    then: "User sees the change password dialog and save is disabled"
      waitFor { changePassword.dialog.displayed }
      changePassword.ok.disabled
      
    when: "User enters an invalid old password"
      changePassword.oldPassword.value = 'unsecret'
    
    then: "Save button stays disabled"
      changePassword.ok.disabled
    
    when: "User enters a new password"
      changePassword.newPassword.value = 'newsecret'
    
    then: "Save button stays disabled"
      changePassword.ok.disabled
    
    when: "User enters a validate password that doesn't match"
      changePassword.newPasswordValidate.value = 'newsecretdoesntmatch'
    
    then: "Save button stays disabled and validation error shown"
      changePassword.newPasswordValidateDoesntMatch.displayed
      changePassword.ok.disabled
    
    when: "User enters proper validation password"
      changePassword.newPasswordValidate.value = 'newsecret'
    
    then: "Save button becomes enabled"
      !changePassword.newPasswordValidateDoesntMatch.displayed
      !changePassword.ok.disabled
    
    when: "User clicks save button"
      changePassword.ok.click()
    
    then: "User sees error stating credentials are invalid"
      invalidCredentialsError.displayed
    
    when: "User enters valid old password and clicks save"
      changePassword.oldPassword.value = 'secret'
      changePassword.ok.click()
    
    then: "User should no longer see the change password dialog"
      waitFor { !changePassword.dialog.displayed }
  }
}
