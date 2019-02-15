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
 * Test aspects of LicenseOverride feature in the CIP.
 * @since 1.13.0
 */
@Stepwise
class LicenseOverrideSpec
extends BaseSpec {
  static final String cannedTestReport = '/canned-reports/small-report.zip'

  @Shared
  Application app

  @Shared
  InsightWork work

  @Shared
  def evaluator

  @Shared
  String scanId

  @Override
  def setupSpec() {
    work = new InsightWork(serviceRule.configuration)
    app = temporaryEntity.newApplication(temporaryEntity.newOrganization().id)
    // The scanId must match the reportId value recorded inside the test report.zip used for this test
    scanId = '306e0a923df34c64b836358182b1b902'
    evaluator = new TestReportEvaluator(app, scanId, getClass().getResource(cannedTestReport), browser.baseUrl, work)
    evaluator.evaluatePolicy()
    loginAsAdminVia()
    to ReportPage, app.publicId, scanId
  }

  def "Should see Licenses tab for a known component"() {
    when: 'First navigating to a report with a known component'
    navigation.toPolicyReportPage()
    Cip cip = results[0].showCip() as Cip

    then: 'the Licenses tab is shown'
    cip.licenses.showTrigger.displayed

    when: 'opening the Licenses tab'
    cip.licenses.showTrigger.click()

    then: 'the form is shown and empty, with a disabled update button'
    waitFor { cip.licenses.form.displayed }
    LicenseModule licenses = cip.licenses as LicenseModule
    licenses.validateLicense('', '', '', app.name, 'Open', '', '', false)
  }

  def "Should have an empty Audit Log"() {
    when: 'Clicking on the Audit Log tab'
    Cip cip = results[0].cip as Cip
    AuditLogModule auditLog = cip.auditLog as AuditLogModule
    auditLog.showTrigger.click()

    then: 'An empty Audit Log is shown'
    waitFor { auditLog.noChangesMessage == 'No changes were found for this component.' }

    cleanup: 'Go back to the License tab for the next test'
    cip.licenses.showTrigger.click()
    waitFor { cip.licenses.form.displayed }
  }

  def "Can override a license for a known component"() {
    given:
    Cip cip = results[0].cip as Cip
    LicenseModule licenses = cip.licenses as LicenseModule

    when: 'Selecting to override the license'
    licenses.status = 'Overridden'

    then: 'License choices are shown'
    waitFor { licenses.selectedLicenses.dropdownButton.text() == 'None selected' }

    when: 'Two new licenses are selected'
    licenses.selectedLicenses.toggleOption('Beerware')
    licenses.selectedLicenses.toggleOption('BSL-1.0')

    then: 'The update button should be enabled'
    waitFor { !licenses.update.disabled }

    when: 'We add a comment(which is optional)'
    licenses.comment = 'Because everything goes better with beer and boost!'

    and: 'We click the update button'
    licenses.update.click()

    then: 'The audit log should be extended'
    AuditLogModule auditLog = cip.auditLog as AuditLogModule
    auditLog.showTrigger.click()
    waitFor { auditLog.audits.size() == 1 }
    auditLog.validateRow(auditLog.audits[0], 'admin', 'Overrode', 'License as BSL-1.0, Beerware',
        'Because everything goes better with beer and boost!')

    when: 'We go back to the licenses, our new info appears (the UI does not automatically update)'
    cip.licenses.showTrigger.click()

    then:
    waitFor { cip.licenses.form.displayed }
    licenses.validateLicense('', '', 'Beerware\nBSL-1.0', app.name, 'Overridden', 'Beerware, BSL-1.0', '', false)
  }
}
