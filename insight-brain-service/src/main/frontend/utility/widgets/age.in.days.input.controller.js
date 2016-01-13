/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

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
      .controller('age.in.days.input.controller', AgeInDaysInputController);

}(angular));
