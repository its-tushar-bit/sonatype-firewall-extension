/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.security.Permission

import org.apache.commons.io.IOUtils

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

    saasRule.setResponseForURI('rest/ci/scan', '{"scanId": "blah", "timeToReport": 0}', 200);
    saasRule.setResponseForURI('rest/ci/report?scanId=blah',
        IOUtils.toByteArray(getClass().getResourceAsStream('/report.zip')), 200);
  }

  def setup() {
    loginAsUserVia()
  }

  def "validate application evaluation available from organization screen"() {
    when: 'User accesses organization'
    OrganizationManagementPage orgManPage = to OrganizationManagementPage
    waitFor { orgManPage.organization("AppEvaluationOrg").displayed }
    orgManPage.organization("AppEvaluationOrg").click()

    then: 'User at organization page and app eval button is visible'
    OrganizationPage orgPage = at(OrganizationPage)
    orgPage.tools.appEvalButton.displayed

    when: 'User clicks on the app eval button'
    orgPage.tools.appEvalButton.click()

    then: 'User see the app eval dialog and no application is selected'
    waitFor { orgPage.tools.appEval.dialog.displayed }
    waitFor { orgPage.tools.appEval.application.value() == '' }

    and: 'Four choices are available for the stage'
    orgPage.tools.appEval.availableStages == AVAILABLE_STAGES

    and: 'Dialog can be canceled'
    orgPage.tools.appEval.cancel.click()
    waitFor { !orgPage.tools.appEval.dialog.displayed }
  }

  def "validate application evaluation available from application screen"() {
    when: 'User accesses application'
    ApplicationManagementPage appManPage = to ApplicationManagementPage
    waitFor { appManPage.application("AppEvaluationApp2").displayed }
    appManPage.application("AppEvaluationApp2").click()

    then: 'User at application page and app eval button is visible'
    ApplicationPage appPage = at(ApplicationPage)
    appPage.tools.appEvalButton.displayed

    when: 'User clicks on the app eval button'
    appPage.tools.appEvalButton.click()

    then: 'User see the app eval dialog and an application is selected'
    waitFor { appPage.tools.appEval.dialog.displayed }
    appPage.tools.appEval.application.value() == '1'
    appPage.tools.appEval.getSelectedApplicationOption() == "AppEvaluationApp2"

    and: 'Four choices are available for the stage'
    appPage.tools.appEval.availableStages == AVAILABLE_STAGES

    and: 'Dialog can be canceled'
    appPage.tools.appEval.cancel.click()
    waitFor { !appPage.tools.appEval.dialog.displayed }
  }

  def "validate upload"() {
    when: 'User accesses application and sets params for application evaluation'
    ApplicationManagementPage appManPage = to ApplicationManagementPage
    waitFor { appManPage.application("AppEvaluationApp2").displayed }
    appManPage.application("AppEvaluationApp2").click()
    ApplicationPage appPage = at(ApplicationPage)
    appPage.tools.appEvalButton.displayed
    appPage.tools.appEvalButton.click()
    waitFor { appPage.tools.appEval.dialog.displayed }
    appPage.tools.appEval.stage.value('3')
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
