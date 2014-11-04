/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.testing.functional.utils.AbstractComponentDetailsSpec

import spock.lang.Stepwise
/**
 * Tests the repository manager(Nexus) endpoints of the clm server.
 * @since 1.12
 */
@Stepwise
class NexusCIPSpec
    extends AbstractComponentDetailsSpec
{
  static Application app

  def setupSpec() {
    Organization org = temporaryEntity.newOrganization('NexusCIPSpec')
    app = temporaryEntity.newApplication('NexusCIPSpec', org.id)
  }

  def 'The application names are available without authentication'() {
    when: 'First loading the CIP'
      to NexusCIPPage

    then: 'Application names are available to choose from'
      waitFor { options == [app.name] }
      selectAnAppText.displayed
      selectAnAppText.text() == 'Select an application.'
  }

  def 'Can select an application'() {
    given: 'The application list has been loaded'
      waitFor { appSelect.displayed }

    when: 'Selecting an application from the list'
      appSelect = app.name

    then: 'Shows the application name in the select'
      appSelect.text() == app.name
      defaultText.displayed
      defaultText.text() == SELECT_COMPONENT

    and: 'the CIP is not loaded'
      !cip.displayed
  }

  def 'Cannot load data without authenticating first'() {
    when: 'Simulating user selection of a GAV with javascript'
      page.setGav(JUNIT.groupId, JUNIT.artifactId, JUNIT.version,
          app.publicId)

    then: 'an error message is shown'
      waitFor { error.displayed }
      error.text().contains('Error 401')
  }

  def 'Can select a GAV once logged in'() {
    given: 'Logged into the server'
      loginAsAdminVia()
      to NexusCIPPage

    expect: 'The previous choice for application is still there'
      waitFor { appSelect.text() == app.name }

    when: 'Simulating user selection of a GAV with javascript'
      page.setGav(JUNIT.groupId, JUNIT.artifactId, JUNIT.version, app.publicId)

    then: 'the CIP loads'
      CIPModule cip = cip
      waitFor('slow') { cip.displayed && cip.website.displayed }
      validateCommon(cip, JUNIT)
      cip.website.@href.startsWith(JUNIT.website) //FF at least appends a slash on the href
      cip.highestPolicyThreat == 'NA'
      cip.highestSecurityThreat == 'NA'
    //TODO - KR fix timing consideration from canned data
      //cip.catalogued == '1 year ago'
      cip.identificationSource == 'Sonatype'
      cip.website.@href.startsWith(JUNIT.website) //FF at least appends a slash on the href

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

    and: 'the select text is no longer shown'
      !defaultText.displayed
  }

  def "The assigned GAV can be removed"() {
    when: 'We simulate the client clearing the GAV information'
      page.clearGav()

    then: 'We are back to being asked to select a component'
      waitFor { defaultText.displayed }
      defaultText.text() == SELECT_COMPONENT
  }

  def "Local policy changes are reflected the next time details are loaded"() {
    given: 'A new policy is added that our viewed component violates'
      Policy policy = createLicensePolicy(app.id, this.getClass().simpleName, JUNIT.declaredLicenses[0].licenseName)

    when: 'We set the GAV'
      page.setGav(JUNIT.groupId, JUNIT.artifactId, JUNIT.version, app.publicId)

    then: 'The changes should be reflected in the component details'
      CIPModule cip = cip
      waitFor('slow') { cip.displayed && cip.website.displayed }
      cip.highestPolicyThreat.toInteger() == policy.threatLevel
  }

  def "Security vulnerabilities are highlighted"() {
    when: 'We load a component with known security vulnerabilities'
      page.setGav(CATALINA_HOST_MANAGER.groupId, CATALINA_HOST_MANAGER.artifactId, CATALINA_HOST_MANAGER.version,
          app.publicId)

    then: 'Details of the vulnerabilities are shown'
      CIPModule cip = cip
      waitFor('slow') { cip.displayed && cip.getNameField('Group') }
      validateCommon(cip, CATALINA_HOST_MANAGER)
      cip.highestSecurityThreat == '4.3 within 4 security issues'

    and: 'No website information is provided for this GAV'
      !cip.website.displayed
  }
  
  String getToolName() {
    return "rm"
  }
}
