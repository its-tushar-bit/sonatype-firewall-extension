/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

/**
 * @since 1.8
 */
@Stepwise
class OrganizationSpec
    extends BaseSpec
{
  def setupSpec() {
    loginAsAdminVia()
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can create a new Organization"() {
    when: 'We click the root Organization'
      OwnerManagementPage ownerManagementPage = to(OwnerManagementPage)
      ownerManagementPage.ownerTreeView.rootOrganization.treeViewElement.click()

    then: 'New organization button is displayed'
      waitFor { ownerManagementPage.ownerTreeView.rootOrganization.newOrganizationButton.displayed }

    when: 'We click the new organization button'
      ownerManagementPage.ownerTreeView.rootOrganization.newOrganizationButton.click()
      OrganizationPage organizationPage = at(OrganizationPage)

    then: 'Organization controls are not visible'
      !organizationPage.tools.deleteButton.present
      !organizationPage.tools.appEvalButton.present

    when: 'Organization name is entered'
      organizationPage.organizationName.click()
      waitFor { organizationPage.organizationNameField.displayed }
      organizationPage.organizationNameField = 'New Organization'

    then: 'Organization controls are still not visible'
      !organizationPage.tools.deleteButton.present
      !organizationPage.tools.appEvalButton.present

    when: 'We click save'
      organizationPage.organizationSaveButton.click()
      waitFor { !organizationPage.organizationSaveButton.displayed }

    then: 'we are left at the Organization page'
      at OrganizationPage
      waitFor { organizationName.text() == 'New Organization' }

    and: 'the policy tab is shown by default'
      waitFor { policies.displayed }

    and: 'and the newly created Org appears in the list of Organizations'
    ownerManagementPage.ownerTreeView.organizations.size() == 1
    ownerManagementPage.ownerTreeView.organization('New Organization').displayed
  }

  def "Can edit an existing Organization"() {
    when: 'We edit the Organization name'
      editOrg('New Organization Updated')

    then: 'the list is updated'
    ownerTreeView.organizations.size() == 1
    ownerTreeView.organization('New Organization Updated').displayed
      organizationName.text() == 'New Organization Updated'
  }

  def "Can delete an existing Organization"() {
    when: 'We click the delete button'
      deleteButton.click()

    then: 'we are presented with a confirmation dialog'
      waitFor { deleteButtonAccept.displayed }

    when: 'we agree to delete the Organization'
      deleteButtonAccept.click()

    then: 'the list of Orgs is now empty'
      at OwnerManagementPage
      waitFor { ownerTreeView.organizations.size() == 0 }
  }

  def "When adding new Organizations, they are listed alphabetically"() {
    when: 'we add multiple Organizations'
    createOrganization('Z')
    createOrganization('A')

    then: 'they are listed alphabetically'
    ownerTreeView.organizations.collect { it.getName() } == ['A', 'Z']
  }

  def "Can add a new Policy"() {
    given: 'The policy tab has loaded'
      waitFor { policies.displayed }

    when: "We add a new Policy"
      policies.newPolicyButton.click()
      def policyEditor = policies.newPolicyEditor
      policyEditor.name = 'NewPolicy'

      def constraint = policyEditor.constraints[0]
      constraint.editButton.click()
      constraint.constraintName = 'Constraint'
      waitFor { constraint.conditions[0].value.displayed }
      constraint.conditions[0].value = '1'
      policyEditor.buttons.save.click()

    then: "it shows up in the list of policies"
      waitFor { policies.findPolicyEditor('NewPolicy').displayed }
  }
}
