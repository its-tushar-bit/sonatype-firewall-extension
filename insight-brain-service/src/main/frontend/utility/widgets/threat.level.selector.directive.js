/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ThreatLevelSelector() {
    return {
      restrict: 'E',
      scope: {
        threatLevelModel: '=ngModel',
        threatType: '@',
        disabled: '=?ngDisabled'
      },
      templateUrl: 'utility/widgets/threat.level.selector.directive.html',
      controller: 'threat.level.selector.controller',
      controllerAs: 'vm',
      bindToController: true
    };
  }

  angular.module('utility').directive('threatLevelSelector', ThreatLevelSelector);

}(angular));
