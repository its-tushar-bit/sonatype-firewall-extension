/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ActionModule
import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule
import com.sonatype.insight.brain.testing.functional.modules.NotificationsModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyEditorModule

import spock.lang.Stepwise

@Stepwise
class PolicyNotificationSpec
extends BaseSpec {
  static PolicyEditorModule policyEditor;
  static OrganizationPage organizationPage;

  public static final String POLICY_NAME = 'Architecture-Quality'

  def setupSpec() {
    def organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrgWithDefaultPolicy('PolicyNotificationSpec', ImportPolicyModule.sampleOrgPolicyFile)
    organizationPage = (OrganizationPage)browser.page
    policyEditor = organizationPage.policies.findPolicyEditor(POLICY_NAME)
    waitFor { policyEditor.displayed }
    policyEditor.editButton.click()
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def 'Opens a modal to edit notifications'() {
    given: 'The action table has fully loaded'
    waitFor { !policyEditor.actions.empty }

    when: 'The first stage notifications envelope is clicked'
    ActionModule firstStage = policyEditor.actions[0]
    firstStage.notificationButton.click()

    then: 'Notifications dialog is shown'
    waitFor { notificationModal.displayed }
  }

  def 'Adds an email to the list of notified emails'() {
    when: 'An email is added'
    NotificationsModule notificationModal = notificationModal
    waitFor { notificationModal.emailInput.displayed }
    notificationModal.emailInput.value('test@sonatype.com')
    notificationModal.addEmailButton.click()

    then: 'Email is shown in the list of notified emails'
    waitFor { notificationModal.selectedEmails.size() == 1 }
    notificationModal.selectedEmails[0].text() == 'test@sonatype.com'
  }

  def 'It validates emails'() {
    when: 'A value that is not an email is entered'
    NotificationsModule notificationModal = notificationModal
    notificationModal.emailInput.value('foo')

    then: 'A popover should be displayed indicating an invalid email'
    popoverText(notificationModal.emailInput) == 'Use valid format: abc@xyz.com'

    when: 'A duplicate email is entered'
    notificationModal.emailInput.value('test@sonatype.com')

    then: 'A popover should be displayed indicating a duplicate email'
    popoverText(notificationModal.emailInput) == 'Enter a unique value'
  }

  def 'Adds a role to the list of roles'() {
    when: 'A role is added'
    NotificationsModule notificationModal = notificationModal
    waitFor { notificationModal.roleSelect.displayed }
    int roleOptionsSize = notificationModal.roleOptions.size()
    String firstRole = notificationModal.roleOptions[1].text()
    notificationModal.roleSelect.value('0')
    notificationModal.addRoleButton.click()

    then: 'Role is shown in the list of notified roles'
    waitFor { notificationModal.selectedRoles.size() == 1 }
    notificationModal.selectedRoles[0].text() == firstRole

    and: 'Expect added role to be removed from select list'
    notificationModal.roleOptions.size() == roleOptionsSize - 1
    for (int i = 0; i< notificationModal.roleOptions.size(); i++) {
      notificationModal.roleOptions[i].text() != firstRole
    }
  }

  def 'Disables role selector when there are no available roles'() {
    when: 'Last role is added'
    NotificationsModule notificationModal = notificationModal
    waitFor { notificationModal.roleSelect.displayed }
    while (notificationModal.roleOptions.size() > 1) {
      notificationModal.roleSelect.value('0');
      notificationModal.addRoleButton.click()
    }

    then: 'Role selector is disabled'
    notificationModal.roleSelect.enabled == false
    notificationModal.addRoleButton.enabled == false
  }

  def 'Updates notifications on policy'() {
    when: 'Notifications are updated'
    NotificationsModule modal = notificationModal
    waitFor { modal.saveButton.displayed }
    modal.saveButton.click()

    then: 'Policy Editor shows updated notification counts'
    policyEditor.actions[0].notificationCount == "5"

    when: 'Policy is saved and refreshed'
    policyEditor.buttons.save.click()
    waitFor { !policyEditor.buttons.save.displayed }
    driver.navigate().refresh()
    waitFor { organizationPage.policies.findPolicyEditor(POLICY_NAME).displayed }
    policyEditor = organizationPage.policies.findPolicyEditor(POLICY_NAME)
    waitFor { policyEditor.displayed }
    policyEditor.editButton.click()

    then: 'Policy editor still shows notification counts'
    waitFor { policyEditor.actions[0].notificationCount == '5' }

    when: 'Notifications editor is opened'
    ActionModule firstStage = policyEditor.actions[0]
    firstStage.notificationButton.click()
    modal = notificationModal
    waitFor { modal.displayed }

    then: 'Notifications show up in modal'
    modal.selectedEmails[0].text() == 'test@sonatype.com'
    modal.selectedRoles[0].text() == 'Application Evaluator'
    modal.selectedRoles[1].text() == 'Component Evaluator'
    modal.selectedRoles[2].text() == 'Developer'
    modal.selectedRoles[3].text() == 'Owner'
  }
}
