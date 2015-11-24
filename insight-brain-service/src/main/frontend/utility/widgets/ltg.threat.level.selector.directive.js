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
        threatLevelModel: '=ngModel',
        disabled: '=?ngDisabled'
      },
      templateUrl: 'utility/widgets/ltg.threat.level.selector.directive.html',
      controller: 'LTGThreatLevelSelectorController',
      controllerAs: 'vm',
      bindToController: true,
      link: function(scope, element) {
        scope.$watch('vm.disabled', function(isDisabled) {
          if (isDisabled) {
            element.find('a').removeAttr('data-toggle');
          }
          else {
            element.find('a').attr('data-toggle', 'dropdown');
          }
        });
      }
    };
  }

  angular.module('utility').directive('ltgThreatLevelSelector', LTGThreatLevelSelector);

}(angular));
