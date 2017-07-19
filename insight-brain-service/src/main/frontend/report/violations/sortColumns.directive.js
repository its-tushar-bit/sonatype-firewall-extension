/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var module = angular.module('ReportViolations');

  function sortColumnsDirective() {
    return {
      require: '^sortable',
      scope: {
        field: '@sortColumns', // comma separated list
        inverted: '@?sortInverted', // is the data logically inverted, i.e. AGE vs TIME
        centered: '@?'
      },
      transclude: true,
      template: '<a class="sort-column" ng-click="setSort()">' +
      '<i ng-if="centered" class="sonatype-icons emptyIconGlyph"></i> ' + // should help center
      '<span ng-transclude></span> <i class="sonatype-icons" ng-class="{ up : isUp(), down : isDown(), emptyIconGlyph : !isUp() && !isDown() }"></i></a>',
      link: function(scope, element, attrs, sortableCtrl) {
        var mainSort = scope.field.split(',')[0];
        var isInverted = scope.inverted === 'true';

        scope.setSort = function() {
          sortableCtrl.setSort(scope.field.split(','));
        };

        scope.isUp = function() {
          var sortColumn = extractColumn(sortableCtrl.sortFields[0]);
          var reversed = sortColumn !== sortableCtrl.sortFields[0];
          var currentColumn = extractColumn(mainSort);
          return sortColumn === currentColumn && (isInverted ? reversed : !reversed);
        };

        scope.isDown = function() {
          var sortColumn = extractColumn(sortableCtrl.sortFields[0]);
          var reversed = sortColumn !== sortableCtrl.sortFields[0];
          var currentColumn = extractColumn(mainSort);
          return sortColumn === currentColumn && (!isInverted ? reversed : !reversed);
        };
      }
    };
  }

  function extractColumn(orderedColumn) {
    if (orderedColumn.indexOf('-') === 0) {
      return orderedColumn.substring(1);
    }
    else {
      return orderedColumn;
    }
  }

  module.directive('sortColumns', sortColumnsDirective);

}());
