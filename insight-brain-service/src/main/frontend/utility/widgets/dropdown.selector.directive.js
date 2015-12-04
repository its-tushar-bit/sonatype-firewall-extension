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
        optionNameParam: '@',
        emptyOptionString: '@?',
        disabled: '=?ngDisabled'
      },
      templateUrl: 'utility/widgets/dropdown.selector.directive.html',
      controller: 'dropdown.selector.controller',
      controllerAs: 'vm',
      bindToController: true
    };
  }

  angular.module('utility').directive('dropdownSelector', DropdownSelector);

}(angular));
