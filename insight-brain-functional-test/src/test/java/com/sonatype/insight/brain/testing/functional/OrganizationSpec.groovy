/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
    loginAsAdmin()
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can create a new Organization"() {
    when: 'We add a new Organization'
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    organizationManagementPage.createOrg('New Organization')

    then: 'we are left at the Organization page, and the newly created Org appears in the list of Organizations'
    at OrganizationPage
    organizationList.size() == 1
    organization('New Organization').displayed
    organizationName.text() == 'New Organization'
  }

  def "Can edit an existing Organization"() {
    when: 'We edit the Organization name'
    editOrg('New Organization Updated')

    then: 'the list is updated'
    organizationList.size() == 1
    organization('New Organization Updated').displayed
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
    waitFor{ at OrganizationManagementPage }
    organizationList.empty
  }

  def "When adding new Organizations, they are listed alphabetically"(){
    when: 'we add multiple Organizations'
    createOrg('Z')
    createOrg('A')

    then: 'they are listed alphabetically'
    organizationList.collect{ it.text() } == ['A','Z']
  }
}
