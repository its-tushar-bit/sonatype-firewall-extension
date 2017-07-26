/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ScrollbarDetector($window, $timeout, EventNameConstant, StableBodyService) {
  return {
    restrict: 'A',
    link: ScrollbarDetectorLink
  };

  function ScrollbarDetectorLink(scope, element) {
    var timerId;
    function update() {
      // floating scrollbar setting in macOs sometimes bloats the container by a pixel.
      // scrollbar would take up more than a few px, so we're letting small differences slide.
      if ((element.width() - element[0].scrollWidth) > 3) {
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
