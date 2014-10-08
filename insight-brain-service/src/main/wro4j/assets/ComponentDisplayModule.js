/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var module = angular.module('ComponentDisplay', ['AngularCommon']).run(['$templateCache', function($templateCache) {
        $templateCache.put('pathnames-display',
                '<div pathnames-popover="component.pathnames">' +
                '<em>{{component.pathnames[0] | fileName | truncate:35 }}</em>' +
                '</div>'
        );
        $templateCache.put('unknown-display',
                '<div>' +
                '<em>Unknown</em>' +
                '</div>'
        );
        $templateCache.put('displayname',
                '<span ng-repeat="part in component.displayName.parts">' +
                '<span ng-if="part.field">{{ part.value }}</span>' +
                '<span ng-if="!part.field" class="wrap-force-break">{{ part.value }}</span>' +
                '</span>'
        );
        $templateCache.put('component-display',
                '<div>' +
                '<div ng-if="component.displayName" ng-include="\'displayname\'"></div>' +
                '<div ng-if="!component.displayName && component.pathnames" ng-include="\'pathnames-display\'"></div>' +
                '<div ng-if="!component.displayName && !component.pathnames" ng-include="\'unknown-display\'"></div>' +
                '</div>'
        );
        $templateCache.put('linked-component-display',
                '<a ui-sref="dashboard.component({ hash: component.hash })">' +
                '<div ng-include="\'component-display\'"></div>' +
                '</a>'
        );
      }
      ]
  );

  module.directive('componentDisplay', function() {
    return {
      restrict: 'A',
      replace: true,
      scope: {
        component: '='
      },
      templateUrl: 'linked-component-display'
    };
  });

  module.service('ComponentDisplayNameUtil', function() {
    var renderToString = function(displayName) {
      return jQuery.map(displayName.parts, function(part) {
        return part.value;
      }).join('');
    };
    return {
      renderToString: renderToString
    };
  });
}());
