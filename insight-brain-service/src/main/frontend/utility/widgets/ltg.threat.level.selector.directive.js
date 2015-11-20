/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LTGThreatLevelSelector() {
    return {
      restrict: 'E',
      scope: {
        threatLevelModel: '=ngModel'
      },
      templateUrl: 'utility/widgets/ltg.threat.level.selector.directive.html',
      controller: 'LTGThreatLevelSelectorController',
      controllerAs: 'vm',
      bindToController: true
    };
  }

  angular.module('utility').directive('ltgThreatLevelSelector', LTGThreatLevelSelector);

}(angular));
