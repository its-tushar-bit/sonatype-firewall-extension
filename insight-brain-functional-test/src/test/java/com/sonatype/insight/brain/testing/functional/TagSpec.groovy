/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule
import spock.lang.Stepwise

/**
 * @since 1.8
 */
@Stepwise
class TagSpec
    extends BaseSpec
{
  static Map samplePolicy

  def setupSpec() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrgWithDefaultPolicy('TagSpec', ImportPolicyModule.sampleOrgPolicyFile)
    samplePolicy = ImportPolicyModule.parsePolicyFile(ImportPolicyModule.sampleOrgPolicyFile)
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
      tags.tagList.size() == samplePolicy.tags.size()
  }

  def "Can add a new Tag"() {
    when: 'We create a new Tag'
      tags.createNewTag()
      tags.buttons.save.click()

    then: 'The tag editor is disposed'
      waitFor { !tags.tagEditor.displayed }

    and: 'The tag is added to the list of available tags'
      waitFor { tags.tagList.size() == samplePolicy.tags.size() + 1 }
      tags.tag(0).text() == 'New Tag'
      tags.tag(0).classes().contains('blackLabel')
      tags.serverAlerts.children().size() == 0
  }

  def "Can edit an existing tag"(){
    when: 'We click on an existing Tag'
      tags.tag(0).click()

    then: 'The form is populated with the name and description of the chosen Tag'
      tags.name == 'New Tag'
      tags.description == 'Tag description'
      tags.color('black').classes().contains('active')

    when: 'We update the tag name and description'
      tags.name = 'New Tag Updated'
      tags.description = 'Updated Tag Description'
      tags.color('green').click()
      tags.buttons.save.click()

    then: 'The tag editor is disposed'
      waitFor { !tags.tagEditor.displayed }

    and: 'The listed tag is updated'
      waitFor { tags.tag(0).text() == 'New Tag Updated' }
      tags.tag(0).classes().contains('greenLabel')

    when:
      tags.tag(0).click()

    then: 'The form is populated with the updated name, description and policy of the chosen Tag'
      tags.name == 'New Tag Updated'
      tags.description == 'Updated Tag Description'
      tags.color('green').classes().contains('active')
      tags.buttons.cancel.click()
  }

  def "Can delete the newly added Tag"() {
    when:
      tags.delete(tags.tagList[0])
      waitFor { tagModal.modal.displayed }
      report 'modal dialog shown'
      tagModal.confirm.click()

    then:
      waitFor { tags.tagList.size() == samplePolicy.tags.size() }
  }

  def "We are prevented from saving if the form won't validate"(){
    when: 'We try to add a second tag with an existing name'
      tags.createNewTag()
      tags.buttons.save.click()
      waitFor { tags.tagList.size() == samplePolicy.tags.size() + 1 }
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
      waitFor { tags.tagList.size() == samplePolicy.tags.size() + 2 }
      tags.tagList[0].text() == (('A' * 22) + '...')
  }

  def "Already applied tags warn they are used on deletion"(){
    given: 'An Application to apply tags to'
      def applicationManagementPage = to(ApplicationManagementPage)
      applicationManagementPage
      applicationManagementPage.createApp('TagSpec', 'TagSpec', 'TagSpec')

    when: 'Viewing the inherited organization policies before tag application'
      waitFor{ tabs.tabLinks.displayed }

    then: 'The non effective policy is not shown'
      !policies.findPolicyEditor('Security-High')

    when: 'Applying a Tag to an Application'
      tabs.tagTabButton.click()
      waitFor { tags.availableTagList.size() > 0 }
      tags.availableTag('Policy Tag 1').click()

    then: 'It appears in the list of applied tags'
      waitFor { tags.appliedTagList.size() == 1 }

    when: 'Viewing the inherited organization policies after tag application'
      tabs.policiesTabButton.click()
      waitFor { policies.findPolicyEditor('Security-High').displayed }

    then: 'The effective inherited policy is marked to show it will be  applied only if we have one of the corresponding tags'
      policies.findPolicyEditor('Security-High').showsTagIcon()

    when: 'We view the Tags in the Organization view'
      to OrganizationManagementPage
      organization('TagSpec').click()
      tabs.tagTabButton.click()

    then: 'The newly applied Tag is visually shown to be applied'
      waitFor { tags.tagList.size() > 0 }
      report 'applied count is shown'
      def marker = tags.appliedMarker(tags.tagList[2])
      marker.displayed
      marker.text() == '1'

    when: 'We try to delete the tag'
      tags.delete(tags.tagList[2])

    then: 'We are warned that it is in use'
      waitFor { tagModal.modal.displayed }
      tagModal.text.contains('It is in use by the following applications: TagSpec.')

    when:
      tagModal.confirm.click()

    then:
      true
  }
}
