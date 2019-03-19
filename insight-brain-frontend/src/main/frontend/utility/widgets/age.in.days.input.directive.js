/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './age.in.days.input.directive.html';

export default function AgeInDaysInput() {
  return {
    restrict: 'E',
    scope: {
      ageInDaysModel: '=ngModel',
      name: '@'
    },
    template,
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
  vm.modifierTypes = [
    {name: 'Days', modifier: 1}, {name: 'Weeks', modifier: 7}, {name: 'Months', modifier: 30},
    {name: 'Years', modifier: 365}
  ];

  $scope.$watch('vm.modifier', function(newModifier, oldModifier) {
    if (vm.ageInDaysModel) {
      vm.ageInDaysModel = ((vm.ageInDaysModel / oldModifier) * newModifier).toString();
    }
  });

  function getInitialModifier(days) {
    return days ? days % 365 === 0 ? 365 : days % 30 === 0 ? 30 : days % 7 === 0 ? 7 : 1 : 365;
  }

  function formatDaysToAge(days) {
    return days ? parseInt(days) / vm.modifier : days;
  }

  function parseAgeToDays(age) {
    return (parseInt(age) * vm.modifier).toString();
  }
}

AgeInDaysInputController.$inject = ['$scope'];
