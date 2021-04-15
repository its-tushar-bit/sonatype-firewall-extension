/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * Usage: <div detect-scrollbar="state"></div>
 * Where "state" object will be watched for changes, to trigger scrollbar detection
 */
export default function detectScrollbar($window, $timeout, StableBodyService) {
  return {
    restrict: 'A',
    link: ScrollbarDetectorLink,
    scope: {
      state: '<detectScrollbar',
    },
  };

  function ScrollbarDetectorLink(scope, element) {
    let timerId;
    function update() {
      // Natively detect if there is a scrollbar. Note that offsetWidth includes border and padding and will cause
      // false positives in cases where such styling is used.
      if (element[0].offsetWidth > element[0].clientWidth) {
        element.addClass('scrollbar-present');
      } else {
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
    scope.$watch('state', () => StableBodyService.whenStable(update));
  }
}

detectScrollbar.$inject = ['$window', '$timeout', 'stable.body.service'];
