/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Dataset, Plots, Scales } from 'plottable';
import template from './violationAveragesChart.html';

export default {
  template,
  controller: violationAveragesChartController,
  controllerAs: 'vm',
  bindings: {
    averagesData: '<',
    isSingleApplicationReport: '<',
    activeApplicationCount: '<',
    monthCount: '<',
  },
};

function violationAveragesChartController() {
  const vm = this,
    { averagesData } = vm;

  vm.averageEvaluationsRounded = Math.round(averagesData.evaluationCount);

  vm.averageDiscoveredTotal = averagesData.totalViolations.averageDiscovered;
  vm.averageDiscoveredSecurity =
    averagesData.securityViolations.averageDiscovered;
  vm.averageDiscoveredLicense =
    averagesData.licenseViolations.averageDiscovered;
  vm.averageDiscoveredQuality =
    averagesData.qualityViolations.averageDiscovered;
  vm.averageDiscoveredOther = averagesData.otherViolations.averageDiscovered;

  vm.averageDiscoveredTotalCritical =
    averagesData.totalViolations.averageDiscoveredCritical;
  vm.averageDiscoveredSecurityCritical =
    averagesData.securityViolations.averageDiscoveredCritical;
  vm.averageDiscoveredLicenseCritical =
    averagesData.licenseViolations.averageDiscoveredCritical;
  vm.averageDiscoveredQualityCritical =
    averagesData.qualityViolations.averageDiscoveredCritical;
  vm.averageDiscoveredOtherCritical =
    averagesData.otherViolations.averageDiscoveredCritical;

  vm.chart = makeChart(averagesData);
}

// map from field name to human-readable name
var columns = {
  securityViolations: 'Security Violations',
  licenseViolations: 'License Violations',
  qualityViolations: 'Quality Violations',
  otherViolations: 'Other Violations',
};

function makeChart(data) {
  var criticalDataset = new Dataset(
      Object.keys(columns).map(function (field) {
        return {
          y: field,
          x: Math.round(data[field].averageDiscoveredCritical),
        };
      }),
      { className: 'iq-chart__dataset--critical' }
    ),
    overallDataset = new Dataset(
      Object.keys(columns).map(function (field) {
        return { y: field, x: Math.round(data[field].averageDiscovered) };
      }),
      { className: 'iq-chart__dataset--overall' }
    ),
    max = overallDataset
      .data()
      .map(function (d) {
        return d.x;
      })
      .reduce(function (a, b) {
        return Math.max(a, b);
      }, 0),
    plot = new Plots.Bar('horizontal')
      .addDataset(overallDataset)
      .addDataset(criticalDataset)
      .y(function (data) {
        return data.y;
      }, new Scales.Category())
      .x(function (data) {
        return data.x;
      }, new Scales.Linear().domain([0, max]))
      .attr('class', function (d, i, dataset) {
        return dataset.metadata().className;
      })
      .attr('fill', function () {
        return undefined;
      });

  return plot;
}
