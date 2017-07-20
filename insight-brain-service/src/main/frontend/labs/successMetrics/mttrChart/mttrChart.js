/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * 'Sonatype' is a trademark of Sonatype, Inc.
 */
/* global d3, Plottable */

var NUMBER_OF_TICKS = 4;

export default {
  templateUrl: 'labs/successMetrics/mttrChart/mttrChart.html?' + clmBuildTimestamp,
  controller: mttrChartController,
  controllerAs: 'vm'
};

function mttrChartController(successMetricsDataService, $q, chartUtilsService) {
  var vm = this;
  vm.doLoad = doLoad;
  vm.error = undefined;

  doLoad();

  function doLoad() {
    vm.mttrChart = successMetricsDataService.getMttrData().then(function(data) {
      vm.isLoaded = true;
      return makeMttrChart(data);
    }, function(error) {
      vm.error = error;
      return $q.reject(error);
    });

    delete vm.error;
  }

  function makeMttrChart(dataset) {

    var formatMonth = d3.utcFormat('%b');

    var max = d3.max(dataset, function(entry) {
      return Math.max(entry.mttrInSeconds, entry.criticalMttrInSeconds);
    });

    var secondsInDay = 24 * 60 * 60;
    var maxDays = max / secondsInDay;

    // This is needed to avoid truncation of max and min scatter points.
    // It's equivalent to calling yScale.padProportion(0.05),
    // except padProportion() doesn't work properly if domainMin is set (plottable bug).
    var padding = 0.05 * maxDays;

    var xScale = new Plottable.Scales.Category();

    var yScaleTickInterval = chartUtilsService.calculateTickInterval(NUMBER_OF_TICKS, maxDays);
    var yScaleTickGenerator = Plottable.Scales.TickGenerators.intervalTickGenerator(yScaleTickInterval);

    var yScale = new Plottable.Scales.Linear()
        .domainMin(0 - padding)
        .tickGenerator(yScaleTickGenerator)

        // the `|| 1` is to handle the case where the data only contains null
        // values.  With a domain of 0 size, they still show up so we need to
        // make the domain (0, 1) in that case
        .domainMax((maxDays + padding) || 1);

    var colorScale = new Plottable.Scales.Color()
        .domain(['All', 'Critical']);

    var legend = new Plottable.Components.Legend(colorScale)
        .maxEntriesPerRow(Infinity);

    var allPlot = getPlot('All', 'mttrInSeconds', dataset, 'iq-chart__dataset--overall');
    var criticalPlot = getPlot('Critical', 'criticalMttrInSeconds', dataset, 'iq-chart__dataset--critical');

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
      var yAccessor = getYAccessor(key);
      var scatterPlot = new Plottable.Plots.Scatter()
          .addDataset(new Plottable.Dataset(data))
          .x(xAccessor, xScale)
          .y(yAccessor, yScale)
          .attr('fill', colorDomain, colorScale)
          .size(8)
          .attr('opacity', 1)
          .attr('class', className);

      var linePlot = new Plottable.Plots.Line()
          .addDataset(new Plottable.Dataset(data))
          .x(xAccessor, xScale)
          .y(yAccessor, yScale)
          .attr('stroke', colorDomain, colorScale)
          .attr('stroke-width', 0.5)
          .attr('opacity', 1)
          .attr('class', className);

      return new Plottable.Components.Group([linePlot, scatterPlot]);
    }

    var xAxis = new Plottable.Axes.Category(xScale, 'bottom')
        .formatter(function(time) {
          return formatMonth(new Date(time));
        });

    var yAxis = new Plottable.Axes.Numeric(yScale, 'left').endTickLength(0);

    var yAxisLabel = new Plottable.Components.AxisLabel('Days to Resolve')
        .yAlignment('center')
        .angle(-90);

    var group = new Plottable.Components.Group([allPlot, criticalPlot]);

    return new Plottable.Components.Table([
      [null, null, legend],
      [yAxisLabel, yAxis, group],
      [null, null, xAxis]
    ]);
  }
}

mttrChartController.$inject = ['successMetricsDataService', '$q', 'chartUtilsService'];
