/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization

import spock.lang.Stepwise

import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.JUNIT_DETAILS_FILE
import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.JUNIT_DETAILS_LIST_FILE

/**
 * Tests the repository manager(Nexus) endpoints of the clm server.
 * @since 1.12
 */
@Stepwise
class NexusCIPSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  static Map<String, Object> hdsComponentResponse

  def setupSpec() {
    hdsComponentResponse = mockComponentDetails(JUNIT_DETAILS_FILE)
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, hdsComponentResponse)

    Organization org = temporaryEntity.newOrganization('NexusCIPSpec')
    app = temporaryEntity.newApplication('NexusCIPSpec', org.id)

    loginAsAdminVia()
    to NexusCIPPage
  }

  def 'Can select an application'() {
    given: 'The application list has been loaded'
      waitFor { appSelect.displayed }

    when: 'Selecting an application from the list'
      appSelect = app.name

    then: 'Shows the application name in the select'
      appSelect.text() == app.name

    and: 'the CIP is not loaded'
      !cip.displayed
  }

  def 'Can select a GAV'() {
    when: 'Simulating user selection of a GAV with javascript'
      page.setGAV(hdsComponentResponse.groupId, hdsComponentResponse.artifactId, hdsComponentResponse.version,
          app.publicId)

    then: 'the CIP loads'
      CIPModule cip = cip
      waitFor('slow') { cip.displayed && cip.website.displayed }
      cip.group == hdsComponentResponse.groupId
      cip.artifact == hdsComponentResponse.artifactId
      cip.version == hdsComponentResponse.version
      cip.overriddenLicense == '-'
      cip.declaredLicense == 'CPL-1.0'
      cip.observedLicense == 'No Source License'
      cip.matchState == 'exact'
      cip.highestPolicyThreat == 'NA'
      cip.highestSecurityThreat == 'NA'
      cip.catalogued == '1 year ago'
      cip.identificationSource == 'Sonatype'
      cip.website.@href.startsWith(hdsComponentResponse.website) //FF at least appends a slash on the href

    and: 'a "View Details" button is present and enabled'
      cip.viewDetails.displayed
      cip.viewDetails.enabled

    and: 'a "Migrate" button is absent'
      !cip.migrate.present

    and: 'the version graph is present and has a fixed height'
      VersionGraphModule versionGraph = versionGraph
      versionGraph.displayed
      versionGraph.labels == ['Popularity', 'License Risk', 'Security Alerts']
      versionGraph.chart.@height.toInteger() == 142
  }
}
