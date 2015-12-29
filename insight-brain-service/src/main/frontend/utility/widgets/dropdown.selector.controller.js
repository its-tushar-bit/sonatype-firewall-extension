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

    function currentlySelectedText() {
      if (vm.selectedModel) {
        return vm.optionNameParam ? vm.selectedModel[vm.optionNameParam] : vm.selectedModel;
      }
      else {
        return vm.emptyOptionString ? vm.emptyOptionString : '-- None --';
      }
    }
  }

  angular.module('utility').controller('dropdown.selector.controller', DropdownSelectorController);

}(angular));
