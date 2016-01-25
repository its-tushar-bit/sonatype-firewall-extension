/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AgeInDaysInput() {
    return {
      restrict: 'E',
      scope: {
        ageInDaysModel: '=ngModel',
        name: '@'
      },
      templateUrl: 'utility/widgets/age.in.days.input.directive.html',
      controller: 'age.in.days.input.controller',
      controllerAs: 'vm',
      bindToController: true,
      require: 'ngModel',
      link: function(scope, element, attr, ctrl) {
        scope.vm.ageInDaysModelCtrl = ctrl;

        ctrl.$formatters.push(scope.vm.formatDaysToAge);
        ctrl.$parsers.push(scope.vm.parseAgeToDays);
      }
    };
  }

  angular //
      .module('utility') //
      .directive('ageInDaysInput', AgeInDaysInput);

}(angular));
