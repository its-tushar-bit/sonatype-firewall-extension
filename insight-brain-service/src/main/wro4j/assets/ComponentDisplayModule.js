/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var componentDisplayModule = angular.module('ComponentDisplay', []);

  componentDisplayModule.directive('componentDisplay', function() {
    return {
      restrict: 'A',
      replace: true,
      scope: {
        component: '='
      },
      templateUrl: 'component-display',
      link: function(scope) {
        if(scope.component.gav){
          scope.gav = scope.component.gav;
        } else if(scope.component.gavs && scope.component.gavs.length) {
          scope.gav = scope.component.gavs[0];
        } else if(scope.component.pathnames && scope.component.pathnames.length) {
          scope.pathnames = scope.component.pathnames;
        }
      }
    };
  });

  componentDisplayModule.directive('gavDisplay', function() {
    return {
      restrict: 'A',
      replace: true,
      templateUrl: 'gav-display'
    };
  });
  componentDisplayModule.directive('pathnamesDisplay', function() {
    return {
      restrict: 'A',
      replace: true,
      templateUrl: 'pathnames-display'
    };
  });
  componentDisplayModule.directive('unknownDisplay', function() {
    return {
      restrict: 'A',
      replace: true,
      templateUrl: 'unknown-display'
    };
  });
  componentDisplayModule.directive('displayName', function() {
    return {
      restrict: 'A',
      replace: true,
      templateUrl: 'component-displayname'
    };
  });
}());
