/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * Scroll element to the top of supplied container if it's not visible.
 */
export default function ScrollToTop() {
  return {
    restrict: 'A',
    scope: {
      shouldScroll: '@',
      scrollContainer: '@',
      scrollOffset: '<?'
    },
    link: scrollToTopLink
  };
}

function scrollToTopLink(scope, element) {

  function isScrolledIntoView() {
    const elementRectangle = element[0].getBoundingClientRect();
    const parentRectangle = $(scope.scrollContainer)[0].getBoundingClientRect();
    const relativeTop = elementRectangle.top - parentRectangle.top;
    const relativeBottom = elementRectangle.bottom - parentRectangle.bottom;

    return (relativeTop > 0) && (relativeBottom < 0);
  }

  function scroll() {
    $(scope.scrollContainer).animate({
      scrollTop: $(element).position().top + $(scope.scrollContainer).scrollTop() -
        $(scope.scrollContainer).position().top - (scope.scrollOffset || 0)
    }, 300, 'easeInOutSine');
  }

  if (scope.shouldScroll === 'true' && !isScrolledIntoView()) {
    scroll();
  }

  scope.$watch('shouldScroll', function(newValue) {
    if (newValue === 'true' && !isScrolledIntoView()) {
      scroll();
    }
  });
}
