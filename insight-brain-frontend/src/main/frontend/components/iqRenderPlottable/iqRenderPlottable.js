/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * "iq-render-plottable" reusable component
 *
 * Attributes:
 * - chart {plottableComponent|Promise<plottableComponent>}: A plottable component, or a Promise resolving to one
 *
 * Example:
 * <iq-render-plottable chart="vm.mttrChart"></iq-render-plottable>
 */
export default function iqRenderPlottable($window, $q) {
  return {
    restrict: 'E',
    scope: {
      chart: '<',
    },
    link: function (scope, el) {
      function renderChart() {
        $q.when(scope.chart).then(function (plot) {
          plot.renderTo(el[0]);
        });
      }

      angular.element($window).on('resize', renderChart);
      scope.$on('$destroy', function () {
        angular.element($window).off('resize', renderChart);
      });

      renderChart();
    },
  };
}

iqRenderPlottable.$inject = ['$window', '$q'];
