/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.dataaccess.ApplicationDAO
import com.sonatype.insight.brain.model.Organization
import com.sonatype.insight.brain.model.security.Permission
import spock.lang.Stepwise

/**
 * @since 1.8
 */
@Stepwise
class ApplicationSpec
    extends BaseSpec
{
  private static Organization org

  def setupSpec() {
    createUser()
    org = temporaryEntity.newOrganization('test organization')

    grantPermissions(getUsername(), org.getId(), Permission.WRITE, Permission.READ)
    loginAsUserVia(ApplicationManagementPage)
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can create a new Application"() {
    when: 'We go to the new application page'
      ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
      newApplicationButton.click()
      ApplicationPage applicationPage = at(ApplicationPage)

    then: 'Controls not shown'
      !applicationPage.tools.deleteButton.present
      !applicationPage.tools.appEvalButton.present

    when: 'Application details completed'
      applicationId.click()
      waitFor { applicationIdField.displayed }
      applicationIdField = 'New-Application'
      applicationOrgField.click()
      waitFor { applicationOrgName('test organization').displayed }
      applicationOrgName('test organization').click()
      applicationPage.applicationName.click()
      waitFor { applicationPage.applicationNameField.displayed }
      applicationPage.applicationNameField = 'New Application'

    then: 'Controls still not shown'
      !applicationPage.tools.deleteButton.present
      !applicationPage.tools.appEvalButton.present

    when: 'We click the save button'
      applicationSaveButton.click()

    then: 'we are left at the Application page'
      at ApplicationPage
      waitFor { applicationName.text() == 'New Application' }
      applicationIdSaved.text() == 'New-Application'

    and: 'the policy tab is shown by default'
      waitFor { policies.displayed }

    and: 'the newly created App appears in the list of Applications'
      waitFor { applicationList.size() == 1 }
      application('New Application').displayed
  }

  def "Policy evaluation summary lists stages in chronological order"() {
    given: 'at least one policy evaluation for the app'
      temporaryEntity.
          newPolicyEvaluation(new ApplicationDAO().getByPublicIdNotNull('New-Application').id, 'build', 'scan-id')

    when: 'refreshing the page to reload the policy evaluation summary'
      driver.navigate().refresh()
      at ApplicationPage

    then: 'the stages are listed in proper order'
      waitFor { policyEvalStages*.text() == ['Build', 'Stage Release', 'Release'] }
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
      at ApplicationManagementPage
      waitFor { applicationList.empty }
  }

  def "When adding new Applications, they are listed alphabetically"() {
    when: 'we add multiple Applications'
      createApp('Z')
      waitFor { applicationList.size() == 1 }
      createApp('A', 'a')

    then: 'they are listed alphabetically'
      waitFor { applicationList.size() == 2 }
      applicationList.collect { it.text() } == ['A', 'Z']
  }

  def "Can add a new Policy"() {
    given: 'The policy tab has loaded'
      waitFor { policies.displayed }

    when: "We click the new policy button"
      policies.newPolicyButton.click()

    then: "the policy editor is shown"
      def policyEditor = policies.newPolicyEditor
      waitFor { policyEditor.displayed }

    and: "the editor lists stages in chronological order"
      waitFor { policyEditor.actions*.stageName == ['Develop', 'Build', 'Stage Release', 'Release', 'Operate'] }

    when: "we complete the form and save the policy"
      policyEditor.name = 'NewPolicy'

      def constraint = policyEditor.constraints[0]
      constraint.editButton.click()
      constraint.constraintName = 'Constraint'
      waitFor { constraint.conditions[0].value.displayed }
      constraint.conditions[0].value = '1'
      policyEditor.buttons.save.click()

    then: "it shows up in the list of policies"
      waitFor { policies.findPolicyEditor('NewPolicy').displayed }
  }
}
