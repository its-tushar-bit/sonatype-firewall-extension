/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

/**
 * @since 1.8
 */
@Stepwise
class TagSpec
    extends BaseSpec
{
  def setupSpec() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrgWithDefaultPolicy('TagSpec')
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can navigate to the TAGS tab"() {
    when: 'Clicking on the tag tab'
      tabs.tagTabButton.click()

    then: 'the url changes and we are presented with an empty list of Tags'
      waitFor { tags.newTagButton.displayed }
      driver.currentUrl.endsWith('tags')
      tags.tagList.empty
  }

  def "Can add a new Tag"() {
    when: 'We create a new Tag'
      tags.createNewTag()
      tags.togglePolicy('Security-High')
      tags.buttons.save.click()

    then: 'The tag editor is disposed'
      waitFor { !tags.tagEditor.displayed }

    and: 'The tag is added to the list of available tags'
      waitFor { tags.tagList.size() == 1 }
      tags.tag(0).text() == 'New Tag'
      tags.tag(0).classes().contains('blackLabel')
      tags.serverAlerts.children().size() == 0
  }

  def "Can edit an existing tag"(){
    when: 'We click on an existing Tag'
      tags.tag(0).click()

    then: 'The form is populated with the name, description and policy of the chosen Tag'
      tags.name == 'New Tag'
      tags.description == 'Tag description'
      tags.color('black').classes().contains('active')
      waitFor { tags.isPolicyApplied('Security-High') }

    when: 'We update the tag name and description'
      tags.name = 'Updated New Tag'
      tags.description = 'Updated Tag Description'
      tags.color('green').click()
      tags.togglePolicy('Security-High')
      tags.buttons.save.click()

    then: 'The tag editor is disposed'
      waitFor { !tags.tagEditor.displayed }

    and: 'The listed tag is updated'
      waitFor { tags.tag(0).text() == 'Updated New Tag' }
      tags.tag(0).classes().contains('greenLabel')

    when:
      tags.tag(0).click()

    then: 'The form is populated with the updated name, description and policy of the chosen Tag'
      tags.name == 'Updated New Tag'
      tags.description == 'Updated Tag Description'
      tags.color('green').classes().contains('active')
      !tags.isPolicyApplied('Security-High')
      tags.buttons.cancel.click()
  }

  def "Can delete the newly added Tag"() {
    when:
      tags.delete(tags.tagList[0])
      waitFor { tagModal.modal.displayed }
      report 'modal dialog shown'
      tagModal.confirm.click()

    then:
      waitFor { tags.tagList.empty }
  }

  def "We are prevented from saving if the form won't validate"(){
    when: 'We try to add a second tag with an existing name'
      tags.createNewTag()
      tags.buttons.save.click()
      waitFor { tags.tagList.size() == 1 }
      tags.createNewTag()

    then: 'We are presented with an error message'
      tags.nameValidations.divStartsWith('Duplicate Tag name').displayed
      report 'duplicate tag error'

    when: 'We append some non-alphanumeric characters'
      tags.name << '$'

    then: 'The error message changes to reflect this'
      !tags.nameValidations.divStartsWith('Duplicate Tag name').displayed
      tags.nameValidations.alphaNumeric.displayed
      tags.buttons.save.@disabled
      report 'alphanumeric validation error'

    when: 'We correct the invalid data'
      tags.name = 'Another new Tag'

    then: 'The save button finally enables'
      !tags.buttons.save.@disabled
  }

  def "We cancel the form"(){
    when: 'We click the cancel button'
      tags.buttons.cancel.click()

    then: 'form inputs are no longer displayed'
      !tags.name.displayed
      !tags.description.displayed
  }

  def "Long names are truncated"() {
    when: 'We use a name that does not fit the UI element'
      tags.createNewTag('A' * 30)
      tags.buttons.save.click()

    then: 'the value displayed will be truncated'
      waitFor { tags.tagList.size() == 2 }
      tags.tagList[0].text() == (('A' * 22) + '...')
  }

  def "Already applied tags warn they are used on deletion"(){
    given: 'An Application to appy tags to'
      def applicationManagementPage = to(ApplicationManagementPage)
      applicationManagementPage
      applicationManagementPage.createApp('TagSpec', 'TagSpec', 'TagSpec')
      waitFor{ tabs.tabLinks.displayed }
      tabs.tagTabButton.click()

    when: 'Applying a Tag to an Application'
      waitFor { tags.availableTagList.size() > 0 }
      tags.availableTagList[0].click()

    then: 'It appears in the list of applied tags'
      waitFor { tags.appliedTagList.size() == 1 }

    when: 'We view the Tags in the Organization view'
      to OrganizationManagementPage
      organization('TagSpec').click()
      tabs.tagTabButton.click()

    then: 'The newly applied Tag is visually shown to be applied'
      waitFor { tags.tagList.size() > 0 }
      report 'applied count is shown'
      def marker = tags.appliedMarker(tags.tagList[0])
      marker.displayed
      marker.text() == '1'

    when: 'We try to delete the tag'
      tags.delete(tags.tagList[0])

    then: 'We are warned that it is in use'
      waitFor { tagModal.modal.displayed }
      tagModal.text.contains('It is in use by the following applications: TagSpec.')
  }
}
