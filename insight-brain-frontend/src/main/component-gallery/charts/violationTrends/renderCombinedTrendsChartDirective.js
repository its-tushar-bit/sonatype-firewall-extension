import renderCombinedTrendsChart from './renderCombinedTrendsChart';

export default function renderCombinedTrendsChartDirective() {
  return {
    scope: {
      data: '<',
      statistics: '<'
    },
    link: function(scope, el) {
      renderCombinedTrendsChart(el[0], scope.data, scope.statistics);
    }
  };
}
