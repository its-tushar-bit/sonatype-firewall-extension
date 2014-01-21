/**
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
    loginAsAdmin()
    OrganizationManagementPage organizationManagementPage = to(OrganizationManagementPage)
    organizationManagementPage.createOrg('TagSpec')
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }

  def "Can navigate to the TAGS tab"() {
    when: 'Clicking on the tag tab'
    tabs.tagTabButton.click()

    then: 'the url changes and we are presented with an empty list of Tags'
    waitFor{tags.newTagButton.displayed}
    driver.currentUrl.endsWith('tags')
    tags.tagList.empty
  }

  def "Can add a new Tag"() {
    when: 'We create a new Tag'
    tags.createNewTag()
    tags.buttons.save.click()

    then: 'it is added to the list of available Tags'
    waitFor { tags.tagList.size() == 1 }
    tags.tagList[0].text() == 'New Tag'
  }

  def "Can edit an existing tag"(){
    when: 'We click on an existing Tag'
    tags.tagList[0].click()

    then: 'The form is populated with the name and description of the chosen Tag'
    tags.name == 'New Tag'
    tags.description == 'Tag description'

    when: 'We update the tag name and description'
    tags.name = 'Updated New Tag'
    tags.description = 'Updated Tag Description'
    tags.buttons.save.click()

    then:
    tags.tagList[0].text() == 'Updated New Tag'
  }

  def "Can delete the newly added Tag"() {
    when:
    tags.delete(tags.tagList[0])
    waitFor { tagModal.modal.displayed }
    report 'modal dialog shown'
    tagModal.confirm.click()

    then:
    tags.tagList.empty
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
}
