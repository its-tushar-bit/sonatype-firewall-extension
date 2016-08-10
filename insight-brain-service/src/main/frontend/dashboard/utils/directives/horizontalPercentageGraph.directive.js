/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global AngularUtils */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  /**
   * expects a model in the following format
   * {
       total: 7, //the total count of all items
   *   items: [{
   *     count: 1, //the count associated with the item]
   *     label: 'label', //the label put in the legend
   *     colorCss: 'css-class' //the css class to assign to the graph section for this item,
   *       and the legend for this item
   *   }]
   * }
   *
   * ex.
   * <div horizontal-percentage-graph model="myData"></div>
   *
   * This directive should be moved someplace more generic, but I don't feel like bloating
   * AngularCommon at the moment..
   */
  dashboardUtilsModule.directive('horizontalPercentageGraph', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=model'
      },
      templateUrl: 'horizontal-percentage-graph',
      controller: [
        '$scope', function($scope) {
          $scope.formatPercentage = AngularUtils.formatPercentage;
        }
      ]
    };
  });

}());
