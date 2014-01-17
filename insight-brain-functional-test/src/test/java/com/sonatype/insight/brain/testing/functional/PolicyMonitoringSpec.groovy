/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import spock.lang.Stepwise

/**
 * @since 1.8
 */
@Stepwise
class PolicyMonitoringSpec
    extends BaseSpec
{
  static Organization org

  static Application app

  def setupSpec() {
    org = new Organization(name: 'PolicyMonitoring')
    organizationDAO.insert(org)
    app = new Application('PolicyMonitoring', 'PolicyMonitoring', org.id)
    applicationDAO.insert(app)
  }

  def cleanupSpec() {
    applicationDAO.delete(app)
    organizationDAO.delete(org)
  }

  def "Initially policy monitoring is not configured"() {
    setup:
    to ReportViolationsPage
    login.loginAsAdmin()
    to OrganizationPage, org.id, 'policies'

    when:
    policyMonitoring.expandButton.click()

    then:
    waitFor { policyMonitoring.form.displayed }
    policyMonitoring.selectedOptionText == '-- do not monitor --'
  }

  def "We can configure a stage for monitoring on the Organization"() {
    when:
    policyMonitoring.form.policyMonitoring = 'Build'

    then:
    policyMonitoring.selectedOptionText == 'Build'
  }

  def "And then observe that the Application inherits this setting"() {
    setup:
    to ApplicationPage, app.publicId, 'policies'

    when:
    policyMonitoring.expandButton.click()

    then:
    waitFor { policyMonitoring.form.displayed }
    policyMonitoring.selectedOptionText == 'Build (inherited from parent)'
  }

  def "We can then override the monitoring stage on the app"() {
    when:
    policyMonitoring.form.policyMonitoring = 'Release'

    then:
    policyMonitoring.selectedOptionText == 'Release'
  }
}
