/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * Since 1.9
 */
class PolicyTagSpec
    extends BaseSpec
{
  def setupSpec() {
    OrganizationManagementPage organizationManagementPage = loginAsAdminVia(OrganizationManagementPage)
    organizationManagementPage.createOrgWithDefaultPolicy('PolicyTagSpec')
    tabs.tagTabButton.click()
    tags.createNewTag('Policy Tag')
    tags.buttons.save.click()

    tabs.policiesTabButton.click()
    waitFor { policies.newPolicyButton.displayed }
    policies.policyEditButton('Architecture-Quality').click()
    waitFor { policies.policyEditors.displayed }
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def 'Can create add a tag to the policy'() {
    when: 'Toggling the tag'
      policies.toggleTag('Policy Tag')

    then: 'the tag shows up in the text of the button'
      policies.tagsDropdownButton.text() == 'Policy Tag'
      policies.isTagApplied('Policy Tag')
  }
}
