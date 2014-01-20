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
class ApplicationSpec extends BaseSpec
{
  def setupSpec() {
    loginAsAdmin()
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    organizationManagementPage.createOrg()
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can create a new Application"() {
    when: 'We add a new Application'
    ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
    applicationManagementPage.createApp('New Application', 'New Application')

    then: 'we are left at the Application page, and the newly created App appears in the list of Applications'
    at ApplicationPage
    waitFor { applicationList.size() == 1 }
    application('New Application').displayed
    waitFor { applicationName.text() == 'New Application' }
    applicationIdSaved.text() == 'New Application'
  }

  def "Can edit an existing Application"() {
    when: 'We edit the Application name'
    editApp('New Application Updated')

    then: 'the list is updated'
    applicationList.size() == 1
    waitFor { application('New Application Updated').displayed }
    applicationName.text() == 'New Application Updated'
  }

  def "Can delete an existing Application"() {
    when: 'We click the delete button'
    deleteButton.click()

    then: 'we are presented with a confirmation dialog'
    waitFor { deleteButtonAccept.displayed }

    when: 'we agree to delete the Application'
    deleteButtonAccept.click()

    then: 'the list of applications is empty'
    waitFor{ at ApplicationManagementPage }
    applicationList.empty
  }

  def "When adding new Applications, they are listed alphabetically"(){
    when: 'we add multiple Applications'
    createApp('Z')
    waitFor { applicationList.size() == 1 }
    createApp('A', 'a')

    then: 'they are listed alphabetically'
    waitFor { applicationList.size() == 2 }
    applicationList.collect{ it.text() } == ['A','Z']
  }
}
