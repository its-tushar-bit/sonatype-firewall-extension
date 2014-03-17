/*
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
    newPolicyEditor {module PolicyEditorModule, $('.inline-policy-editor')}
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
    //header elements are only present for existing Policies
    header(required:false) { $('.accordion-heading') }
    editButton(required:false) { header.find('button', 'ng-click': 'edit(policy)') }
    chicklet(required:false) { header.find('span.threat-chiclet')}

    //Policy specifics
    name{ $('input#policyName') }
    constraints { moduleList ConstraintModule, $('.accordion-group', 'ng-repeat': 'constraint in policy.constraints')}

    //Tags related to Policies
    tagsHeader(required: false) { $('h5', text: 'Application Matching') }
    buttons { module ButtonsModule }
    policyTag { $('.policy-tag') }
    policyTagError { policyTag.find('div')[-1] }
    allApplicationRadioButton { $('[id^="radio-all-applications"]') }
    taggedApplicationRadioButton { $('[id^="radio-tag-applications"]') }
    tags { $('span', items: 'tags') }
    tagsDropdownButton { tags.find('button') }
    tagsDropdownList { tags.find('ul') }
    tagsDropdownCheck { name -> tagsDropdownList.find('label', text: name).find('input') }
    tagsDropdownColor { name -> tagsDropdownList.find('label', text: name).find('span.multi-dropdown-item-color') }
  }

  void toggleTag(String name) {
    showTagDropdown()
    this.tagsDropdownCheck(name).click()
    hideTagDropdown()
  }

  boolean areTagsApplied(List<String> names) {
    showTagDropdown()
    boolean isApplied = true;
    for (String name in names) {
      if (!tagsDropdownCheck(name).value()) {
        isApplied = false
        break
      }
    }
    hideTagDropdown()
    return isApplied
  }

  /**
   * Assumes that the tag dropdown is already open
   */
  boolean areTagsColored(Map<String, String> namesToColors) {
    return namesToColors.every { String name, String color ->
      tagsDropdownColor(name).classes().contains("${color}Label".toString())
    }
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

  void showTagDropdown() {
    tagsDropdownButton.click()
    waitFor { tagsDropdownList.displayed }
  }

  void hideTagDropdown() {
    tagsHeader.click()
    waitFor { !tagsDropdownList.displayed }
  }
}

class ConstraintModule extends Module{
  static content = {
    editButton {$('button', title: 'Edit Constraint')}
    constraintName {$('input', 'ng-model': 'constraint.name')}
    conditions { moduleList ConditionModule, $('div', 'ng-repeat': 'condition in constraint.conditions')}
  }
}

class ConditionModule extends Module{
  static content = {
    conditionTypes {$('select', 'ng-model': 'condition.conditionTypeId')}
    operators {$('select', 'ng-model': 'condition.operator')}
    value {$('span', 'ng-model': 'condition.value').find('input')}
  }
}
