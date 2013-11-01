/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.security.UserDAO
import com.sonatype.insight.brain.model.security.User
import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class GlobalRolesSpec
    extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
      Resources.getResource('config-test.yml').getPath())

  def setupSpec() {
    UserDAO userDAO = new UserDAO()
    User user = new User(username: "test-a", password: "secret", firstName: "John", lastName: "Doe", email: "john@doe.net")
    userDAO.insert(user);
    user = new User(username: "test-b", password: "secret", firstName: "Jane", lastName: "Doe", email: "jane@doe.net")
    userDAO.insert(user);

    to LoginPage
    loginAsAdmin()
    to GlobalRolesPage
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
      roleRow.availableMemberNames*.@title == ["Jane Doe <jane@doe.net>"]
      roleRow.appliedMembers.size() == 0

    when: "entering last name prefix"
      roleRow.queryInput.value("Do")

    then: "the matching users are listed"
      waitFor { roleRow.availableMembers.size() == 2 }
      roleRow.availableMemberNames*.text().sort() == ["Jane Doe", "John Doe"]
      roleRow.availableMemberNames*.@title.sort() == ["Jane Doe <jane@doe.net>", "John Doe <john@doe.net>"]
      roleRow.appliedMembers.size() == 0
  }

  def "Adding an available user moves him to the applied list"() {
    when: "clicking an available user"
      def roleRow = mapping.role("Administrator")
      roleRow.availableMember("John Doe").click()

    then: "the user is moved from the available list to the applied list"
      roleRow.availableMembers.size() == 1
      roleRow.availableMemberNames*.text() == ["Jane Doe"]
      roleRow.appliedMembers.size() == 1
      roleRow.appliedMemberNames*.text() == ["John Doe"]

    when: "clicking another available user"
      roleRow.availableMember("Jane Doe").click()

    then: "the user is moved from the available list to the applied list as well"
      roleRow.availableMembers.size() == 0
      roleRow.appliedMembers.size() == 2
      roleRow.appliedMemberNames*.text().sort() == ["Jane Doe", "John Doe"]
  }

  def "Removing an applied user moves him to the available list"() {
    when: "clicking an applied user"
      def roleRow = mapping.role("Administrator")
      roleRow.appliedMember("Jane Doe").click()

    then: "the user is moved from the applied list to the available list"
      roleRow.availableMembers.size() == 1
      roleRow.availableMemberNames*.text() == ["Jane Doe"]
      roleRow.appliedMembers.size() == 1
      roleRow.appliedMemberNames*.text() == ["John Doe"]
  }

  def "Saving the changes updates the mapping"() {
    when: "clicking the save button"
      def roleRow = mapping.role("Administrator")
      roleRow.confirmButton.click()
      //make sure we grab latest dom, as the save will rebuild it
      roleRow = mapping.role("Administrator");

    then: "the edit form is closed and the added member shown in the list"
      !roleRow.editor.displayed
      roleRow.memberNames.sort() == ["Admin BuiltIn", "John Doe"]
  }
}
