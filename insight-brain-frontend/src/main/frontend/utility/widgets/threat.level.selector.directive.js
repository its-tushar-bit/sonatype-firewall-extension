/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './threat.level.selector.directive.html';

export default function ThreatLevelSelector() {
  return {
    restrict: 'E',
    scope: {
      threatLevelModel: '=ngModel',
      onChange: '=?',
      threatType: '@',
      disabled: '=?ngDisabled',
    },
    template,
    controller: ['$timeout', ThreatLevelSelectorController],
    controllerAs: 'vm',
    bindToController: true,
  };
}

function ThreatLevelSelectorController($timeout) {
  var vm = this;

  vm.threatLevels = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0];
  vm.selectLevel = selectLevel;

  function selectLevel(threatLevel) {
    vm.threatLevelModel = parseInt(threatLevel);
    $timeout(function () {
      if (vm.onChange) {
        vm.onChange();
      }
    });
  }
}
