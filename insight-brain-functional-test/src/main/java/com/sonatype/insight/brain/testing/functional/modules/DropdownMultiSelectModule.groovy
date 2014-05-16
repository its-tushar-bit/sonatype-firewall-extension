/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * Since 1.11
 */
class DropdownMultiSelectModule
    extends Module
{
  static content = {
    dropdown { $('.btn-group') }
    dropdownButton { dropdown.find('button') }
    dropdownList { dropdown.find('ul') }
    dropdownItem(required: false) { name -> dropdownList.find('label').has('.multi-dropdown-item.name', text: name) }
    dropdownCheck { name -> dropdownItem(name).find('input') }
    dropdownColor { name -> dropdownItem(name).find('span.multi-dropdown-item-color') }
    dropdownOwner { name -> dropdownItem(name).find('div.multi-dropdown-item.owner') }
  }

  void toggleOption(String name) {
    showDropdown()
    waitFor { dropdownCheck(name) }.click()
    hideDropdown()
  }

  boolean areOptionsApplied(List<String> names) {
    showDropdown()
    boolean isApplied = true;
    for (String name in names) {
      if (!dropdownCheck(name).value()) {
        isApplied = false
        break
      }
    }
    hideDropdown()
    return isApplied
  }

  /**
   * Assumes that the options dropdown is already open
   */
  boolean areOptionsColored(Map<String, String> namesToColors) {
    return namesToColors.every { String name, String color ->
      dropdownColor(name).classes().contains("${color}Label".toString())
    }
  }

  void showDropdown() {
    dropdownButton.click()
    waitFor { dropdownList.displayed }
  }

  void hideDropdown() {
    dropdownButton.click()
    waitFor { !dropdownList.displayed }
  }
}
