/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.security.Permission

class ContactUserSpec
    extends BaseSpec
{

  String appName = 'TestApplication'

  private static final String USER_NAME = ContactUserSpec.class.getSimpleName()


  def setup() {
    createUser()
    Application app = temporaryEntity.newApplicationWithParent(appName, appName)

    grantPermissions(getUsername(), app.getId(), Permission.WRITE, Permission.READ)
    loginAsUserVia(OwnerManagementPage)
  }

  def cleanup() {
    cleanAppsAndOrgs()
  }

  def "validate application contact"() {

    given: 'User accesses application'
    OwnerManagementPage ownerManagementPage = at OwnerManagementPage
    ownerManagementPage.selectApplication(appName, appName)

    when: 'User selects admin as contact'
      ApplicationPage appPage = at ApplicationPage
      appPage.applicationContactField.click()
      waitFor { appPage.applicationContactDialogSearchField.displayed }

      appPage.applicationContactDialogSearchField << 'admin*'
      appPage.applicationContactDialogSearchButton.click()
      waitFor { appPage.applicationContactDialogResultList.size() == 1 }

      appPage.applicationContactDialogResultList.allElements().getAt(0).click()
      appPage.applicationSaveButton.click()
      waitFor { !appPage.applicationSaveButton.displayed }

    then: 'Admin is set as contact user'
      appPage.applicationContactField.text() == "Admin BuiltIn"
  }
}
