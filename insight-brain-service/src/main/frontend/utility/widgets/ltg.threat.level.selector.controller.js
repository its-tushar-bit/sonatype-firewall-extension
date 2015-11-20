/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LTGThreatLevelSelectorController() {
    var vm = this;

    vm.threatLevels = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0];
    vm.selectLevel = selectLevel;

    function selectLevel(threatLevel) {
      vm.threatLevelModel = parseInt(threatLevel);
    }
  }

  angular.module('utility').controller('LTGThreatLevelSelectorController', LTGThreatLevelSelectorController);

}(angular));
