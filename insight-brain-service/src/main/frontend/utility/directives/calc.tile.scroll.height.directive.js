/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function CalcTileScrollHeight($rootScope, $interpolate, EventNameConstant) {
  return {
    restrict: 'A',
    link: CalcTileScrollHeightLink
  };

  function CalcTileScrollHeightLink(scope, element) {
    var selector = element.attr('calc-tile-scroll-height');
    if (!selector) {
      throw new Error('missing selector for calc-tile-scroll-height directive');
    }

    updateHeight($(selector).outerHeight(true));

    scope.$watch(function() {
      return $(selector).outerHeight(true);
    }, function(val) {
      updateHeight(val);
    });

    function updateHeight(height) {
      element.css('height', $interpolate(
          'calc(100% - ({{height}}px + {{paddingTop}} + {{paddingBottom}} + {{marginTop}} + {{marginBottom}}))')({
        height: height,
        paddingTop: element.css('padding-top'),
        paddingBottom: element.css('padding-bottom'),
        marginTop: element.css('margin-top'),
        marginBottom: element.css('margin-bottom')
      }));
      $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY);
    }
  }
}

CalcTileScrollHeight.$inject = ['$rootScope', '$interpolate', 'event.name.constant'];

angular //
    .module('utility.directives') //
    .directive('calcTileScrollHeight', CalcTileScrollHeight);
