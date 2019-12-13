/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function CIPTabPane() {
  return {
    scope: {
      directive: '=cipTabPane'
    },
    controller: ['$scope', '$element', '$compile', function($scope, $element, $compile) {
      var childScope,
          childElement;

      $scope.$watch('directive', function (directive) {
        if (childScope) {
          childScope.$destroy();
          childScope = undefined;
        }
        if (childElement) {
          childElement.remove();
          childElement = undefined;
        }
        if (directive) {
          childScope = $scope.$new(true);
          childElement = $compile('<div ' + directive + '></div>')(childScope);
          childElement.appendTo($element);
        }
      });
    }]
  };
}
