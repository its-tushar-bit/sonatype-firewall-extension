/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ScrollbarDetector($window, $timeout, EventNameConstant, StableBodyService) {
    return {
      restrict: 'A',
      link: ScrollbarDetectorLink
    };

    function ScrollbarDetectorLink(scope, element) {
      var timerId;
      function update() {
        if (element[0].scrollWidth < element.width()) {
          element.addClass('scrollbar-present');
        }
        else {
          element.removeClass('scrollbar-present');
        }
      }
      function debounce() {
        if (timerId) {
          $timeout.cancel(timerId);
        }
        timerId = $timeout(update, 50);
      }
      angular.element($window).bind('resize', debounce);
      StableBodyService.whenStable(update);
      scope.$on(EventNameConstant.UPDATE_DASHBOARD_FILTERS, function() {
        StableBodyService.whenStable(update);
      });
    }
  }

  ScrollbarDetector.$inject = ['$window', '$timeout', 'event.name.constant', 'stable.body.service'];

  angular //
      .module('utility.directives') //
      .directive('detectScrollbar', ScrollbarDetector);

}(angular));
