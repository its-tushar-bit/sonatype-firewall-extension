/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator

import spock.lang.Shared
import spock.lang.Stepwise

/**
 * Use the unframed report page definition for most tests because it's easier to work with.
 */
@Stepwise
class ReportPageSpec
extends BaseSpec {

  static cannedTestReport = '/canned-reports/small-report.zip'

  @Shared
  def app

  @Shared
  def scanId

  @Override
  def setupSpec() {
    app = newApplication()

    // ensure there is a report to view
    // The scanId must match the reportId value recorded inside the test report.zip used for this test
    scanId = '306e0a923df34c64b836358182b1b902'
    def evaluator = new TestReportEvaluator(app, scanId, getClass().getResource(cannedTestReport), browser.baseUrl,
        new InsightWork(serviceRule.configuration))
    evaluator.evaluatePolicy()

    // Can't do anything without a logged in user
    loginAsAdminVia()
  }

  @Override
  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Sub report navigation is shown"() {
    when: 'viewing the report'
    to ReportPage, app.publicId, scanId

    then: 'summary nav button is shown'
    navigation.summaryTrigger.displayed == true

    and: 'policy nav button is shown'
    navigation.policyTrigger.displayed == true

    // add other nav button verifications to new then: blocks as tests require them
  }

  def "Can view summary sub report"() {
    when: 'viewing the summary sub report'
    navigation.toSummaryReportPage()

    then: 'summary information is shown'
    summaryContent.displayed == true
  }

  def "Can view policy sub report"() {
    when: 'viewing the policy sub report'
    navigation.toPolicyReportPage()

    then: 'component summary information is shown'
    policyContent.displayed == true
  }

  def "Report can be framed in CLM UI"() {
    when: 'viewing the report in the CLM UI'
    to ReportContainerPage, app.publicId, scanId

    then: 'it is framed'
    withFrame(reportFrame, ReportPage) { contentContainer.displayed == true }
  }

  private Application newApplication() {
    def org = temporaryEntity.newOrganization()
    def app = temporaryEntity.newApplication(org.id)

    return app
  }
}
