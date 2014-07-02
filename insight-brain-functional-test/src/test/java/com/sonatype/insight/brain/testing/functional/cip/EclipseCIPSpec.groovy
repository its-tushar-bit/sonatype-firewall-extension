/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.testing.functional.utils.AbstractComponentDetailsSpec

import spock.lang.Stepwise

import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.JUNIT_DETAILS_FILE
import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.JUNIT_DETAILS_LIST_FILE

/**
 * Tests the ide endpoints of the clm server.
 * @since 1.12
 */
@Stepwise
class EclipseCIPSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  static Map<String, Object> component

  def setupSpec() {
    component = mockComponentDetails(JUNIT_DETAILS_FILE)
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, component)

    Organization org = temporaryEntity.newOrganization('EclipseCIPSpec')
    app = temporaryEntity.newApplication('EclipseCIPSpec', org.id)
  }

  def 'The initial page can be loaded without authentication'() {
    when: 'We load the CIP'
      to EclipseCIPPage

    then: 'We get the default message'
      !cip.displayed
      defaultText.displayed
      defaultText.text() == 'Select a component to view details.'
  }

  def 'Cannot load data without authenticating first'() {
    when: 'Simulating user selection of a GAV with javascript'
      page.setGAV(component.groupId, component.artifactId, component.version,
          app.publicId)

    then: 'an error message is shown'
      error.displayed
      error.text().contains('Error 401')
  }

  def 'Initially the CIP is not shown'() {
    given: 'Logged into the server'
      loginAsAdminVia()
      to EclipseCIPPage

    expect: 'the CIP is not loaded'
      !cip.displayed
      defaultText.displayed
      defaultText.text() == 'Select a component to view details.'
  }

  def 'Can select a GAV'() {
    when: 'Simulating user selection of a GAV with javascript'
      page.setGAV(component.groupId, component.artifactId, component.version,
          app.publicId)

    then: 'the CIP loads'
      CIPModule cip = cip
      waitFor('slow') { cip.displayed && cip.website.displayed }
      cip.group == component.groupId
      cip.artifact == component.artifactId
      cip.version == component.version
      cip.overriddenLicense == '-'
      cip.declaredLicense == 'CPL-1.0'
      cip.observedLicense == 'No Source License'
      cip.matchState == 'exact'
      cip.highestPolicyThreat == 'NA'
      cip.highestSecurityThreat == 'NA'
      cip.catalogued == '1 year ago'
      cip.identificationSource == 'Sonatype'
      cip.website.@href.startsWith(component.website) //FF at least appends a slash on the href

    and: 'a "View Details" button is present and enabled'
      cip.viewDetails.displayed
      cip.viewDetails.enabled

    and: 'a "Migrate" button is present and disabled'
      cip.migrate.displayed
      !cip.migrate.enabled

    and: 'the version graph is present and has a fixed height'
      VersionGraphModule versionGraph = versionGraph
      versionGraph.displayed
      versionGraph.labels == ['Popularity', 'License Risk', 'Security Alerts']
      versionGraph.chart.@height.toInteger() == 142
  }
}
