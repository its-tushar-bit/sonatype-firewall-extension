/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.model.security.User

class AppEvaluationSpec
extends BaseSpec {
  static Organization org

  private static final ArrayList<String> AVAILABLE_STAGES = ['Build', 'Stage Release', 'Release', 'Operate']

  def setupSpec() {
    createUser()
    org = temporaryEntity.newOrganization('AppEvaluationOrg')
    grantPermissions(getUsername(), org.getId(), Permission.WRITE, Permission.READ, Permission.EVALUATE_APPLICATION)

    for (i in 1..5) {
      def name = "AppEvaluationApp$i"
      temporaryEntity.newApplication(name, name, org.getId())
    }

    hdsRule.setResponseForURI('rest/application/analysis', '{"scanId": "blah", "timeToReport": 0}', 200);
    hdsRule.setResponseForURI('rest/application/analysis/blah', getClass().getResource('/report.zip'), 200);
  }

  def setup() {
    loginAsUserVia()
  }

  def "validate exceptions prior to polling are displayed"() {
    setup:
    User readOnlyUser = temporaryEntity.newUser("readOnlyUser")
    Role role = temporaryEntity.newRole(false /* global */, Permission.READ)
    temporaryEntity.newMembershipMapping(org.getId(), role.getId(), readOnlyUser.getUsername())
    userOptions.logoutClick()
    loginAsUserVia(readOnlyUser.getUsername(), readOnlyUser.getPassword())
    
    when: 'User accesses organization'
    OwnerManagementPage ownerManagementPage = to OwnerManagementPage
    ownerManagementPage.selectOrganization('AppEvaluationOrg')

    then: 'User at organization page and app eval button is visible'
    OrganizationPage orgPage = at(OrganizationPage)
    orgPage.tools.appEvalButton.displayed

    when: 'User uploads app using app eval button'
    orgPage.tools.appEvalButton.click()
    waitFor { orgPage.tools.appEval.application.displayed }
    orgPage.tools.appEval.application.value('AppEvaluationApp1')
    orgPage.tools.appEval.stage.value('string:release')
    orgPage.tools.appEval.file.value(
        new File(getClass().getResource('/AppEvaluationSpec/some.file').toURI()).getAbsoluteFile().getAbsolutePath())
    waitFor { !orgPage.tools.appEval.upload.disabled }
    orgPage.tools.appEval.upload.click()


    then: 'Status is set to Done and an error is displayed'
    waitFor { orgPage.tools.appEval.status.text() == 'Done' }
    orgPage.tools.appEval.viewReport.@disabled == 'true'
    orgPage.tools.appEval.alerts.displayed
    waitFor { !orgPage.tools.appEval.close.disabled }
    orgPage.tools.appEval.close.click()
  }
  
  def "validate application evaluation available from organization screen"() {
    when: 'User accesses organization'
    OwnerManagementPage ownerManagementPage = to OwnerManagementPage
    ownerManagementPage.selectOrganization('AppEvaluationOrg')

    then: 'User at organization page and app eval button is visible'
    OrganizationPage orgPage = at(OrganizationPage)
    orgPage.tools.appEvalButton.displayed

    when: 'User clicks on the app eval button'
    orgPage.tools.appEvalButton.click()

    then: 'User see the app eval dialog'
    waitFor { orgPage.tools.appEval.dialog.displayed }

    and: 'no application is selected'
    waitFor { orgPage.tools.appEval.application.value() == '' }

    and: 'Four choices are available for the stage'
    orgPage.tools.appEval.availableStages == AVAILABLE_STAGES

    and: 'Dialog can be canceled'
    orgPage.tools.appEval.cancel.click()
    waitFor { !orgPage.tools.appEval.dialog.displayed }
  }

  def "validate application evaluation available from application screen"() {
    when: 'User accesses application'
    OwnerManagementPage ownerManagementPage = to OwnerManagementPage
    ownerManagementPage.selectApplication('AppEvaluationOrg', 'AppEvaluationApp2')

    then: 'User at application page and app eval button is visible'
    ApplicationPage appPage = at(ApplicationPage)
    appPage.tools.appEvalButton.displayed

    when: 'User clicks on the app eval button'
    appPage.tools.appEvalButton.click()

    then: 'User see the app eval dialog'
    waitFor { appPage.tools.appEval.dialog.displayed }

    and: 'an application is selected'
    waitFor { appPage.tools.appEval.application.value() == 'string:AppEvaluationApp2' }
    appPage.tools.appEval.getSelectedApplicationOption() == "AppEvaluationApp2"

    and: 'Four choices are available for the stage'
    appPage.tools.appEval.availableStages == AVAILABLE_STAGES

    and: 'Dialog can be canceled'
    appPage.tools.appEval.cancel.click()
    waitFor { !appPage.tools.appEval.dialog.displayed }
  }

  def "validate upload"() {
    when: 'User accesses application and sets params for application evaluation'
    OwnerManagementPage ownerManagementPage = to OwnerManagementPage
    ownerManagementPage.selectApplication('AppEvaluationOrg', 'AppEvaluationApp2')
    ApplicationPage appPage = at(ApplicationPage)
    appPage.tools.appEvalButton.displayed
    appPage.tools.appEvalButton.click()
    waitFor { appPage.tools.appEval.stage.displayed }
    appPage.tools.appEval.stage.value('string:release')
    // integrating the file input with Angular is, interesting, populating this last to check UI responds
    // properly/immediately
    appPage.tools.appEval.file.value(
        new File(getClass().getResource('/AppEvaluationSpec/some.file').toURI()).getAbsoluteFile().getAbsolutePath())

    then: 'The upload button enables'
    !appPage.tools.appEval.upload.disabled

    when: 'User clicks upload'
    appPage.tools.appEval.upload.click()

    then: 'User sees the evaluation status screen'
    //will be disabled initially, until processing complete
    waitFor { appPage.tools.appEval.viewReport.@disabled || appPage.tools.appEval.status.text() == 'Done' }
    //so just wait for that to happen
    waitFor('slow') { !appPage.tools.appEval.viewReport.@disabled }
    getAvailableWindows().size() == 1

    when: 'User clicks to view the report'
    appPage.tools.appEval.viewReport.click()

    then: 'new tab is open on the report page'
    waitFor { getAvailableWindows().size() == 2 }
    withWindow(close: true, availableWindows[1]) {
      waitFor { driver.currentUrl.contains('index.html#/reports/AppEvaluationApp2') }
    }

    and: 'Dialog can be closed'
    appPage.tools.appEval.close.click()
    waitFor { !appPage.tools.appEval.dialog.displayed }
  }
}
