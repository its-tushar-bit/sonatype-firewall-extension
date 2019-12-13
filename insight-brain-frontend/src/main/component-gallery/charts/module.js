/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import lineChart from './lineChart/lineChart';
import stackedBarChart from './stackedBarChart/stackedBarChart';
import combinedChart from './combinedChart/combinedChart';

import renderPlottable from './renderPlottable';

export default angular.module('charts', [])
    .component('lineChart', lineChart)
    .component('stackedBarChart', stackedBarChart)
    .component('combinedChart', combinedChart)
    .directive('renderPlottable', renderPlottable)
    .config(function($stateProvider)
    {
      $stateProvider
          .state('Line chart', {
            url: '/Line chart',
            template: '<line-chart></line-chart>'
          })
          .state('Stacked Bar chart', {
            url: '/Stacked Bar chart',
            template: '<stacked-bar-chart></stacked-bar-chart>'
          })
          .state('Combined chart', {
            url: '/Combined chart',
            template: '<combined-chart></combined-chart>'
          });
    });
