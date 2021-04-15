/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Dataset, Plots, Scales } from 'plottable';
import template from './applicationCountsChart.html';

export default {
  template,
  controller: applicationCountsChartController,
  controllerAs: 'vm',
  bindings: {
    applicationCountsData: '<',
    monthCount: '<',
  },
};

function applicationCountsChartController() {
  const vm = this,
    { applicationCountsData } = vm;

  vm.applicationCount = applicationCountsData.activeApplications;

  vm.applicationCountSecurity = applicationCountsData.security.applicationsWithViolations;
  vm.applicationCountLicense = applicationCountsData.license.applicationsWithViolations;
  vm.applicationCountQuality = applicationCountsData.quality.applicationsWithViolations;
  vm.applicationCountOther = applicationCountsData.other.applicationsWithViolations;
  vm.applicationCountTotalViolating = applicationCountsData.total.applicationsWithViolations;

  vm.applicationCountSecurityCritical = applicationCountsData.security.applicationsWithCriticalViolations;
  vm.applicationCountLicenseCritical = applicationCountsData.license.applicationsWithCriticalViolations;
  vm.applicationCountQualityCritical = applicationCountsData.quality.applicationsWithCriticalViolations;
  vm.applicationCountOtherCritical = applicationCountsData.other.applicationsWithCriticalViolations;
  vm.applicationCountTotalViolatingCritical = applicationCountsData.total.applicationsWithCriticalViolations;

  vm.chart = makeChart(applicationCountsData);
}

function makeDataset(data, valueProp, datasetClassName) {
  var threatCategories = ['security', 'license', 'quality', 'other'],
    columns = threatCategories.map(function (field) {
      return { y: field, x: data[field][valueProp] };
    });

  return new Dataset(columns, { className: datasetClassName });
}

function makeChart(data) {
  var overallDataset = makeDataset(data, 'applicationsWithViolations', 'iq-chart__dataset--overall'),
    criticalDataset = makeDataset(data, 'applicationsWithCriticalViolations', 'iq-chart__dataset--critical'),
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
