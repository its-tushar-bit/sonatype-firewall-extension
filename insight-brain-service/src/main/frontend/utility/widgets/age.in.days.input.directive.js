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
      controller: AgeInDaysInputController,
      controllerAs: 'vm',
      bindToController: true,
      require: 'ngModel',
      link: AgeInDaysInputLink
    };

    function AgeInDaysInputLink(scope, element, attr, ctrl) {
      scope.vm.ageInDaysModelCtrl = ctrl;

      ctrl.$formatters.push(scope.vm.formatDaysToAge);
      ctrl.$parsers.push(scope.vm.parseAgeToDays);
    }
  }

  function AgeInDaysInputController($scope) {
    var vm = this;

    vm.formatDaysToAge = formatDaysToAge;
    vm.parseAgeToDays = parseAgeToDays;
    vm.modifier = getInitialModifier(vm.ageInDaysModel);
    vm.modifierTypes = [{name: 'Days', modifier: 1}, {name: 'Months', modifier: 30}, {name: 'Years', modifier: 365}];

    $scope.$watch('vm.modifier', function(newModifier, oldModifier) {
      if (vm.ageInDaysModel) {
        vm.ageInDaysModel = ((vm.ageInDaysModel / oldModifier) * newModifier).toString();
      }
    });

    function getInitialModifier(days) {
      return days ? days % 365 === 0 ? 365 : days % 30 === 0 ? 30 : 1 : 365;
    }

    function formatDaysToAge(days) {
      return days ? parseInt(days) / vm.modifier : days;
    }

    function parseAgeToDays(age) {
      return (parseInt(age) * vm.modifier).toString();
    }
  }

  AgeInDaysInputController.$inject = ['$scope'];

  angular //
      .module('utility') //
      .directive('ageInDaysInput', AgeInDaysInput);

}(angular));
