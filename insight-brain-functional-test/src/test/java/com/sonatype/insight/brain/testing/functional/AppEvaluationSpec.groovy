/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;


class AppEvaluationSpec extends BaseSpec {  
  Organization org
  
  def apps = []

  def setup() {
    org = new Organization(name: 'AppEvaluationOrg')
    organizationDAO.insert(org)
    
    for ( i in 1..5 ) {
      def app = new Application('AppEvaluationApp' + i, 'AppEvaluationApp' + i, org.id)
      applicationDAO.insert(app)
      apps.add(app)
    }
    
    to ReportViolationsPage
    login.loginAsAdmin()
  }

  def cleanup() {
    for (app in apps) {
      applicationDAO.delete(app)
    }
    
    organizationDAO.delete(org)
  }

  def "validate application evaluation available from organization screen"() {
    when: 'User accesses organization'
      OrganizationManagementPage orgManPage = to OrganizationManagementPage
      waitFor { orgManPage.organization("AppEvaluationOrg").displayed }
      orgManPage.organization("AppEvaluationOrg").click()
    then: 'User at organization page and app eval button is visible'
      OrganizationPage orgPage = at (OrganizationPage)
      orgPage.tools.appEvalButton.displayed
    when: 'User clicks on the app eval button'
      orgPage.tools.appEvalButton.click()
    then: 'User see the app eval dialog and no application is selected'
      orgPage.tools.appEval.dialog.displayed
      orgPage.tools.appEval.application.value() == ''
  }

  def "validate application evaluation available from application screen"() {
    when: 'User accesses application'
      ApplicationManagementPage appManPage = to ApplicationManagementPage
      waitFor { appManPage.application("AppEvaluationApp2").displayed }
      appManPage.application("AppEvaluationApp2").click()
    then: 'User at application page and app eval button is visible'
      ApplicationPage appPage = at (ApplicationPage)
      appPage.tools.appEvalButton.displayed
    when: 'User clicks on the app eval button'
      appPage.tools.appEvalButton.click()
    then: 'User see the app eval dialog and an application is selected'
      appPage.tools.appEval.dialog.displayed
      appPage.tools.appEval.application.value() == '1'
      appPage.tools.appEval.getSelectedApplicationOption() == "AppEvaluationApp2"
  }

  def "validate upload"() {
    when: 'User accesses application and sets params for application evaluation'
      ApplicationManagementPage appManPage = to ApplicationManagementPage
      waitFor { appManPage.application("AppEvaluationApp2").displayed }
      appManPage.application("AppEvaluationApp2").click()
      ApplicationPage appPage = at (ApplicationPage)
      appPage.tools.appEvalButton.displayed
      appPage.tools.appEvalButton.click()
      appPage.tools.appEval.file.value(new File(getClass().getResource( '/AppEvaluationSpec/some.file' ).toURI()).getAbsoluteFile().getAbsolutePath())
      appPage.tools.appEval.stage.value('0')
    then: 'The upload button enables'
      !appPage.tools.appEval.upload.disabled
    when: 'User clicks upload'
      appPage.tools.appEval.upload.click()
    then: 'User sees the evaluation status screen'
      //will be disabled initially, until processing complete
      waitFor { appPage.tools.appEval.viewReport.@disabled || appPage.tools.appEval.status.text() == 'Done' }
      //so just wait for that to happen
      waitFor('slow') {!appPage.tools.appEval.viewReport.@disabled}
      getAvailableWindows().size() == 1
    when: 'User clicks to view the report'
      appPage.tools.appEval.viewReport.click()
    then: 'new tab is open on the report page'
      waitFor {getAvailableWindows().size() == 2 }
      withWindow(availableWindows[1]){
        driver.currentUrl.contains('reports.html#/reports/AppEvaluationApp2')
      }
  }
}