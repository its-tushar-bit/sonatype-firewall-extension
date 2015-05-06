/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('ComponentName', []).run(['$templateCache', function($templateCache) {
    $templateCache.put('displayname',
            '<span ng-repeat="part in displayName.parts">' +
            '<span ng-if="part.field">{{ part.value }}</span>' +
            '<span ng-if="!part.field" class="wrap-force-break">{{ part.value }}</span>' +
            '</span>'
    );
  }]).directive('componentName', function () {
    return {
      restrict: 'A',
      replace: true,
      scope: {
        displayName : '=componentName'
      },
      templateUrl: 'displayname'
    };
  });

  var module = angular.module('ComponentDisplay', ['AngularCommon', 'ComponentName']).run(['$templateCache', function($templateCache) {
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
        $templateCache.put('component-display',
                '<div>' +
                '<div ng-if="component.displayName"><span component-name="component.displayName"></span></div>' +
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
