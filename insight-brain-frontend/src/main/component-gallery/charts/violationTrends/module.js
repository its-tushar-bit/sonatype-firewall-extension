import violationTrends from './violationTrends';
import renderCombinedTrendsChartDirective from './renderCombinedTrendsChartDirective';

export default angular.module('violationTrends', [])
    .directive('renderCombinedTrendsChart', renderCombinedTrendsChartDirective)
    .component('violationTrends', violationTrends);
