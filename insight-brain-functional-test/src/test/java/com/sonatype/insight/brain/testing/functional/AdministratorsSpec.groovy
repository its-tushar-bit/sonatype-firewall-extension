/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.RoleDAO
import com.sonatype.insight.brain.model.security.Role

import spock.lang.Stepwise

@Stepwise
class AdministratorsSpec
extends BaseSpec {
  private static Role systemAdminRole = new RoleDAO().getById(Role.SYSTEM_ADMIN_ROLE_ID)
  private static Role clmAdminRole = new RoleDAO().getById(Role.CLM_ADMIN_ROLE_ID)

  def setupSpec() {
    temporaryEntity.newUser("test-a", "secret", "John", "Doe", "john@doe.net")
    temporaryEntity.newUser("test-b", "secret", "Jane", "Doe", "jane@doe.net")

    loginAsAdminVia(AdministratorsPage)
  }

  def "Help context is displayed for user search field"() {
    when: "hovering over query help icon"
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.editButton.click()
    waitFor { roleRow.queryHelp.displayed }
    interact { moveToElement(roleRow.queryHelp) }


    then: "query help popover is displayed"
    waitFor { roleRow.queryHelpPopover.displayed }
  }

  def "Entering the page shows the administrators"() {
    when: "navigating to the administrators page"
    to AdministratorsPage

    then: "the default roles along with builtin users"
    waitFor { mapping.roles.size() == 2 }
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.displayed
    roleRow.memberNames == ["Admin BuiltIn"]
    !roleRow.editor.displayed
    def clmRoleRow = mapping.role(clmAdminRole.getName())
    clmRoleRow.displayed
    !clmRoleRow.editor.displayed
  }

  def "Clicking the edit button opens the form"() {
    when: "hovering over a role"
    def roleRow = mapping.role(systemAdminRole.getName())
    interact { moveToElement(roleRow) }

    then: "the edit button is visible"
    roleRow.editButton.displayed

    when: "clicking the edit button"
    js.exec '$( ".content" ).scrollLeft( 300 );' //content appears offscreen in default browser size
    roleRow.editButton.click()

    then: "the edit form opens"
    roleRow.editor.displayed

    and: 'the users & groups buttons are not visible'
    !roleRow.usersButton.displayed
    !roleRow.groupsButton.displayed
  }

  def "Search input filters the list of available users"() {
    when: "entering first name prefix"
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.queryInput.value("Jan*")

    and: "clicking the search button"
    roleRow.searchButton.click()

    then: "the matching users are listed"
    waitFor { roleRow.availableMembers.size() == 1 }
    roleRow.availableMemberNames*.text() == ["Jane Doe"]
    roleRow.availableMemberEmail*.text() == ["jane@doe.net"]
    roleRow.availableMemberRealm*.text() == ["CLM"]
    roleRow.appliedMembers.size() == 0

    when: "entering last name prefix"
    roleRow.queryInput.value("*Do*")

    and: "clicking on the search button"
    roleRow.searchButton.click()

    then: "the matching users are listed"
    waitFor { roleRow.availableMembers.size() == 2 }
    roleRow.availableMemberNames*.text().sort() == ["Jane Doe", "John Doe"]
    roleRow.availableMemberEmail*.text().sort() == ["jane@doe.net", "john@doe.net"]
    roleRow.availableMemberRealm*.text() == ["CLM", "CLM"]
    roleRow.appliedMembers.size() == 0
  }

  def "Adding an available user moves him to the applied list"() {
    when: "clicking an available user"
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.availableMember("John Doe").click()

    then: "the user is moved from the available list to the applied list"
    roleRow.availableMembers.size() == 1
    roleRow.availableMemberNames*.text() == ["Jane Doe"]
    roleRow.availableMemberEmail*.text() == ["jane@doe.net"]
    roleRow.appliedMembers.size() == 1
    roleRow.appliedMemberNames*.text() == ["John Doe"]
    roleRow.appliedMemberEmail*.text() == ["john@doe.net"]
    roleRow.appliedMemberRealm*.text() == ["CLM"]

    when: "clicking another available user"
    roleRow.availableMember("Jane Doe").click()

    then: "the user is moved from the available list to the applied list as well"
    roleRow.availableMembers.size() == 0
    roleRow.appliedMembers.size() == 2
    roleRow.appliedMemberNames*.text().sort() == ["Jane Doe", "John Doe"]
    roleRow.appliedMemberEmail*.text().sort() == ["jane@doe.net", "john@doe.net"]
    roleRow.appliedMemberRealm*.text() == ["CLM", "CLM"]
  }

  def "Removing an applied user moves him to the available list"() {
    when: "clicking an applied user"
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.appliedMember("Jane Doe").click()

    then: "the user is moved from the applied list to the available list"
    roleRow.availableMembers.size() == 1
    roleRow.availableMemberNames*.text() == ["Jane Doe"]
    roleRow.availableMemberNames*.text() == ["Jane Doe"]
    roleRow.availableMemberEmail*.text() == ["jane@doe.net"]
    roleRow.appliedMembers.size() == 1
    roleRow.appliedMemberNames*.text() == ["John Doe"]
    roleRow.appliedMemberEmail*.text() == ["john@doe.net"]
    roleRow.appliedMemberRealm*.text() == ["CLM"]
  }

  def "Saving the changes updates the mapping"() {
    when: "clicking the save button"
    def roleRow = mapping.role(systemAdminRole.getName())
    roleRow.confirmButton.click()
    //make sure we grab latest dom, as the save will rebuild it
    roleRow = mapping.role(systemAdminRole.getName());

    then: "the edit form is closed and the added member shown in the list"
    !roleRow.editor.displayed
    roleRow.memberNames == ["Admin BuiltIn", "John Doe"]
  }
}
