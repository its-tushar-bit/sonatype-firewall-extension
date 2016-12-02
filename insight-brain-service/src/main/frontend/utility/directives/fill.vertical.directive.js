/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function FillVertical($window, $timeout, maximizeHeightService) {
    function link(scope, element) {
      var timerId;

      function updateDimensions() {
        timerId = maximizeHeightService.updateDimensions(element, {bottomPadding: 0}) || timerId;
      }

      function dedupe() {
        if (timerId) {
          $timeout.cancel(timerId);
        }
        timerId = $timeout(updateDimensions, 20);
      }

      $timeout(updateDimensions, 100);
      $($window).resize(dedupe);

      scope.$on('$destroy', function () {
        $($window).unbind('resize', dedupe);
      });
    }

    return {
      link: link
    };
  }
  FillVertical.$inject = ['$window', '$timeout', 'maximizeHeightService'];

  angular.module('utility.directives').directive('fillVertical', FillVertical);
}());
