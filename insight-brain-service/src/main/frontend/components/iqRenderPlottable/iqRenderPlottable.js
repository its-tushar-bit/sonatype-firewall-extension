/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  /**
   * "iq-render-plottable" reusable component
   *
   * Attributes:
   * - chart {Promise<plottableComponent>}: Promise resolving with plottable component
   *
   * Example:
   * <iq-render-plottable chart="vm.mttrChart"></iq-render-plottable>
   */
  angular.module('components').directive('iqRenderPlottable', ['$window', function($window) {
    return {
      restrict: 'E',
      scope: {
        chart: '<'
      },
      link: function(scope, el) {
        function renderChart() {
          scope.chart.then(function(plot) {
            plot.renderTo(el[0]);
          });
        }

        angular.element($window).on('resize', renderChart);
        scope.$on('$destroy', function() {
          angular.element($window).off('resize', renderChart);
        });

        renderChart();
      }
    };
  }]);

}());
