/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */

/* global Plottable */

export default {
  templateUrl: 'labs/successMetrics/violationAveragesChart/violationAveragesChart.html?' + clmBuildTimestamp,
  controller: violationAveragesChartController,
  controllerAs: 'vm',
  bindings: {
    averagesData: '<',
    isSingleApplicationReport: '<'
  }
};

function violationAveragesChartController() {
  const vm = this,
      { averagesData } = vm;

  vm.averageEvaluations = averagesData.averageEvaluations;
  vm.averagePolicyViolations = averagesData.averagePolicyViolations;
  vm.averageCriticalPolicyViolations = averagesData.averageCriticalPolicyViolations;

  vm.averageDiscoveredSecurity = sumColumn(averagesData.security);
  vm.averageDiscoveredLicense = sumColumn(averagesData.license);
  vm.averageDiscoveredQuality = sumColumn(averagesData.quality);
  vm.averageDiscoveredOther = sumColumn(averagesData.other);

  vm.averageDiscoveredSecurityCritical = averagesData.security.averageDiscoveredCritical;
  vm.averageDiscoveredLicenseCritical = averagesData.license.averageDiscoveredCritical;
  vm.averageDiscoveredQualityCritical = averagesData.quality.averageDiscoveredCritical;
  vm.averageDiscoveredOtherCritical = averagesData.other.averageDiscoveredCritical;

  vm.chart = makeChart(averagesData);
}

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
        return {y: field, x: Math.round(data[field].averageDiscoveredCritical)};
      }), {className: 'iq-chart__dataset--critical'}),

      overallDataset = new Plottable.Dataset(Object.keys(columns).map(function(field) {
        return {y: field, x: Math.round(sumColumn(data[field]))};
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
