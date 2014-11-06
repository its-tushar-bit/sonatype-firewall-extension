/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

import com.sonatype.clm.dto.model.SecurityVulnerability
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.testing.functional.utils.AbstractComponentDetailsSpec

import spock.lang.Stepwise

/**
 * @since 1.12
 */
@Stepwise
class EclipseViewDetailsSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  def setupSpec() {
    Organization org = temporaryEntity.newOrganization(this.getClass().simpleName)
    app = temporaryEntity.newApplication(this.getClass().simpleName, org.id)
  }

  def 'Does not load without authentication'() {
    when: 'trying to load the page without being authenticated'
      via EclipseViewDetailsPage,
          appId: app.publicId, groupId: JUNIT.groupId, artifactId: JUNIT.artifactId,
          version: JUNIT.version

    then: 'an authentication error is shown'
      waitFor { error.displayed }
      error.text().startsWith('Authentication with the CLM Server failed.')
  }

  def 'Does not load with invalid authentication'() {
    when: 'trying to load the page with invalid authentication'
      via EclipseViewDetailsPage,
          appId: app.publicId, groupId: JUNIT.groupId, artifactId: JUNIT.artifactId,
          version: JUNIT.version
      page.setAuthHeaders('foo', 'bar')

    then: 'an authentication error is shown'
      waitFor { error.displayed }
      error.text().startsWith('Authentication with the CLM Server failed.')
  }

  def "Can load view details page for a particular GAV in the context of an application"() {
    when: 'loading the page with GAV and application public id'
      to EclipseViewDetailsPage,
          appId: app.publicId, groupId: JUNIT.groupId, artifactId: JUNIT.artifactId,
          version: JUNIT.version, deferLoad: true

    and: 'setting authentication headers for requests'
      page.setAuthHeaders(User.ADMIN_USERNAME, 'admin123')

    then: 'details for the GAV are shown'
      waitFor('slow') { sectionHeaders.size == 3 }
      sectionHeaders[0] == 'Policy Violations'
      sectionHeaders[1] == 'License Analysis'
      sectionHeaders[2] == 'Security Issues'

    and: 'there are no policy violations'
      !policyViolationTable.displayed
      noPolicyViolations.displayed
      noPolicyViolations.text() == 'None'

    and: 'there are no security violations'
      !securityViolationTable.displayed
      noSecurity.displayed && noSecurity.text() == 'None'

    and: 'there is a single license violation'
      licenseAnalysisTable.displayed
      LicenseViolationTableRow row = licenseAnalysisTable.rows[0]
      row.policyName == 'Weak Copyleft'
      row.declaredLicense == JUNIT.declaredLicenses[0].licenseName
      row.observedLicense == JUNIT.observedLicenses[0].licenseName
  }

  def "Local policy changes are reflected the next time details are loaded"() {
    given: 'A new policy is added that our viewed component violates'
      Policy policy = createLicensePolicy(app.id, this.getClass().simpleName, JUNIT.declaredLicenses[0].licenseName)

    when: 'We refresh the page'
      page.reload()

    then: 'The changes should be reflected in the component details'
      waitFor('slow') { policyViolationTable.displayed }
      PolicyViolationTableRow row = policyViolationTable.rows[0]
      row.policyName == policy.name
      row.constraintName == policy.constraints[0].name
      row.summary.contains('No Source License')
      row.summary.contains(JUNIT.declaredLicenses[0].licenseName)
  }

  def "Security vulnerabilities are highlighted"() {
    when: 'We load a component with known security vulnerabilities'
      to EclipseViewDetailsPage,
          appId: app.publicId, groupId: CATALINA_HOST_MANAGER.groupId, artifactId: CATALINA_HOST_MANAGER.artifactId,
          version: CATALINA_HOST_MANAGER.version, deferLoad: true
      page.reload()

    then: 'Details of the vulnerabilities are shown'
      waitFor { securityViolationTable.displayed }
      securityViolationTable.rows.size() == 4
      SecurityViolationTableRow row = securityViolationTable.rows[0]
      SecurityVulnerability mockSecurityVulnerability = CATALINA_HOST_MANAGER.securityVulnerabilities[0]

      row.threatLevel == Math.floor(mockSecurityVulnerability.severity)
      row.problemCode.text() == mockSecurityVulnerability.refId
      row.status == mockSecurityVulnerability.status ?: 'Open'
      row.summary == mockSecurityVulnerability.summary
  }

  public String getToolName() {
    return 'ide'
  }
}
