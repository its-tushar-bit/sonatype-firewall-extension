/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
(function() {
  'use strict';

  angular.module('BootstrapSlider', []).directive('slider', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=ngModel',
        min: '@',
        max: '@',
        hideLabels: '@'
      },
      priority: 99,
      link: function(scope, element) {
        $(element).slider({
          min: parseInt(scope.min),
          max: parseInt(scope.max),
          value: scope.model,
          orientation: 'horizontal',
          selection: 'after',
          handle: 'square',
          tooltip: 'none',
          labels: !scope.hideLabels,
          showHandleValues: true
        }).on('slide', function(event) {
          scope.$apply(function() {
            scope.model = event.value;
          });
        });

        scope.$watch('model', function(newValue) {
          $(element).slider('setValue', newValue);
        });
      }
    };
  });
}());