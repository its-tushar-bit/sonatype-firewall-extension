/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('alphaBackground', [
    function() {
      return {
        scope: {
          alphaBackground: '@'
        },
        link: function(scope, element) {
          var backgroundProperty = 'background-color';
          var background = element.css(backgroundProperty);
          var backgroundMatched = /(rgb|rgba)\((.*)\)/.exec(background);
          if (!backgroundMatched || backgroundMatched.length < 2) {
            return;
          }
          var rgb = backgroundMatched[2].split(',').map(function(color) {
            return parseInt(color);
          });

          // enforce a lower bound on all alpha values
          scope.alphaBackground = (9 * scope.alphaBackground + 1) / 10;

          if (rgb.length === 4) {
            rgb[3] = scope.alphaBackground;
          }
          else {
            rgb.push(scope.alphaBackground);
          }
          background = 'rgba(' + rgb.join(',') + ')';
          element.css(backgroundProperty, background);
        }
      };
    }
  ]);

}());
