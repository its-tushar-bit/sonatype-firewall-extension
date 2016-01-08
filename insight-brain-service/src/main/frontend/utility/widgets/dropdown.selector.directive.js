/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DropdownSelector() {
    return {
      restrict: 'E',
      scope: {
        selectedModel: '=ngModel',
        options: '=',
        optionNameParam: '@?', // One-time binding
        emptyOptionString: '@?',
        optionValueParam: '@?',
        disabled: '=?ngDisabled'
      },
      templateUrl: 'utility/widgets/dropdown.selector.directive.html',
      controller: 'dropdown.selector.controller',
      controllerAs: 'vm',
      bindToController: true,
      require: 'ngModel',
      link: function(scope, element, attr, ctrl) {
        scope.vm.getSelectedViewValue = getSelectedViewValue;
        scope.vm.formatSelectedModel = formatSelectedModel;
        scope.vm.selectItem = selectItem;

        ctrl.$viewChangeListeners.push(function() {
          scope.$eval(attr.ngChange);
        });

        ctrl.$formatters.push(scope.vm.formatSelectedModel);
        ctrl.$parsers.push(scope.vm.parseSelectedModel);

        function getSelectedViewValue() {
          return ctrl.$viewValue;
        }

        function formatSelectedModel(modelValue) {
          if (scope.vm.optionValueParam) {
            modelValue = scope.vm.optionModelMap[modelValue];
          }

          if (modelValue) {
            return scope.vm.optionNameParam ? modelValue[scope.vm.optionNameParam] : modelValue;
          }
          else {
            return scope.vm.emptyOptionString || '-- None --';
          }
        }

        function selectItem(item) {
          ctrl.$setViewValue(scope.vm.optionNameParam ? item[scope.vm.optionNameParam] : item);
        }
      }
    };
  }

  angular.module('utility').directive('dropdownSelector', DropdownSelector);

}(angular));
