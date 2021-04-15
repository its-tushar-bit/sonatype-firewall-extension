/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function windowEventsFactory($window) {
  return {
    addResizeHandler: function (scope, element, callBack) {
      var width = element.width();
      var height = element.height();

      function callBackWrapper() {
        var newWidth = element.width();
        var newHeight = element.height();
        if (newWidth !== width || newHeight !== height) {
          width = newWidth;
          height = newHeight;
          callBack();
        }
      }

      angular.element($window).on('resize', callBackWrapper);
      scope.$on('$destroy', function () {
        angular.element($window).off('resize', callBackWrapper);
      });
    },
  };
}

windowEventsFactory.$inject = ['$window'];
