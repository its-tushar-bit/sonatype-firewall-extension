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
    editor { $('.accordion-body.in') }
    buttons { module ButtonsModule }
    tagsHeader { $('h5', text: 'Application Matching') }
    policyTag { $('.policy-tag') }
    policyTagError { policyTag.find('div')[-1] }
    radioButton { option -> $('input[type=radio]', 'ng-value': option) }
    allApplicationRadioButton { radioButton('false') }
    taggedApplicationRadioButton { radioButton('true') }
    tags { $('span', items: 'tags') }
    tagsDropdownButton { tags.find('button') }
    tagsDropdownList { tags.find('ul') }
    tagsDropdownCheck { name -> tagsDropdownList.find('a', text: name).find('input') }
    tagsDropdownColor { name -> tagsDropdownList.find('a', text: name).find('span.multi-dropdown-item-color') }
  }

  def toggleTag(String name) {
    showDropdown()
    this.tagsDropdownCheck(name).click()
    hideDropdown()
  }

  def areTagsApplied(List<String> names) {
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

  def areTagsColored(Map<String, String> namesToColors) {
    showDropdown()
    boolean hasColor = true
    namesToColors.each { String name, String color ->
      def contains = tagsDropdownColor(name).classes().contains("${color}Label".toString())
      hasColor = hasColor && contains
    }
    hideDropdown()
    return hasColor
  }

  def isSelected(radioButton) {
    return radioButton.firstElement().selected
  }

  def showDropdown() {
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
  }

  def hideDropdown() {
    tagsHeader.click()
    waitFor { !tagsDropdownList.displayed }
  }
}
