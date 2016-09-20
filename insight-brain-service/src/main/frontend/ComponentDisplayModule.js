/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('ComponentName', []).run(['$templateCache', function($templateCache) {
    $templateCache.put('displayname',
            '<span ng-repeat="part in displayName.parts">' +
            '<span ng-if="part.field">{{ part.value | periodDelimiter }}</span>' +
            '<span ng-if="!part.field" class="wrap-force-break">{{ part.value | periodDelimiter }}</span>' +
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
                '<div class="pathname">' +
                '<em>{{component.pathnames[0] | fileName}}</em>' +
                '</div>'
        );
        $templateCache.put('unknown-display',
                '<div>' +
                '<em>Unknown</em>' +
                '</div>'
        );
        $templateCache.put('component-display',
                '<div>' +
                '<div ng-if="component.displayName" ng-class="{\'truncate-ellipsis\': truncate}"><span component-name="component.displayName"></span></div>' +
                '<div ng-if="!component.displayName && component.pathnames" ng-include="\'pathnames-display\'" ng-class="{\'truncate-ellipsis\': truncate}"></div>' +
                '<div ng-if="!component.displayName && !component.pathnames" ng-include="\'unknown-display\'" ng-class="{\'truncate-ellipsis\': truncate}"></div>' +
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
        component: '=',
        truncate: '@'
      },
      templateUrl: 'linked-component-display'
    };
  });

  module.service('ComponentDisplayNameUtil', ['$filter', function($filter) {
    var renderToString = function(displayName) {
      return $.map(displayName.parts, function(part) {
        return part.value;
      }).join('');
    };

    var deriveComponentName = function(component) {
      if (component.displayName) {
        return renderToString(component.displayName);
      }
      else {
        return component.pathnames ? $filter('fileName')(component.pathnames[0]) : 'Unknown';
      }
    };

    return {
      renderToString: renderToString,
      deriveComponentName: deriveComponentName
    };
  }]);

  module.filter('periodDelimiter', function() {
    return addWordBreakAfterPeriods;

    function addWordBreakAfterPeriods(input) {
      // NOTE: You can't see it, but we are replacing the periods with a period followed by a zero-width space.
      // This makes our periods into word breaking delimiters. Also, we only replace the periods in between words as
      // to preserve version numbers.
      return input.replace(/(?=\.\D+)\.(?=\D+)/g, '.​');
    }
  });
}());
