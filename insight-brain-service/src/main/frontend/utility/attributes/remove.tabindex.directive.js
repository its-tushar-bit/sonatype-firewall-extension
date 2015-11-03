/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function RemoveTabindex() {
    return {
      restrict: 'A',
      link: function(scope, element) {
        var unregisterListener = scope.$watch(function() {
          return element.attr('tabindex') !== undefined;
        }, function(hasTabindex) {
          if (hasTabindex) {
            element.removeAttr('tabindex');
            unregisterListener();
          }
        });

        scope.$on('$destroy', function() {
          unregisterListener();
        });
      }
    };
  }

  angular.module('utility').directive('removeTabindex', RemoveTabindex);
}(angular));
