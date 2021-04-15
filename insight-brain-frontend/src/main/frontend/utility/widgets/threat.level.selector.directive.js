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
      threatType: '@',
      disabled: '=?ngDisabled',
    },
    template,
    controller: ThreatLevelSelectorController,
    controllerAs: 'vm',
    bindToController: true,
  };
}

function ThreatLevelSelectorController() {
  var vm = this;

  vm.threatLevels = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0];
  vm.selectLevel = selectLevel;

  function selectLevel(threatLevel) {
    vm.threatLevelModel = parseInt(threatLevel);
  }
}
