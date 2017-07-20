/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

/* global Plottable */

export default {
  templateUrl: 'labs/successMetrics/violationAveragesChart/violationAveragesChart.html?' + clmBuildTimestamp,
  controller: violationAveragesChartController,
  controllerAs: 'vm'
};

function violationAveragesChartController(successMetricsDataService, $q) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.error = undefined;
  vm.isLoaded = false;

  vm.averageDiscoveredSecurity = undefined;
  vm.averageDiscoveredLicense = undefined;
  vm.averageDiscoveredQuality = undefined;
  vm.averageDiscoveredOther = undefined;

  vm.averageDiscoveredSecurityCritical = undefined;
  vm.averageDiscoveredLicenseCritical = undefined;
  vm.averageDiscoveredQualityCritical = undefined;
  vm.averageDiscoveredOtherCritical = undefined;

  doLoad();

  function doLoad() {
    vm.error = undefined;

    vm.chart = successMetricsDataService.getAveragesData().then(function(data) {
      vm.isLoaded = true;

      vm.averageEvaluations = data.averageEvaluations;
      vm.averagePolicyViolations = data.averagePolicyViolations;
      vm.averageCriticalPolicyViolations = data.averageCriticalPolicyViolations;

      vm.averageDiscoveredSecurity = sumColumn(data.security);
      vm.averageDiscoveredLicense = sumColumn(data.license);
      vm.averageDiscoveredQuality = sumColumn(data.quality);
      vm.averageDiscoveredOther = sumColumn(data.other);

      vm.averageDiscoveredSecurityCritical = data.security.averageDiscoveredCritical;
      vm.averageDiscoveredLicenseCritical = data.license.averageDiscoveredCritical;
      vm.averageDiscoveredQualityCritical = data.quality.averageDiscoveredCritical;
      vm.averageDiscoveredOtherCritical = data.other.averageDiscoveredCritical;

      return makeChart(data);
    }, function(error) {
      vm.error = error;
      return $q.reject(error);
    });
  }
}

violationAveragesChartController.$inject = ['successMetricsDataService', '$q'];

function sumColumn(colData) {
  return colData.averageDiscoveredLow + colData.averageDiscoveredModerate + colData.averageDiscoveredSevere +
      colData.averageDiscoveredCritical;
}

// map from field name to human-readable name
var columns = {
  security: 'Security Violations',
  license: 'License Violations',
  quality: 'Quality Violations',
  other: 'Other Violations'
};

function makeChart(data) {
  var criticalDataset = new Plottable.Dataset(Object.keys(columns).map(function(field) {
        return {y: field, x: data[field].averageDiscoveredCritical};
      }), {className: 'iq-chart__dataset--critical'}),

      overallDataset = new Plottable.Dataset(Object.keys(columns).map(function(field) {
        return {y: field, x: sumColumn(data[field])};
      }), {className: 'iq-chart__dataset--overall'}),

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
