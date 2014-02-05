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
    newPolicyButton { $('.new-entity-button', text: 'New Policy') }

    //TODO KR - separate this out to a composed module, since technically it's all in a List
    policiesAccordion(requied:false) { $('#policyList .accordion.policies') }
    policyHeader(required:false) { name -> policiesAccordion.find('.accordion-heading', text: '  ' + name) }
    policyEditButton(required:false) { name -> policyHeader(name).find('button', 'ng-click': 'edit(policy)') }
    policyEditors(required:false) { policiesAccordion.find('.accordion-body.in') }  //assumes a single one is open at a time
    policyEditorButtons(required:false){ module ButtonsModule, policyEditors}
    tagsHeader(required:false) { policyEditors.find('h5', text: 'Application Matching') }
    policyTag(required:false) { policyEditors.find('.policy-tag') }
    policyTagError(required:false) { policyTag.find('div')[-1] }
    tagRadioButtons(required: false) { policyEditors.find('.policy-tag input[type=radio]') }
    radioButton {option -> policyEditors.find('.policy-tag input[type=radio]', 'ng-value': option) }
    allApplicationRadioButton {radioButton('false')}
    taggedApplicationRadioButton {radioButton('true')}
    tags(required:false) { policyEditors.find('span', items: 'tags') }
    tagsDropdownButton(required:false) { tags.find('button') }
    tagsDropdownList(required:false) { tags.find('ul') }
    tagsDropdownCheck(required:false) { name -> tagsDropdownList.find('a', text: name).find('input') }
    tagsDropdownColor(required:false) { name -> tagsDropdownList.find('a', text: name).find('span.multi-dropdown-item-color') }
  }

  def toggleTag(String name) {
    showDropdown()
    this.tagsDropdownCheck(name).click()
    hideDropdown()
  }

  def areTagsApplied(List<String> names){
    showDropdown()
    boolean isApplied = true;
    for(String name in names){
      if(! tagsDropdownCheck(name).value()){
        isApplied = false
        break
      }
    }
    hideDropdown()
    return isApplied
  }

  def areTagsColored(Map<String, String> namesToColors){
    showDropdown()
    boolean hasColor = true
    namesToColors.each{String name, String color ->
      def contains = tagsDropdownColor(name).classes().contains("${color}Label".toString())
      hasColor = hasColor && contains
    }
    hideDropdown()
    return hasColor
  }

  def isSelected(radioButton){
    return radioButton.firstElement().selected
  }

  def showDropdown(){
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
  }

  def hideDropdown(){
    tagsHeader.click()
    waitFor { !tagsDropdownList.displayed }
  }
}
