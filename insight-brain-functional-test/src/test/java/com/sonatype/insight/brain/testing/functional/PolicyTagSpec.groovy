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
      policies.policyEditButton('Architecture-Quality').click()
      waitFor { policies.policyEditors.displayed }
      policies.showDropdown()

    then: 'the "All Applications" option is selected'
      policies.isSelected(policies.allApplicationRadioButton)
      !policies.isSelected(policies.taggedApplicationRadioButton)

    and: 'expected Tags are available to choose'
      [tag1.name, tag2.name].each{ name ->
        policies.tagsDropdownCheck(name).displayed
      }

    cleanup:
      policies.hideDropdown()
  }

  def 'We can add tags to the policy'() {
    when: 'Toggling the first tag'
      policies.toggleTag(tag1.name)

    then: 'the tag name shows up in the text of the button'
      policies.tagsDropdownButton.text() == tag1.name

    and: 'the "Applications with tags" option should now be selected'
      policies.isSelected(policies.taggedApplicationRadioButton)
      !policies.isSelected(policies.allApplicationRadioButton)

    when: 'adding a second tag'
      policies.toggleTag(tag2.name)

    then: 'both names are shown on the text of the button, and both are checked'
      def tagNames = [tag1.name, tag2.name]
      policies.tagsDropdownButton.text() == tagNames.join(', ')
      policies.areTagsApplied(tagNames)

    and: 'they are styled in the list with the appropriate color'
      policies.areTagsColored([(tag1.name): tag1.color, (tag2.name): tag2.color])
  }

  def 'We can save the Tag changes'(){
    when: 'We save'
      policies.policyEditorButtons(0).save.click()
      driver.navigate().refresh()
      policies.policyEditButton('Architecture-Quality').click()
      waitFor { policies.policyEditors.displayed }

    then: 'We can observe the persisted changes'
      def tagNames = [tag1.name, tag2.name]
      policies.tagsDropdownButton.text() == tagNames.join(', ')
      policies.areTagsApplied(tagNames)
  }

  def 'We can clear all Tags'(){
    when: 'We choose the "All Applications" option'
      policies.allApplicationRadioButton.click()

    then:
      policies.isSelected(policies.allApplicationRadioButton)
      !policies.isSelected(policies.taggedApplicationRadioButton)
      policies.tagsDropdownButton.text() == 'None selected'
  }

  def 'We cannot save if we do not select Tags and "All Applications" is not selected'(){
    when: 'We click on the "Applications with Tags" option'
      policies.taggedApplicationRadioButton.click()

    then: 'We are presented with an error message'
      policies.policyTag.classes().contains('error')
      policies.policyTagError.text() == 'Must select tags to associate with the policy.'

    cleanup:
      policies.policyEditorButtons(0).cancel.click()
  }
}
