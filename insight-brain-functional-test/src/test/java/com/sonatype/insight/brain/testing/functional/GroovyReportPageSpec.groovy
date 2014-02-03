/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator

import org.junit.Ignore;
import spock.lang.Stepwise;
import spock.lang.Shared;

@Stepwise
class GroovyReportPageSpec
    extends BaseSpec 
{

  static cannedTestReport = '/canned-reports/small-report.zip'

  @Shared
  def app

  @Shared
  def scanId

  def setupSpec() {
    app = newApplication()

    // ensure there is a report to view
    def evaluator = new TestReportEvaluator(app, getClass().getResource(cannedTestReport), browser.baseUrl,
      new InsightWork(serviceRule.configuration))
    scanId = evaluator.evaluatePolicy()

    // Can't do anything without a logged in user
    loginAsAdminVia()
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Sub report navigation is shown"() {
    when: 'viewing the report'
      to GroovyReportPage, app.publicId, scanId

    then: 'policy nav button is shown'
      policyButton.displayed == true

    // add other nav button verifications to new then: blocks as tests require them
  }

  def "Can view policy sub report"() {
    when: 'viewing the policy sub report'
      toPolicyReportPage()

    then: 'component summary table is shown'
      policyContent.displayed == true
  }

  private Application newApplication() {
    def org = temporaryEntity.newOrganization()
    def app = temporaryEntity.newApplication(org.id)

    return app
  }
}