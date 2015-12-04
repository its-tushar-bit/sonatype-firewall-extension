/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DropdownSelectorController() {
    var vm = this;

    vm.currentlySelectedText = currentlySelectedText;
    vm.selectItem = selectItem;

    function currentlySelectedText() {
      if (vm.selectedModel) {
        return vm.selectedModel[vm.optionNameParam];
      }
      else {
        return vm.emptyOptionString ? vm.emptyOptionString : '-- None --';
      }
    }

    function selectItem(item) {
      vm.selectedModel = item;
    }
  }

  angular.module('utility').controller('dropdown.selector.controller', DropdownSelectorController);

}(angular));
