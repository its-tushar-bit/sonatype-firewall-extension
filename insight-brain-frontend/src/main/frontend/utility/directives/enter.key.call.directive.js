/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function EnterKeyCallDirective() {
  return {
    restrict: 'A',
    scope: false,
    link: function (scope, element, attrs) {
      element.bind('keydown', function (e) {
        if (e.keyCode === 13) {
          // Enter
          e.preventDefault();
          scope.$apply(function () {
            scope.$eval(attrs.enterKeyCall);
          });
        }
      });
    },
  };
}
