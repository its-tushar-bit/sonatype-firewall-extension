/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class ContactUserSpec
    extends BaseSpec
{

  String appName = 'TestApplication'

  def setup() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrg()

    ApplicationManagementPage applicationManagementPage = to ApplicationManagementPage
    applicationManagementPage.createApp(appName, appName)
    at ApplicationPage
    waitFor { applicationList.size() == 1 }
  }

  def cleanup() {
    cleanAppsAndOrgs()
  }

  def "validate application contact"() {

    given: 'User accesses application'
      ApplicationManagementPage appManPage = to ApplicationManagementPage
      waitFor { appManPage.application(appName).displayed }
      appManPage.application(appName).click()

    when: 'User selects admin as contact'
      ApplicationPage appPage = at ApplicationPage
      appPage.applicationContactField.click()
      waitFor { appPage.applicationContactDialog.displayed }

      appPage.applicationContactDialogSearchField << 'admin'
      waitFor { appPage.applicationContactDialogResultList.size() == 1 }

      appPage.applicationContactDialogResultList.allElements().getAt(0).click()
      appPage.applicationSaveButton.click()
      waitFor { !appPage.applicationSaveButton.displayed }

    then: 'Admin is set as contact user'
      appPage.applicationContactField.text() == "Admin BuiltIn"
  }
}
