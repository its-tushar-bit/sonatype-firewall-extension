/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.TemporaryEntity
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.model.security.User

class ContactUserSpec
    extends BaseSpec
{

  String appName = 'TestApplication'

  private static final String USER_NAME = ContactUserSpec.class.getSimpleName()


  def setup() {
    User user = temporaryEntity.newUser(USER_NAME)
    Application app = temporaryEntity.newApplicationWithParent(appName, appName)

    Role role = temporaryEntity.newRole(false /* global */, Permission.WRITE, Permission.READ)
    temporaryEntity.newMembershipMapping(app.getId(), role.getId(), user.getUsername())
    loginAsUserVia(USER_NAME, TemporaryEntity.USER_PASSWORD_CLEAR, ApplicationManagementPage)
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
      appPage.applicationContactDialogSearchButton.click()
      waitFor { appPage.applicationContactDialogResultList.size() == 1 }

      appPage.applicationContactDialogResultList.allElements().getAt(0).click()
      appPage.applicationSaveButton.click()
      waitFor { !appPage.applicationSaveButton.displayed }

    then: 'Admin is set as contact user'
      appPage.applicationContactField.text() == "Admin BuiltIn"
  }
}
