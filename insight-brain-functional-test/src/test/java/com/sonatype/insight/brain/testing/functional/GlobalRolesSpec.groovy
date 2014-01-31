/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import spock.lang.Stepwise

@Stepwise
class GlobalRolesSpec
    extends BaseSpec
{
  def setupSpec() {
    UserDAO userDAO = new UserDAO()
    User user = new User(username: "test-a", password: "secret", firstName: "John", lastName: "Doe", email: "john@doe.net")
    userDAO.insert(user);
    user = new User(username: "test-b", password: "secret", firstName: "Jane", lastName: "Doe", email: "jane@doe.net")
    userDAO.insert(user);

    loginAsAdminVia(GlobalRolesPage)
  }

  def cleanupSpec() {
    UserDAO userDAO = new UserDAO();
    userDAO.getAll().each { user ->
      if (user.username.startsWith("test")) {
        userDAO.delete(user);
      }
    }
  }

  def "Entering the page shows the global roles"() {
    expect: "the default roles along with builtin users"
      def roleRow = mapping.role("Administrator")
      roleRow.displayed
      roleRow.memberNames == ["Admin BuiltIn"]
      !roleRow.editor.displayed
      mapping.roles.size() == 1
  }

  def "Clicking the edit button opens the form"() {
    when: "hovering over a role"
      def roleRow = mapping.role("Administrator")
      interact {
        moveToElement(roleRow)
      }

    then: "the edit button is visible"
      roleRow.editButton.displayed

    when: "clicking the edit button"
      js.exec '$( ".content" ).scrollLeft( 300 );'  //content appears offscreen in default browser size
      roleRow.editButton.click()

    then: "the edit form opens"
      roleRow.editor.displayed
  }

  def "Typing in the search input filters the list of available users"() {
    when: "entering first name prefix"
      def roleRow = mapping.role("Administrator")
      roleRow.queryInput.value("Jan")

    then: "the matching users are listed"
      waitFor { roleRow.availableMembers.size() == 1 }
      roleRow.availableMemberNames*.text() == ["Jane Doe"]
      roleRow.availableMemberEmail*.text() == ["jane@doe.net"]
      roleRow.availableMemberRealm*.text() == ["CLM"]
      roleRow.appliedMembers.size() == 0

    when: "entering last name prefix"
      roleRow.queryInput.value("Do")

    then: "the matching users are listed"
      waitFor { roleRow.availableMembers.size() == 2 }
      roleRow.availableMemberNames*.text().sort() == ["Jane Doe", "John Doe"]
      roleRow.availableMemberEmail*.text().sort() == ["jane@doe.net", "john@doe.net"]
      roleRow.availableMemberRealm*.text() == ["CLM", "CLM"]
      roleRow.appliedMembers.size() == 0
  }

  def "Adding an available user moves him to the applied list"() {
    when: "clicking an available user"
      def roleRow = mapping.role("Administrator")
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
      def roleRow = mapping.role("Administrator")
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
      def roleRow = mapping.role("Administrator")
      roleRow.confirmButton.click()
      //make sure we grab latest dom, as the save will rebuild it
      roleRow = mapping.role("Administrator");

    then: "the edit form is closed and the added member shown in the list"
      !roleRow.editor.displayed
      roleRow.memberNames == ["Admin BuiltIn", "John Doe"]
  }
}
