/**
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
class TagApplicationSpec
    extends BaseSpec
{
  def setupSpec() {
    loginAsAdmin()
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    organizationManagementPage.createOrg()
    tabs.tagTabButton.click()
    tags.createNewTag()
    tags.buttons.save.click()
    waitFor { tags.tagList.size() == 1 }

    ApplicationManagementPage applicationManagementPage = to(ApplicationManagementPage)
    applicationManagementPage.createApp('ApplicationTagSpec')
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def 'Can navigate to the TAGS tab'() {
    when: 'Clicking on the tag tab'
      waitFor{ tabs.tagTabButton.displayed }
      tabs.tagTabButton.click()

    then: 'the url changes and we are presented with list of available tags'
      waitFor{ tags.availableTagList.displayed }
      tags.availableTagList.size() == 1
      tags.availableTagList[0].text() == 'New Tag'
      tags.appliedTagList.size() == 0
  }

  def 'Can apply a tag'() {
    when: 'Clicking on the available tag'
      tags.availableTagList[0].click()

    then: 'the available tag is moved to the applied tags'
      waitFor { tags.availableTagList.size() == 0 }
      tags.availableTagEmptyText.text() == 'No tags available'
      tags.appliedTagList.size() == 1
      tags.appliedTagList[0].text() == 'New Tag'
  }

  def 'Can detach a tag'() {
    when: 'Clicking on the applied tag'
      tags.appliedTagList[0].click()

    then: 'the applied tag is moved to the available tags'
      waitFor { tags.availableTagList.size() == 1 }
      tags.availableTagList[0].text() == 'New Tag'
      tags.appliedTagList.size() == 0
      tags.appliedTagEmptyText.text() == 'No tags applied'
  }

  def 'Can filter tag'() {
    when: 'A existing tag filter is applied'
      tags.tagFilterInput = 'Tag'

    then: 'the existing tag shows up'
      tags.availableTagList.size() == 1
      tags.availableTagList[0].text() == 'New Tag'
      tags.appliedTagList.size() == 0
      tags.appliedTagEmptyText.text() == 'No tags applied matching Tag'

    when: 'A non existing tag filter is applied'
      tags.tagFilterInput = 'Foo'

    then: 'no tags show up'
      tags.availableTagList.size() == 0
      tags.availableTagEmptyText.text() == 'No tags available matching Foo'

  }
}