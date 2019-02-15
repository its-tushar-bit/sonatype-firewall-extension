/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.testing.functional.utils.AbstractComponentDetailsSpec

import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * Tests the repository manager(Nexus) endpoints of the clm server.
 * @since 1.12
 */
@Stepwise
class NexusCIPSpec
    extends AbstractComponentDetailsSpec
{
  static Application app
  static Application app1
  static Application app2
  static Policy violatedPolicy = null

  String optionLabelFor(Application app) {
    return app.name + " (" + app.publicId + ")";
  }

  @Override
  def setupSpec() {
    app2 = temporaryEntity.newApplicationWithParent('SomeApp', 'Some App')
    app1 = temporaryEntity.newApplicationWithParent('app-123', 'app-123')
    app = temporaryEntity.newApplicationWithParent('MY-APP', 'My App')
  }

  def 'Can log in to the server'() {
    when: 'logging in as admin'
      loginAsAdminVia()

    then: 'should see the logout link'
      waitFor { userOptions.logout.present }
  }

  def 'Previous session'() {
    when: 'First loading the CIP'
      to NexusCIPPage

    then: 'Applications are available to choose from in alphabetical order'
      waitFor { options == [optionLabelFor(app1), optionLabelFor(app), optionLabelFor(app2)] }
      selectAnAppText.displayed
      selectAnAppText.text() == 'Select an application.'
  }

  def 'Can select an application'() {
    given: 'The application list has been loaded'
      waitFor { appSelect.displayed }

    when: 'Selecting an application from the list'
      appSelect = optionLabelFor(app)

    then: 'Shows the application name in the select'
      appSelectText == optionLabelFor(app)
      defaultText.displayed
      defaultText.text() == SELECT_COMPONENT

    and: 'the CIP is not loaded'
      !cip.displayed
  }

  @Unroll
  def 'Can select a Component by #tests.name once logged in'() {
    given: 'Logged into the server'
      to NexusCIPPage

    expect: 'The previous choice for application is still there'
      waitFor { appSelectText == optionLabelFor(app) }

    when: 'Simulating user selection of a Component with javascript'
      tests.setCoordinates()

    then: 'the CIP loads'
      CIPModule cip = cip as CIPModule
      validateMavenComponent(cip, JUNIT)
      cip.website.@href.startsWith(JUNIT.website) //FF at least appends a slash on the href
      cip.highestPolicyThreat == 'NA'
      cip.highestSecurityThreat == 'NA'

    and: 'a "View Details" button is present and enabled'
      cip.viewDetails.displayed
      !cip.viewDetails.hasClass('disabled')

    and: 'a "Migrate" button is absent'
      !cip.migrate.present

    and: 'the version graph is present and has a fixed height'
      verifyVersionGraph(versionGraph as VersionGraphModule)

    and: 'the select text is no longer shown'
      !defaultText.displayed

    where:
      tests << [ [
          name: 'Legacy GAV',
          setCoordinates: { page.setGav(JUNIT.groupId, JUNIT.artifactId, JUNIT.version, app.publicId) }
        ],[
          name: 'Component Identifier',
          setCoordinates: { page.setCoordinates(JUNIT.componentIdentifier, app.publicId) }
        ]
      ]
  }

  def "The assigned GAV can be removed"() {
    when: 'We simulate the client clearing the GAV information'
      page.clearGav()

    then: 'We are back to being asked to select a component'
      waitFor { defaultText.displayed }
      defaultText.text() == SELECT_COMPONENT
  }

  @Unroll
  def "Local policy changes are reflected the next time details are loaded by #tests.name"() {
    given: 'A new policy is added that our viewed component violates'
      if (violatedPolicy == null) {
        violatedPolicy = createLicensePolicy(app.id, this.getClass().simpleName, JUNIT.declaredLicenses[0].licenseName)
      }

    when: 'We set the Component'
      tests.setCoordinates()

    then: 'The changes should be reflected in the component details'
      CIPModule cip = cip as CIPModule
      cip.highestPolicyThreat.toInteger() == violatedPolicy.threatLevel

    where:
      tests << [ [
            name: 'Legacy GAV',
            setCoordinates: { page.setGav(JUNIT.groupId, JUNIT.artifactId, JUNIT.version, app.publicId) }
        ],[
            name: 'Component Identifier',
            setCoordinates: { page.setCoordinates(JUNIT.componentIdentifier, app.publicId) }
        ]
      ]
  }

  @Unroll
  def "Security vulnerabilities for #tests.name are highlighted"() {
    when: 'We load a component with known security vulnerabilities'
      tests.setCoordinates()

    then: 'Details of the vulnerabilities are shown'
      CIPModule cip = cip as CIPModule
      validateMavenComponent(cip, CATALINA_HOST_MANAGER)
      cip.highestSecurityThreat == '4.3 within 4 security issues'

    and: 'No website information is provided for this GAV'
      !cip.website.displayed

    where:
      tests << [ [
          name: 'Legacy GAV',
          setCoordinates: {
            page.setGav(CATALINA_HOST_MANAGER.groupId, CATALINA_HOST_MANAGER.artifactId, CATALINA_HOST_MANAGER.version,
                app.publicId)
          }
        ], [
          name: 'Component Identifier',
          setCoordinates: { page.setCoordinates(CATALINA_HOST_MANAGER.componentIdentifier, app.publicId) }
        ]
      ]
  }

  def "Can show classifier and extension"() {
    when: 'We load a component with both classifier and extension'
    setCoordinates(PREZI_DIST.componentIdentifier, app.publicId)

    then: 'the CIP loads and all GAVEC coordinate information is shown'
    CIPModule cip = cip as CIPModule
    validateMavenComponent(cip, PREZI_DIST)
    cip.getNameField('Extension') == PREZI_DIST.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_EXTENSION]
    cip.getNameField('Classifier') == PREZI_DIST.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_CLASSIFIER]
  }

  def 'Can select a NuGet Component'() {
    when: 'We load a NuGet component'
    setCoordinates(ENTITY_FRAMEWORK.componentIdentifier, app.publicId)

    then: 'the CIP loads and shows the expected fields'
    CIPModule cip = cip as CIPModule
    validateNuGetComponent(cip, ENTITY_FRAMEWORK)
    cip.highestPolicyThreat == 'NA'
    cip.highestSecurityThreat == 'NA'
  }
  
  @Override
  String getToolName() {
    return "rm"
  }
}
