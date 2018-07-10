import lineChart from './lineChart/lineChart';
import stackedBarChart from './stackedBarChart/stackedBarChart';
import combinedChart from './combinedChart/combinedChart';
import violationTrendsModule from './violationTrends/module';

import renderPlottable from './renderPlottable';

export default angular.module('charts', [violationTrendsModule.name])
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
          })
          .state('Violation Trends', {
            url: '/Violation Trends',
            template: '<violation-trends></violation-trends>'
          });
    });
