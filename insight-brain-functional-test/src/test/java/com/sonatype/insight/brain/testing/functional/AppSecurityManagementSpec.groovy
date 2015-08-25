/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO
import com.sonatype.insight.brain.model.Application
import com.sonatype.insight.brain.model.security.Permission

class AppSecurityManagementSpec
    extends BaseSpec
{
  String orgId

  String appPublicId

  def setupSpec() {
    createUser()
  }

  // assumes a license has already been installed
  // get to the organizations page
  def setup() {
    orgId = temporaryEntity.newOrganization('Test Org').id
    Application app = temporaryEntity.newApplication('Test App', 'test-app', orgId)
    appPublicId = app.publicId

    grantPermissions(getUsername(), orgId, Permission.READ)
    grantPermissions(getUsername(), app.getId(), Permission.WRITE, Permission.READ)
    loginAsUserVia(OwnerManagementPage)
  }

  def cleanup() {
    cleanAppsAndOrgs()
    userOptions.logoutClick()
  }

  def "validate organization roles"() {
    when: "Open Security Tab"
      to OrganizationPage, orgId
      tabs.securityTabButton.click()

    then: "security tab content is shown"
      waitFor { tabs.securityTab.displayed }
      waitFor { tabs.securityTab.role("Developer").displayed }
      waitFor { tabs.securityTab.role("Owner").displayed }
  }

  def "validate application roles"() {
    when: "Navigating to an application"
      to ApplicationPage, appPublicId

    then: "see the security tab shown"
      waitFor { tabs.securityTabButton.displayed }

    when: "user clicks on security tab"
      tabs.securityTabButton.click()

    then: "security tab is shown"
      waitFor { tabs.securityTab.displayed }
      waitFor { tabs.securityTab.role("Developer").displayed }
      waitFor { tabs.securityTab.role("Owner").displayed }
  }

  def 'Can directly enter role members when group search is disabled'() {
    given: 'an LDAP server with group search disabled'
      String serverId = temporaryEntity.newLdapServer('LDAP').id
      temporaryEntity.newLdapConnection(serverId)
      LdapUserMapping userMapping = temporaryEntity.newLdapUserMapping(serverId)
      userMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC)
      userMapping.setDynamicGroupSearchEnabled(false)
      new LdapUserMappingDAO().update(userMapping)

    when: 'navigating to the security tab of some application'
      to ApplicationPage, appPublicId
      waitFor { tabs.securityTabButton.displayed }
      tabs.securityTabButton.click()

    then: 'the security tab shows the built-in roles'
      waitFor { tabs.securityTab.displayed }
      waitFor { tabs.securityTab.role('Developer').displayed }
      waitFor { tabs.securityTab.role('Owner').displayed }

    when: 'clicking the edit button for a role'
      def roleRow = tabs.securityTab.role('Developer')
      interact {
        moveToElement(roleRow)
      }
      roleRow.editButton.click()

    then: 'the users & groups buttons are visible'
      roleRow.usersButton.displayed
      roleRow.groupsButton.displayed

    and: 'only the users button is marked as active'
      roleRow.usersButton.hasClass('active')
      !roleRow.groupsButton.hasClass('active')

    when: 'entering a username query'
      roleRow.queryInput.value('admin*')
      roleRow.searchButton.click();

    then: 'the matching users are listed as usual'
      waitFor { roleRow.availableMembers.size() == 1 }
      roleRow.availableMemberNames*.text().sort() == ['Admin BuiltIn']

    when: 'entering a group name'
      roleRow.queryInput.value('admin')
      
    and: 'clicking the groups button'
      roleRow.groupsButton.click()

    then: 'the available members list is hidden'
      !roleRow.availableMembersList.displayed

    and: 'an add button is shown instead'
      roleRow.addGroupButton.displayed

    and: 'only the groups button is marked as active'
      !roleRow.usersButton.hasClass('active')
      roleRow.groupsButton.hasClass('active')

    when: 'clicking the add button'
      roleRow.addGroupButton.click()

    then: 'the entered group is added to the applied list'
      roleRow.appliedMembers.size() == 1
      roleRow.appliedMemberNames*.text() == ['admin']
      roleRow.appliedMemberUsername*.text() == ['admin']
      roleRow.appliedMemberEmail*.text() == ['']
      roleRow.appliedMemberRealm*.text() == ['LDAP']

    when: 'saving the updated role membership mapping'
      roleRow.confirmButton.click()
      roleRow = tabs.securityTab.role('Developer') // account for DOM refresh

    then: 'the edit form is closed'
      !roleRow.editor.displayed

    and: 'the added member is shown in the list'
      roleRow.memberNames == ['admin']
  }
}
