/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

/* global Plottable */
import template from './componentCountsChart.html';

export default {
  template: template,
  controller: componentCountsChartController,
  controllerAs: 'vm'
};

function componentCountsChartController($q, successMetricsDataService) {
  const vm = this;

  vm.error = undefined;
  vm.isLoaded = false;
  vm.showRow = showRow;

  vm.$onInit = function() {
    vm.error = undefined;

    successMetricsDataService.getComponentCountsData().then(function(data) {
      vm.isLoaded = true;
      vm.componentData = data;

      vm.componentsWithMostApplicationsChart = $q.resolve(
          makeChart(data, 'componentsInTheMostApplications', 'iq-chart__dataset--component'));
      vm.componentsWithMostViolationsChart = $q.resolve(
          makeChart(data, 'componentsWithTheMostViolations', 'iq-chart__dataset--critical'));
    }, function(error) {
      vm.error = error;
      return $q.reject(error);
    });
  };

  function showRow(componentDisplayName) {
    return componentDisplayName.indexOf(successMetricsDataService.EMPTY_PREFIX) === -1;
  }
}

componentCountsChartController.$inject = ['$q', 'successMetricsDataService'];

function makeDataset(data, type, datasetClassName) {
  return new Plottable.Dataset(data[type].map(function(element) {
    return {y: element.componentDisplayName, x: element.count};
  }), {className: datasetClassName});
}

function makeChart(data, type, datasetClassName) {
  const dataset = makeDataset(data, type, datasetClassName),
      max = dataset.data().map(d => d.x).reduce((a, b) => Math.max(a, b), 0),
      plot = new Plottable.Plots.Bar('horizontal')
          .addDataset(dataset)
          .y(({y}) => y, new Plottable.Scales.Category())
          .x(({x}) => x, new Plottable.Scales.Linear().domain([0, max]))
          .attr('class', (d, i, dataset) => dataset.metadata().className)
          .attr('fill', () => undefined);

  return plot;
}
