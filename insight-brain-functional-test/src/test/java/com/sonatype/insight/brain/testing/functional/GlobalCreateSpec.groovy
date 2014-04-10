/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

@Stepwise
class GlobalCreateSpec
    extends BaseSpec 
{

  static app;

  def setupSpec() {
    loginAsAdminVia(ReportViolationsPage)
    app = temporaryEntity.newApplicationWithParent('testing', 'Testing')
  }

  def 'Main header shows global create button'() {
    when: 'the user has logged in'
      waitFor { userOptions.displayName.displayed }

    then: 'the global create button is displayed'
      globalCreate.dropdown.displayed
  }

  def 'Clicking global create button allows to create new organization'() {
    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Organization" entry is displayed'
      waitFor { globalCreate.newOrganization.displayed }

    when: 'clicking "New Organization"'
      globalCreate.newOrganization.click()

    then: 'the organization editor is brought up'
      at OrganizationPage
      waitFor { organizationSaveButton.displayed }
  }

  def 'Clicking global create button allows to create new application'() {
    given: 'viewing an existing organization'
      to OrganizationPage, app.organizationId
      waitFor { organizationName.text() == 'Testing' }

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Application" entry is displayed'
      waitFor { globalCreate.newApplication.displayed }

    when: 'clicking "New Application"'
      globalCreate.newApplication.click()

    then: 'the application editor is brought up'
      at ApplicationPage
      waitFor { applicationSaveButton.displayed }

    and: 'the previously viewed organization is pre-selected as parent'
      waitFor { applicationOrgField.text() == 'Testing' }
  }

  def 'Clicking global create button allows to create new policy from application'() {
    given: 'viewing an existing application'
      to ApplicationPage, app.publicId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Policy" entry is displayed'
      waitFor { globalCreate.newPolicy.displayed }

    when: 'clicking "New Policy"'
      globalCreate.newPolicy.click()

    then: 'the policy editor is brought up'
      waitFor { policies.displayed }
      waitFor { policies.newPolicyEditor.displayed }
  }
  
  def 'Clicking global create button allows to create new policy from organization'() {
    given: 'viewing an existing organization'
      to OrganizationPage, app.organizationId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Policy" entry is displayed'
      waitFor { globalCreate.newPolicy.displayed }

    when: 'clicking "New Policy"'
      globalCreate.newPolicy.click()

    then: 'the policy editor is brought up'
      waitFor { policies.displayed }
      waitFor { policies.newPolicyEditor.displayed }
  }

  def 'Clicking global create button allows to create new label from application'() {
    given: 'viewing an existing application'
      to ApplicationPage, app.publicId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Label" entry is displayed'
      waitFor { globalCreate.newLabel.displayed }

    when: 'clicking "New Label"'
      globalCreate.newLabel.click()

    then: 'the label editor is brought up'
      waitFor { labels.displayed }
      waitFor { labels.labelEditor.displayed }
  }
  
  def 'Clicking global create button allows to create new label from organization'() {
    given: 'viewing an existing organization'
      to OrganizationPage, app.organizationId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New Label" entry is displayed'
      waitFor { globalCreate.newLabel.displayed }

    when: 'clicking "New Label"'
      globalCreate.newLabel.click()

    then: 'the label editor is brought up'
      waitFor { labels.displayed }
      waitFor { labels.labelEditor.displayed }
  }

  def 'Clicking global create button allows to create new license threat group from application'() {
    given: 'viewing an existing application'
      to ApplicationPage, app.publicId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New License Threat Group" entry is displayed'
      waitFor { globalCreate.newLicenseThreatGroup.displayed }

    when: 'clicking "New License Threat Group"'
      globalCreate.newLicenseThreatGroup.click()

    then: 'the license threat group editor is brought up'
      waitFor { licenseThreatGroups.displayed }
      waitFor { licenseThreatGroups.ltgEditor.displayed }
  }
  
  def 'Clicking global create button allows to create new license threat group from organization'() {
    given: 'viewing an existing organization'
      to OrganizationPage, app.organizationId

    when: 'clicking the create button'
      globalCreate.dropdown.click()

    then: 'the "New License Threat Group" entry is displayed'
      waitFor { globalCreate.newLicenseThreatGroup.displayed }

    when: 'clicking "New License Threat Group"'
      globalCreate.newLicenseThreatGroup.click()

    then: 'the license threat group editor is brought up'
      waitFor { licenseThreatGroups.displayed }
      waitFor { licenseThreatGroups.ltgEditor.displayed }
  }
}
