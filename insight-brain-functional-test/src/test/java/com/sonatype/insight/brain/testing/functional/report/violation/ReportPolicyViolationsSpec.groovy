/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType
import com.sonatype.insight.brain.service.InsightWork
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.utils.TestReportEvaluator

import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@Ignore //https://sonatype.atlassian.net/browse/CLM-30530
class ReportPolicyViolationsSpec
extends BaseSpec {
  @Shared
  private static PolicyDAO policyDAO

  private static cannedTestReport = '/canned-reports/small-report2.zip'

  private static Application app

  private static String scanId

  @Override
  def setupSpec() {
    // get DAOs
    policyDAO = lookup(PolicyDAO.class);

    // create app
    app = temporaryEntity.newApplication(temporaryEntity.newOrganization().getId())

    // create policy
    createLicenseNotAllowedPolicy("LGPL-3.0", "lgpl") //  expected to affect 1 artifact
    createLicenseNotAllowedPolicy("UNKNOWN", 'NonStandard') // expected to affect 1 artifact
    Policy policy = createNoSVPolicy() // expected to affect 2 artifacts

    // add waivers
    temporaryEntity.newWaiver("848d7549ef7ec13ce546", policy.getId(), app.getId())
    temporaryEntity.newWaiver("494308fc2d433720c778", policy.getId(), app.getId())

    // trigger eval
    // The scanId must match the reportId value recorded inside the test report.zip used for this test
    scanId = 'b9a43b67bf98409f9f79eae8574e227f'
    def evaluator = new TestReportEvaluator(app, scanId, getClass().getResource(cannedTestReport), browser.baseUrl,
        new InsightWork(serviceRule.configuration))
    evaluator.evaluatePolicy()

    // Can't do anything without a logged in user
    loginAsAdminVia()
    to ReportPage, app.publicId, scanId
  }

  def "Validate the summary view"() {
    when: "we view summary"
    navigation.toPolicyReportPage()

    then: "waived violations are hidden"
    // verify Summary is selected
    selectedViolationFilter == 'Summary'
    waitFor { results.size() == 4 }
    hasRow(results, 'javancss : javancss : 29.50')
    hasRow(results, 'ch.qos.logback : logback-access : 0.6')
    hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15')
    hasRow(results, 'org.apache.geronimo.framework : geronimo-security : 2.1')
  }

  def "Validate the all view"() {
    when: "we view all"
    allViolations.click()

    then: "all policy violations are visible"
    waitFor { selectedViolationFilter == 'All' }
    waitFor { results.size() == 24 }
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
    waitFor { selectedViolationFilter == 'Waived' }
    waitFor { results.size() == 21 }
    hasRow(results, 'org.apache.geronimo.framework : geronimo-security : 2.1', true)
    hasRow(results, 'org.mortbay.jetty : jetty : 6.1.15', true)
  }

  /*
   * Creates a policy against presence of a license
   */

  private static Policy createLicenseNotAllowedPolicy(String license, String name) {
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
    constraints: [
      new Constraint(name: 'NoSV', operator: LogicalOperator.AND,
      conditions: [new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0")])
    ])
    policyDAO.insert(policy)
    return policy
  }

  private static boolean hasRow(List<PolicyReportRow> rows, String coordinates, boolean waived = false) {
    return rows.any { PolicyReportRow row ->
      row.coordinates == coordinates && row.waived.isPresent() == waived
    }
  }
}
