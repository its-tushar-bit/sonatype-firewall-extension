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
class NexusViewDetailsSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  static Map<String, Object> hdsComponentResponse

  def setupSpec() {
    hdsComponentResponse = mockComponentDetails(JUNIT_DETAILS_FILE)

    Organization org = temporaryEntity.newOrganization(this.getClass().simpleName)
    app = temporaryEntity.newApplication(this.getClass().simpleName, org.id)

    loginAsAdminVia()
  }

  def "Can load view details page for a particular GAV in the context of an application"() {
    when: 'loading the page with GAV and application public id'
      to NexusViewDetailsPage,
          appId: app.publicId, groupId: hdsComponentResponse.groupId, artifactId: hdsComponentResponse.artifactId,
          version: hdsComponentResponse.version

    then: 'details for the GAV are shown'
      waitFor { at NexusViewDetailsPage }
      String gav = [hdsComponentResponse.groupId, hdsComponentResponse.artifactId, hdsComponentResponse.version].
          join(':')
      sectionHeaders[0] == "CLM Details for ${gav} in the context of CLM Application ${app.name}"
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
      row.policyName == 'Weak Copyleft'
      row.declaredLicense == hdsComponentResponse.declaredLicenses[0].licenseName
      row.observedLicense == hdsComponentResponse.observedLicenses[0].licenseName
  }
}
