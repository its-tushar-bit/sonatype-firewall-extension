/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function PadToTop($interval) {
  return {
    restrict: 'A',
    link: PadToTopLink,
  };

  function PadToTopLink(scope, element, attrs) {
    var topTarget,
      refreshPadToTopInterval,
      container = element.parent(),
      isTopTargetElement = !attrs.padToTop,
      originalBottomMargin = element.css('margin-bottom'),
      currentBottomMargin = originalBottomMargin;

    var waitUntilElementReadyInterval = $interval(
      initializeAfterTopTargetReady,
      200
    );

    scope.$on('$destroy', function () {
      if (refreshPadToTopInterval) {
        $interval.cancel(refreshPadToTopInterval);
      }
    });

    function initializeAfterTopTargetReady() {
      var target = isTopTargetElement ? element : $(attrs.padToTop, element);
      if (target && target.length) {
        $interval.cancel(waitUntilElementReadyInterval);

        topTarget = target;
        updatePaddingWithMargin();

        refreshPadToTopInterval = $interval(updatePaddingWithMargin, 1000);
      }
    }

    function updatePaddingWithMargin() {
      var newBottomMargin =
        container.height() > topTargetOuterHeight()
          ? container.height() - topTargetOuterHeight() + 'px'
          : originalBottomMargin;

      if (newBottomMargin !== currentBottomMargin) {
        element.css('margin-bottom', newBottomMargin);
        currentBottomMargin = newBottomMargin;
      }
    }

    function topTargetOuterHeight() {
      return isTopTargetElement
        ? topTarget.outerHeight(true) - parseInt(currentBottomMargin)
        : topTarget.outerHeight(true);
    }
  }
}

PadToTop.$inject = ['$interval'];
