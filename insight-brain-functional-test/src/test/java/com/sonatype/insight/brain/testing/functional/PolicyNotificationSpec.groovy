/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ActionModule
import com.sonatype.insight.brain.testing.functional.modules.NotificationsModule
import com.sonatype.insight.brain.testing.functional.modules.PolicyEditorModule
import spock.lang.Stepwise

@Stepwise
class PolicyNotificationSpec
    extends BaseSpec
{
  static PolicyEditorModule policyEditor;

  def setupSpec() {
    def organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrg("PolicyNotificationOrg")
    def organizationPage = (OrganizationPage)browser.page
    organizationPage.policies.newPolicyButton.click();
    policyEditor = organizationPage.policies.newPolicyEditor;
    waitFor { policyEditor.displayed }
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def 'Opens a modal to edit notifications'() {
    when: 'The first stage notifications envelope is clicked'
      ActionModule firstStage = policyEditor.actions[0]
      firstStage.notificationButton.click()

    then: 'Notifications dialog is shown'
      waitFor { notificationModal.displayed }
  }

  def 'Adds an email to the list of notified emails'() {
    when: 'An email is added'
      NotificationsModule notificationModal = notificationModal
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
}
