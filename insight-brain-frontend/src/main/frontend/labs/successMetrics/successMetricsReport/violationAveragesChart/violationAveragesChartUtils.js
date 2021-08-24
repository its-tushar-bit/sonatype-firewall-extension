/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Dataset, Plots, Scales } from 'plottable';

var columns = {
  securityViolations: 'Security Violations',
  licenseViolations: 'License Violations',
  qualityViolations: 'Quality Violations',
  otherViolations: 'Other Violations',
};

export function makeChart(data) {
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
      .y(function (dt) {
        return dt.y;
      }, new Scales.Category())
      .x(function (dt) {
        return dt.x;
      }, new Scales.Linear().domain([0, max]))
      .attr('class', function (d, i, dataset) {
        return dataset.metadata().className;
      })
      .attr('fill', function () {
        return undefined;
      });

  return plot;
}
