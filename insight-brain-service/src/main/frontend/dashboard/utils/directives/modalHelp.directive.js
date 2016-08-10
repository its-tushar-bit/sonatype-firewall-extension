/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('modalHelp', [
    '$modal', function($modal) {
      return {
        restrict: 'A',
        scope: {
          modalHelp: '@',
          modalHelpClass: '@',
          modalHelpTrigger: '@'
        },
        link: function(scope, element) {
          var helpClass = 'modal-help';
          if (scope.modalHelpClass) {
            helpClass = scope.modalHelpClass;
          }

          var trigger = 'click';
          if (scope.modalHelpTrigger) {
            trigger = scope.modalHelpTrigger;
          }

          var options = {
            templateUrl: scope.modalHelp,
            windowClass: 'clm-modal ' + helpClass
          };

          element.on(trigger, function() {
            $modal.open(options);
          });
        }
      };
    }
  ]);

}());
