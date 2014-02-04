/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * Since 1.9
 */
class PolicyModule
    extends Module
{
  static content = {
    newPolicyButton(required:false) { $('.new-entity-button', text: 'New Policy') }
    policiesAccordion(requied:false) { $('.accordion.policies') }
    policyHeader(required:false) { name -> policiesAccordion.find('.accordion-heading', text: '  ' + name) }
    policyEditButton(required:false) { name -> policyHeader(name).find('button', 'ng-click': 'edit(policy)') }
    policyEditors(required:false) { policiesAccordion.find('.accordion-body.in') }

    tagsLabel(required:false) { policyEditors.find('h5', text: 'Application Matching') }
    tags(required:false) { policyEditors.find('span', items: 'tags') }
    tagsDropdownButton(required:false) { tags.find('button') }
    tagsDropdownList(required:false) { tags.find('ul') }
    tagsDropdownAnchor(required:false) { name -> tagsDropdownList.find('a', text: name) }
  }

  def toggleTag(String name) {
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
    this.tagsDropdownAnchor(name).mouseover()
    this.tagsDropdownAnchor(name).click()
    tagsLabel.click()
    waitFor { !tagsDropdownList.displayed }
  }

  def isTagApplied(String name) {
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
    def isApplied = tagsDropdownList.find('li', text: name).classes().contains('active')
    tagsLabel.click()
    waitFor { !tagsDropdownList.displayed }
    return isApplied
  }
}
