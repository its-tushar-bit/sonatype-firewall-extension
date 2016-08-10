/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('sortable', function() {
    return {
      require: 'sortable',
      controller: [
        '$scope', 'extractColumn', function($scope, extractColumn) {
          var me = this;
          me.sortFields = [];

          $scope.getSortField = function() {
            return me.sortFields;
          };
          me.setSort = function(newFields) {
            if (angular.equals(me.sortFields, newFields)) {
              var column = extractColumn(newFields[0]);
              if (me.sortFields[0] !== column) {
                me.sortFields[0] = column;
              }
              else {
                me.sortFields[0] = '-' + column;
              }
            }
            else {
              me.sortFields = newFields;
            }
          };
        }
      ],
      link: function(scope, element, attrs, sortable) {
        sortable.sortFields = attrs.sortable.split(',');
      }
    };
  });

}());
