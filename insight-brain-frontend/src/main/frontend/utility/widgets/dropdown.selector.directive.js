/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*
* Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
import template from './dropdown.selector.directive.tpl.html';

export default function DropdownSelector() {
  return {
    restrict: 'E',
    scope: {
      selectedModel: '=ngModel',
      options: '=',
      optionNameParam: '@?', // One-time binding
      classNameParam: '@?',
      itemAsClass: '@?',
      emptyOptionString: '@?',
      undefinedOptionsString: '@?',
      noOptionsString: '@?',
      optionValueParam: '@?',
      disabled: '=?ngDisabled'
    },
    template: template,
    controller: DropdownSelectorController,
    controllerAs: 'vm',
    bindToController: true,
    require: ['ngModel', '^form'],
    link: DropdownSelectorLink
  };

  function DropdownSelectorLink(scope, element, attr, ctrls) {
    var ctrl = ctrls[0],
        form = ctrls[1];

    form.$addControl(ctrl);

    scope.vm.getSelectedViewValue = getSelectedViewValue;
    scope.vm.formatSelectedModel = formatSelectedModel;
    scope.vm.selectItem = selectItem;

    ctrl.$viewChangeListeners.push(function() {
      scope.$eval(attr.ngChange);
    });

    ctrl.$formatters.push(scope.vm.formatSelectedModel);
    ctrl.$parsers.push(scope.vm.parseSelectedModel);
    ctrl.$isEmpty = isEmpty;

    scope.$watch('vm.disabled', function(disabled) {
      element[disabled ? 'addClass' : 'removeClass']('disabled');
    });

    scope.$watch('vm.options', function(options) {
      ctrl.$setPristine();
      element[angular.isUndefined(options) || scope.vm.disabled ? 'addClass' : 'removeClass']('disabled');
    });

    if (scope.vm.noOptionsString) {
      scope.$watch('vm.options.length', function(hasOptions) {
        // no-options class should only show if no options are available from a defined options set
        element[angular.isUndefined(scope.vm.options) || hasOptions ? 'removeClass' : 'addClass']('no-options');
      });
    }

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
        return scope.vm.emptyOptionString || 'None Selected';
      }
    }

    function isEmpty(viewValue) {
      return !viewValue || viewValue === (scope.vm.emptyOptionString || 'None Selected');
    }

    function selectItem(item) {
      ctrl.$setViewValue(scope.vm.optionNameParam ? item[scope.vm.optionNameParam] : item);
    }
  }
}

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

    if (vm.options) {
      vm.options.forEach(function(option) {
        if (vm.optionValueParam) {
          vm.optionModelMap[option[vm.optionValueParam]] = option;
        }

        if (vm.optionNameParam) {
          vm.optionViewMap[option[vm.optionNameParam]] = option;
        }
      });
    }

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
