/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DropdownSelectorController($scope, $element) {
    var vm = this;

    vm.parseSelectedModel = parseSelectedModel;
    vm.optionModelMap = undefined;
    vm.optionViewMap = undefined;

    if (vm.optionValueParam || vm.optionNameParam) {
      $scope.$watch('vm.options', buildOptionMaps, true);
      buildOptionMaps();
    }

    function buildOptionMaps() {
      vm.optionModelMap = {};
      vm.optionViewMap = {};

      vm.options.forEach(function(option) {
        if (vm.optionValueParam) {
          vm.optionModelMap[option[vm.optionValueParam]] = option;
        }

        if (vm.optionNameParam) {
          vm.optionViewMap[option[vm.optionNameParam]] = option;
        }
      });

      if (vm.optionValueParam && vm.formatSelectedModel) {
        // Re-run formatter with updated map
        var ctrl = $element.controller('ngModel');
        ctrl.$setViewValue(vm.formatSelectedModel(ctrl.$modelValue));
      }
    }

    function parseSelectedModel(viewValue) {
      var selected = vm.optionNameParam ? vm.optionViewMap[viewValue] : viewValue;
      return vm.optionValueParam ? selected[vm.optionValueParam] : selected;
    }
  }

  DropdownSelectorController.$inject = ['$scope', '$element'];

  angular.module('utility').controller('dropdown.selector.controller', DropdownSelectorController);

}(angular));
