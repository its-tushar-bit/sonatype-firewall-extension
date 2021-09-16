/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function MiddleClickDirective() {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {
      // auxclick is mainly supported in newer browsers. If not supported fallback to mousedown.
      var event = 'onauxclick' in document.documentElement ? 'auxclick' : 'mousedown';
      element.on(event, function (e) {
        // make sure we have an ng-click attribute in addition to a middle click
        if (attrs.ngClick && e.which === 2) {
          scope.$eval(attrs.ngClick, { $event: e });
        }
      });
    },
  };
}
