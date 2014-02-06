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
    extends Module {
  static content = {
    newPolicyButton { $('.new-entity-button', text: 'New Policy') }
    policyEditors(required: false) { moduleList PolicyEditorModule, $('#policyList div', 'ng-repeat': 'policy in policies') }
  }

  def findPolicyEditor(String policyName){
    return policyEditors.find{ it.header.text().endsWith(policyName) }
  }
}

/**
 * Since 1.9
 */
class PolicyEditorModule
    extends Module {
  static content = {
    header { $('.accordion-heading') }
    editButton { header.find('button', 'ng-click': 'edit(policy)') }
    chicklet { header.find('span.threat-chiclet')}
    body { $('.accordion-body') }
    tagsHeader(required: false) { body.find('h5', text: 'Application Matching') }
    buttons { module ButtonsModule, body }
    policyTag { body.find('.policy-tag') }
    policyTagError { policyTag.find('div')[-1] }
    allApplicationRadioButton { body.find('#radio-all-applications') }
    taggedApplicationRadioButton { body.find('#radio-tag-applications') }
    tags { body.find('span', items: 'tags') }
    tagsDropdownButton { tags.find('button') }
    tagsDropdownList { tags.find('ul') }
    tagsDropdownCheck { name -> tagsDropdownList.find('a', text: name).find('input') }
    tagsDropdownColor { name -> tagsDropdownList.find('a', text: name).find('span.multi-dropdown-item-color') }
  }

  void toggleTag(String name) {
    showDropdown()
    this.tagsDropdownCheck(name).click()
    hideDropdown()
  }

  boolean areTagsApplied(List<String> names) {
    showDropdown()
    boolean isApplied = true;
    for (String name in names) {
      if (!tagsDropdownCheck(name).value()) {
        isApplied = false
        break
      }
    }
    hideDropdown()
    return isApplied
  }

  boolean areTagsColored(Map<String, String> namesToColors) {
    showDropdown()
    boolean hasColor = true
    namesToColors.each { String name, String color ->
      def contains = tagsDropdownColor(name).classes().contains("${color}Label".toString())
      hasColor = hasColor && contains
    }
    hideDropdown()
    return hasColor
  }

  boolean isSelected(radioButton) {
    return radioButton.firstElement().selected
  }

  boolean showsTagIcon(){
    return chicklet.classes().contains('icon-tags')
  }

  boolean isExpanded(){
    return body.classes().contains('in')
  }

  void showDropdown() {
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
  }

  void hideDropdown() {
    tagsHeader.click()
    waitFor { !tagsDropdownList.displayed }
  }
}
