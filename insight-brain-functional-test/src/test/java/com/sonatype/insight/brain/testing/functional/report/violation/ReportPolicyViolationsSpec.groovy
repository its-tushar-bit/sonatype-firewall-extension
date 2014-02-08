/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.policy.Condition
import com.sonatype.insight.brain.model.policy.Constraint
import com.sonatype.insight.brain.model.policy.LogicalOperator
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise // Share the login and browser instance to reduce execution time
class ReportPolicyViolationsSpec
extends BaseSpec
{
  @Shared
  private static PolicyDAO policyDAO = new PolicyDAO()

  private static cannedTestReport = '/canned-reports/small-report2.zip'

  private static Application app

  private static String scanId

  def setupSpec() {
    // create app
    app = temporaryEntity.newApplication(temporaryEntity.newOrganization().getId())

    // create policy
    createNoLGPL("LGPL-3.0", "lgpl") //  expected to affect 1 artifact
    createNoLGPL("UNKNOWN", 'NonStandard') // expected to affect 1 artifact
    Policy policy = createNoSVPolicy() // expected to affect 2 artifacts

    // add waivers
    temporaryEntity.newWaiver("848d7549ef7ec13ce546", policy.getId(), app.getId())
    temporaryEntity.newWaiver("494308fc2d433720c778", policy.getId(), app.getId())

    // trigger eval
    def evaluator = new TestReportEvaluator(app, getClass().getResource(cannedTestReport), browser.baseUrl,
        new InsightWork(serviceRule.configuration))
    scanId = evaluator.evaluatePolicy()

    // Can't do anything without a logged in user
    loginAsAdminVia()
    to ReportPage, app.publicId, scanId
  }

  def "Validate the summary view"() {
    when: "we view summary"
      navigation.toPolicyReportPage()
    then: "waived violations are hidden"
      // verify Summary is selected
      getSelectedViolationFilter() == 'Summary'
      waitFor{ results.size() == 3 }
      hasRow(results, 'javancss : javancss : 29.50')
      hasRow(results, 'ch.qos.logback : logback-access : 0.6')
      hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15')
  }

  def "Validate the all view"() {
    when: "we view all"
      allViolations.click()
    then: "all policy violations are visible"
      waitFor { getSelectedViolationFilter() == 'All' }
      waitFor { results.size() == 5 }
      hasRow(results, 'javancss : javancss : 29.50')
      hasRow(results, 'ch.qos.logback : logback-access : 0.6')
      hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15')
      hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15', true)
      hasRow(results, 'org.apache.geronimo.framework : geronimo-security : 2.1', true)
  }

  def "Validate the waived view"() {
    when: "we view waived"
      waivedViolations.click()
    then: "only waived violations are visible"
      waitFor { getSelectedViolationFilter() == 'Waived' }
      waitFor { results.size() == 2 }
      hasRow(results, 'org.apache.geronimo.framework : geronimo-security : 2.1', true)
      hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15', true)
  }

  /*
   * Creates a policy against presence of a license
   */
  private static Policy createNoLGPL(String license, String name) {
    Policy policy = new Policy(name: "No$name", ownerId: app.id, threatLevel: 7,
        constraints: [new Constraint(name: "No$name", operator: LogicalOperator.AND,
            conditions: [new Condition(LicenseConditionType.ID, 'is', license)])])
    policyDAO.insert(policy)
    return policy
  }

  /*
   * Creates a policy against presence of SVs
   */
  private static Policy createNoSVPolicy() {
    Policy policy = new Policy(name: 'NoSV', ownerId: app.id, threatLevel: 9,
        constraints: [new Constraint(name: 'NoSV', operator: LogicalOperator.AND,
            conditions: [new Condition(SecurityVulnerabilityConditionType.ID, 'present')])])
    policyDAO.insert(policy)
    return policy
  }
  
  private static boolean hasRow(List<PolicyReportRow> rows, String coordinates, boolean waived = false) {
    return rows.any { PolicyReportRow row ->
      row.coordinates == coordinates && row.waived.isPresent() == waived
    }
  }
}
