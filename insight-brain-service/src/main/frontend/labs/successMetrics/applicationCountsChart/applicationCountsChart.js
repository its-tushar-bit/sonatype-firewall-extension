/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

/* global Plottable */

export default {
  templateUrl: 'labs/successMetrics/applicationCountsChart/applicationCountsChart.html?' + clmBuildTimestamp,
  controller: applicationCountsChartController,
  controllerAs: 'vm'
};

function applicationCountsChartController(successMetricsDataService, $q) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.error = undefined;
  vm.isLoaded = false;

  vm.applicationCount = undefined;

  vm.applicationCountTotalViolating = undefined;
  vm.applicationCountSecurity = undefined;
  vm.applicationCountLicense = undefined;
  vm.applicationCountQuality = undefined;
  vm.applicationCountOther = undefined;

  vm.applicationCountTotalViolatingCritical = undefined;
  vm.applicationCountSecurityCritical = undefined;
  vm.applicationCountLicenseCritical = undefined;
  vm.applicationCountQualityCritical = undefined;
  vm.applicationCountOtherCritical = undefined;

  doLoad();

  function doLoad() {
    vm.error = undefined;

    vm.chart = successMetricsDataService.getApplicationCountsData().then(function(data) {
      vm.isLoaded = true;

      vm.applicationCount = data.activeApplications;

      vm.applicationCountSecurity = data.security.applicationsWithViolations;
      vm.applicationCountLicense = data.license.applicationsWithViolations;
      vm.applicationCountQuality = data.quality.applicationsWithViolations;
      vm.applicationCountOther = data.other.applicationsWithViolations;
      vm.applicationCountTotalViolating = data.total.applicationsWithViolations;

      vm.applicationCountSecurityCritical = data.security.applicationsWithCriticalViolations;
      vm.applicationCountLicenseCritical = data.license.applicationsWithCriticalViolations;
      vm.applicationCountQualityCritical = data.quality.applicationsWithCriticalViolations;
      vm.applicationCountOtherCritical = data.other.applicationsWithCriticalViolations;
      vm.applicationCountTotalViolatingCritical = data.total.applicationsWithCriticalViolations;

      return makeChart(data);
    }, function(error) {
      vm.error = error;
      return $q.reject(error);
    });
  }
}

applicationCountsChartController.$inject = ['successMetricsDataService', '$q'];

function makeDataset(data, valueProp, datasetClassName) {
  var threatCategories = ['security', 'license', 'quality', 'other'],
      columns = threatCategories.map(function(field) {
        return { y: field, x: data[field][valueProp] };
      });

  return new Plottable.Dataset(columns, { className: datasetClassName });
}

function makeChart(data) {
  var overallDataset = makeDataset(data, 'applicationsWithViolations', 'iq-chart__dataset--overall'),
      criticalDataset = makeDataset(data, 'applicationsWithCriticalViolations', 'iq-chart__dataset--critical'),

      max = overallDataset.data()
          .map(function(d) {
            return d.x;
          })
          .reduce(function(a, b) {
            return Math.max(a, b);
          }, 0),

      plot = new Plottable.Plots.Bar('horizontal')
          .addDataset(overallDataset)
          .addDataset(criticalDataset)
          .y(function(data) {
            return data.y;
          }, new Plottable.Scales.Category())
          .x(function(data) {
            return data.x;
          }, new Plottable.Scales.Linear().domain([0, max]))
          .attr('class', function(d, i, dataset) {
            return dataset.metadata().className;
          })
          .attr('fill', function() {
            return undefined;
          });

  return plot;
}
