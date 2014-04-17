/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

/**
 * @since 1.9
 */
@Stepwise
class LabelSpec
    extends BaseSpec
{
  def setupSpec() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrg('LabelSpec')
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can navigate to the labels tab"() {
    when: 'Clicking on the label tab'
      tabs.labelsTabButton.click()

    then: 'the url changes and we are presented with an empty list of labels'
      waitFor { labels.newLabelButton.displayed }
      driver.currentUrl.endsWith('labels')
      labels.labelList.size() == 0
  }

  def "Can add a new label"() {
    when: 'We create a new label'
      labels.createNewLabel()
      labels.buttons.save.click()

    then: 'The label editor is disposed'
      waitFor { !labels.labelEditor.displayed }

    and: 'The label is added to the list of available labels'
      waitFor { labels.labelList.size() == 1 }
      labels.label(0).text() == 'NewLabel'
      labels.label(0).classes().contains('blackLabel')
      labels.errorFree
  }

  def "Can edit an existing label"() {
    when: 'We click on an existing label'
      labels.label(0).click()

    then: 'The form is populated with the name and description of the chosen label'
      labels.name == 'NewLabel'
      labels.description == 'Label description'
      labels.color('black').classes().contains('active')

    when: 'We update the label name and description'
      labels.name = 'NewLabelUpdated'
      labels.description = 'Updated Label Description'
      labels.color('green').click()
      labels.buttons.save.click()

    then: 'The label editor is disposed'
      waitFor { !labels.labelEditor.displayed }

    and: 'The listed label is updated'
      waitFor { labels.label(0).text() == 'NewLabelUpdated' }
      labels.label(0).classes().contains('greenLabel')

    when:
      labels.label(0).click()

    then: 'The form is populated with the updated name, description and policy of the chosen label'
      labels.name == 'NewLabelUpdated'
      labels.description == 'Updated Label Description'
      labels.color('green').classes().contains('active')
      labels.buttons.cancel.click()
  }

  def 'Are warned when attempting to change labels while editing'() {
    given: 'We create another new label'
      labels.createNewLabel('AnotherNewLabel')
      labels.buttons.save.click()

    and: 'The label editor has been disposed'
      waitFor { !labels.labelEditor.displayed }

    when: 'We click on an existing label'
      labels.label(0).click()

    then: 'The form is populated with the name and description of the chosen label'
      labels.name == 'AnotherNewLabel'

    when: 'We make a change then attempt to edit another label'
      labels.name = 'NewLabelUpdatedAgain'
      labels.label(1).click()

    then: 'We are presented with a modal warning that we have existing edits'
      waitFor { isEditingModal.modal.displayed }
      isEditingModal.text() == 'This label may contain unsaved changes, continuing will discard them.'
      isEditingModal.cancel.click()
      labels.name == 'NewLabelUpdatedAgain'

    when: 'We once again attempt to edit another label'
      labels.label(1).click()

    then: 'We can discard changes and edit another tag'
      waitFor { isEditingModal.modal.displayed }
      isEditingModal.text() == 'This label may contain unsaved changes, continuing will discard them.'
      isEditingModal.continueButton.click()
      labels.name == 'NewLabelUpdated'
      labels.buttons.cancel.click()
  }

  def "We are prevented from saving if the form won't validate"() {
    when: 'We try to add another label with an invalid name'
      labels.createNewLabel('Another New Label')

    then: 'An invalid characters error message appears'
      labels.nameValidations.invalidCharacters.displayed
      labels.buttons.save.@disabled

    when: 'We adjust name to use some non-alphanumeric characters'
      labels.name << '$'

    then: 'The error message remains'
      labels.nameValidations.invalidCharacters.displayed
      labels.buttons.save.@disabled

    when: 'We correct the invalid data'
      labels.name = 'AnotherNewLabelAgain'

    then: 'The save button finally enables'
      !labels.buttons.save.@disabled
  }

  def "We cancel the form"() {
    when: 'We click the cancel button'
      labels.buttons.cancel.click()

    then: 'form inputs are no longer displayed'
      !labels.name.displayed
      !labels.description.displayed
  }

  def "Can delete the newly added label"() {
    when:
      labels.labelList.each { label ->
        labels.delete(label)
        waitFor { deleteModal.modal.displayed }
        deleteModal.confirm.click()
      }

    then:
      waitFor { labels.labelList.size() == 0 }
  }
}
