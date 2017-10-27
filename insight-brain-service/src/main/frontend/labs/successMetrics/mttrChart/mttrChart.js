/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */
/* global d3 */

import Plottable from 'plottable';
import template from './mttrChart.html';

const NUMBER_OF_TICKS = 4;

const mttrChart = {
  template,
  controller,
  controllerAs: 'vm',
  bindings: {
    mttrData: '<'
  }
};

export default mttrChart;

function controller(chartUtilsService) {
  const vm = this;

  vm.mttrChart = makeMttrChart(vm.mttrData);

  function makeMttrChart(dataset) {
    const formatMonth = d3.utcFormat('%b');

    var max = d3.max(dataset, function({mttrInSeconds, criticalMttrInSeconds}) {
      return Math.max(mttrInSeconds, criticalMttrInSeconds);
    });

    const secondsInDay = 24 * 60 * 60;
    const maxDays = max / secondsInDay;

    // This is needed to avoid truncation of max and min scatter points.
    // It's equivalent to calling yScale.padProportion(0.08),
    // except padProportion() doesn't work properly if domainMin is set (plottable bug).
    const padding = 0.08 * maxDays;

    const xScale = new Plottable.Scales.Category();

    const yScaleTickInterval = chartUtilsService.calculateTickInterval(NUMBER_OF_TICKS, maxDays);
    const yScaleTickGenerator = Plottable.Scales.TickGenerators.intervalTickGenerator(yScaleTickInterval);
    // the `< 1` is to handle the case where the domain size is less than 1. 
    // In this case we bump the value to 1 for display purposes.
    const yDomainMax = Math.max(1, maxDays) + padding;

    const yScale = new Plottable.Scales.Linear()
        .domainMin(0 - padding)
        .tickGenerator(yScaleTickGenerator)
        .domainMax(yDomainMax);

    const colorScale = new Plottable.Scales.Color()
        .domain(['All', 'Critical']);

    const legend = new Plottable.Components.Legend(colorScale)
        .maxEntriesPerRow(Infinity);

    const allPlot = getPlot('All', 'mttrInSeconds', dataset, 'iq-chart__dataset--overall');
    const criticalPlot = getPlot('Critical', 'criticalMttrInSeconds', dataset, 'iq-chart__dataset--critical');

    function getYAccessor(key) {
      return function(d) {
        // expected data in seconds; use undefined for nulls to display graph breaks
        return d[key] === null ? undefined : d[key] / secondsInDay;
      };
    }

    function xAccessor(d) {
      return d.timePeriodStart;
    }

    function getPlot(colorDomain, key, data, className) {
      const yAccessor = getYAccessor(key);
      const scatterPlot = new Plottable.Plots.Scatter()
          .addDataset(new Plottable.Dataset(data))
          .x(xAccessor, xScale)
          .y(yAccessor, yScale)
          .attr('fill', colorDomain, colorScale)
          .size(8)
          .attr('opacity', 1)
          .attr('class', className);

      const linePlot = new Plottable.Plots.Line()
          .addDataset(new Plottable.Dataset(data))
          .x(xAccessor, xScale)
          .y(yAccessor, yScale)
          .attr('stroke', colorDomain, colorScale)
          .attr('stroke-width', 0.5)
          .attr('opacity', 1)
          .attr('class', className);

      return new Plottable.Components.Group([linePlot, scatterPlot]);
    }

    const xAxis = new Plottable.Axes.Category(xScale, 'bottom')
        .formatter(time => formatMonth(new Date(time)));

    const yAxis = new Plottable.Axes.Numeric(yScale, 'left').endTickLength(0);

    const yAxisLabel = new Plottable.Components.AxisLabel('Days to Resolve')
        .yAlignment('center')
        .angle(-90);

    const group = new Plottable.Components.Group([allPlot, criticalPlot]);

    return new Plottable.Components.Table([
      [null, null, legend],
      [yAxisLabel, yAxis, group],
      [null, null, xAxis]
    ]);
  }
}

controller.$inject = ['chartUtilsService'];
