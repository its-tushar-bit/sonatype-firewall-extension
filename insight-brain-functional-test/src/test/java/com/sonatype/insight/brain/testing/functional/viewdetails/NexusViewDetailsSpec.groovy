/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

import com.sonatype.clm.dto.model.SecurityVulnerability
import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.testing.functional.utils.AbstractComponentDetailsSpec
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper

import spock.lang.Stepwise
/**
 * @since 1.12
 */
@Stepwise
class NexusViewDetailsSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  @Override
  def setupSpec() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(temporaryEntity)

    Organization org = temporaryEntity.newOrganization(this.getClass().simpleName)
    app = temporaryEntity.newApplication(this.getClass().simpleName, org.id)
  }

  def 'Does not load without authentication'() {
    when: 'trying to load the page without being authenticated'
      via NexusViewDetailsPage,
          appId: app.publicId, groupId: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID],
          artifactId: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID],
          version: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.VERSION]

    then: 'an authentication error is shown'
      waitFor { error.displayed }
      error.text().startsWith('Authentication with the Nexus IQ Server failed.')
  }

  def "Can load view details page for a particular GAV in the context of an application"() {
    given: 'Logged into the server'
      loginAsAdminVia()

    when: 'loading the page with GAV and application public id'
      to NexusViewDetailsPage,
          appId: app.publicId, groupId: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID],
          artifactId: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID],
          version: JUNIT.componentIdentifier.coordinates[ComponentIdentifier.VERSION]

    then: 'details for the GAV are shown'
      String gav = [JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID],
                    JUNIT.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID],
                    JUNIT.componentIdentifier.coordinates[ComponentIdentifier.VERSION]].join(' : ')
      sectionHeaders[0] == "Component Details for ${gav} in the context of IQ Application ${app.name}"
      sectionHeaders[1] == 'Policy Violations'
      sectionHeaders[2] == 'License Analysis'
      sectionHeaders[3] == 'Security Issues'

    and: 'there are no policy violations'
      !policyViolationTable.displayed
      noPolicyViolations.displayed && noPolicyViolations.text() == 'None'

    and: 'there are no security violations'
      !securityViolationTable.displayed
      noSecurity.displayed && noSecurity.text() == 'None'

    and: 'there is a single license violation'
      licenseAnalysisTable.displayed
      LicenseViolationTableRow row = licenseAnalysisTable.rows[0]
      row.policyName == 'Sonatype Special Licenses'
      row.declaredLicense == JUNIT.declaredLicenses[0].licenseName
      row.observedLicense == JUNIT.observedLicenses[0].licenseName
  }

  def "Local policy changes are reflected the next time details are loaded"() {
    given: 'A new policy is added that our viewed component violates'
      Policy policy = createLicensePolicy(app.id, this.getClass().simpleName, JUNIT.declaredLicenses[0].licenseName)

    when: 'We refresh the page'
      driver.navigate().refresh()

    then: 'The changes should be reflected in the component details'
      waitFor { policyViolationTable.displayed }
      PolicyViolationTableRow row = policyViolationTable.rows[0]
      row.policyName == policy.name
      row.constraintName == policy.constraints[0].name
      row.summary.contains('Found \'CPL-1.0\' license')
  }

  def "Security vulnerabilities are highlighted"() {
    when: 'We load a component with known security vulnerabilities'
      to NexusViewDetailsPage, appId: app.publicId,
          groupId: CATALINA_HOST_MANAGER.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID],
          artifactId: CATALINA_HOST_MANAGER.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID],
          version: CATALINA_HOST_MANAGER.componentIdentifier.coordinates[ComponentIdentifier.VERSION]

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

  @Override
  public String getToolName() {
    return 'rm';
  }
}
