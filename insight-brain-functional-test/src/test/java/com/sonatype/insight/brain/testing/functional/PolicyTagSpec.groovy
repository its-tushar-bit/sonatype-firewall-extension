/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule
import spock.lang.Stepwise

@Stepwise
class PolicyTagSpec
    extends BaseSpec
{
  static Map tag1
  static Map tag2

  def setupSpec() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrgWithDefaultPolicy('PolicyTagSpec', ImportPolicyModule.sampleOrgPolicyFile)
    Map samplePolicy = ImportPolicyModule.parsePolicyFile(ImportPolicyModule.sampleOrgPolicyFile)
    tag1 = samplePolicy.tags[0].asImmutable()
    tag2 = samplePolicy.tags[1].asImmutable()
    tabs.policiesTabButton.click()
    waitFor { policies.newPolicyButton.displayed }
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def 'Initially a policy applies to all applications'(){
    when: 'We edit a policy that has no tags'
      def editor = policies.findPolicyEditor('Architecture-Quality')
      editor.editButton.click()
      waitFor { editor.tagsHeader.displayed }
      editor.showDropdown()

    then: 'the "All Applications" option is selected'
      editor.isSelected(editor.allApplicationRadioButton)
      !editor.isSelected(editor.taggedApplicationRadioButton)

    and: 'expected Tags are available to choose'
      [tag1.name, tag2.name].each{ name ->
        editor.tagsDropdownCheck(name).displayed
      }

    cleanup:
      editor.hideDropdown()
  }

  def 'We can add tags to the policy'() {
    given :
      def editor = policies.findPolicyEditor('Architecture-Quality')

    when: 'Toggling the first tag'
      editor.toggleTag(tag1.name)

    then: 'the tag name shows up in the text of the button'
      editor.tagsDropdownButton.text() == tag1.name

    and: 'the "Applications with tags" option should now be selected'
      editor.isSelected(editor.taggedApplicationRadioButton)
      !editor.isSelected(editor.allApplicationRadioButton)

    when: 'adding a second tag'
      editor.toggleTag(tag2.name)

    then: 'both names are shown on the text of the button, and both are checked'
      def tagNames = [tag1.name, tag2.name]
      editor.tagsDropdownButton.text() == tagNames.join(', ')
      editor.areTagsApplied(tagNames)

    and: 'they are styled in the list with the appropriate color'
      editor.areTagsColored([(tag1.name): tag1.color, (tag2.name): tag2.color])
  }

  def 'We can save the Tag changes'(){
    given :
      def editor = policies.findPolicyEditor('Architecture-Quality')

    when: 'We save and refresh the page'
      editor.buttons.save.click()
      driver.navigate().refresh()
      editor = policies.findPolicyEditor('Architecture-Quality')
      editor.editButton.click()
      waitFor { editor.tagsHeader.displayed }

    then: 'We can observe the persisted changes'
      def tagNames = [tag1.name, tag2.name]
      editor.tagsDropdownButton.text() == tagNames.join(', ')
      editor.areTagsApplied(tagNames)
  }

  def 'We can clear all Tags'(){
    given :
      def editor = policies.findPolicyEditor('Architecture-Quality')

    when: 'We choose the "All Applications" option'
      editor.allApplicationRadioButton.click()

    then:
      editor.isSelected(editor.allApplicationRadioButton)
      !editor.isSelected(editor.taggedApplicationRadioButton)
      editor.tagsDropdownButton.text() == 'None selected'
  }

  def 'We cannot save if we do not select Tags and "All Applications" is not selected'(){
    given :
      def editor = policies.findPolicyEditor('Architecture-Quality')
    when: 'We click on the "Applications with Tags" option'
      editor.taggedApplicationRadioButton.click()

    then: 'We are presented with an error message'
      editor.policyTag.classes().contains('error')
      editor.policyTagError.text() == 'Must select tags to associate with the policy.'

    cleanup:
      editor.buttons.cancel.click()
  }
}
