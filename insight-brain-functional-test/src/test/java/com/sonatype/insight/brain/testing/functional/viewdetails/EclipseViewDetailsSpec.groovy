/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.testing.functional.cip.AbstractComponentDetailsSpec

import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.JUNIT_DETAILS_FILE

/**
 * @since 1.12
 */
class EclipseViewDetailsSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  static Map<String, Object> component

  def setupSpec() {
    component = mockComponentDetails(JUNIT_DETAILS_FILE)

    Organization org = temporaryEntity.newOrganization(this.getClass().simpleName)
    app = temporaryEntity.newApplication(this.getClass().simpleName, org.id)

    loginAsAdminVia()
  }

  def "Can load view details page for a particular GAV in the context of an application"() {
    when: 'loading the page with GAV and application public id'
      to EclipseViewDetailsPage,
          appId: app.publicId, groupId: component.groupId, artifactId: component.artifactId,
          version: component.version

    then: 'details for the GAV are shown'
      sectionHeaders[0] == 'Policy Violations'
      sectionHeaders[1] == 'License Analysis'
      sectionHeaders[2] == 'Security Issues'

    and: 'there are no policy violations'
      !policyViolationTable.displayed
      noPolicyViolations.displayed && noPolicyViolations.text() == 'None'

    and: 'there are no security violations'
      !securityViolationTable.displayed
      noSecurity.displayed && noSecurity.text() == 'None'

    and: 'there is a single license violation'
      licenseAnalysisTable.displayed
      LicenseViolationTableRow row = licenseAnalysisTable.rows[0]
      row.policyName == 'Weak Copyleft'
      row.declaredLicense == component.declaredLicenses[0].licenseName
      row.observedLicense == component.observedLicenses[0].licenseName
  }
}
