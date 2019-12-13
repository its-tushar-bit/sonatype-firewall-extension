/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var data = [
  { 'date': new Date(2016, 0), 'days': 20 },
  { 'date': new Date(2016, 1), 'days': 18 },
  { 'date': new Date(2016, 2), 'days': 10 },
  { 'date': new Date(2016, 3), 'days': 12 },
  { 'date': new Date(2016, 4), 'days': 8 },
  { 'date': new Date(2016, 5), 'days': 8 },
  { 'date': new Date(2016, 6), 'days': 10 },
  { 'date': new Date(2016, 7), 'days': 11 },
  { 'date': new Date(2016, 8), 'days': 12 },
  { 'date': new Date(2016, 9), 'days': 18 },
  { 'date': new Date(2016, 10), 'days': 16 },
  { 'date': new Date(2016, 11), 'days': 13 }
];

function makeLineChart() {
  var plot = new Plottable.Plots.Line();
  var xScale = new Plottable.Scales.Time().domain([new Date(2016, 0, 1), new Date(2016, 11, 31)]);
  var yScale = new Plottable.Scales.Linear().domainMin(0);
  plot.addDataset(new Plottable.Dataset(data));
  plot.x(function(d) { return d.date; }, xScale);
  plot.y(function(d) { return d.days; }, yScale);

  var xAxis = new Plottable.Axes.Time(xScale, 'bottom');
  var yAxis = new Plottable.Axes.Numeric(yScale, 'left');

  var yAxisLabel = new Plottable.Components.AxisLabel('Days to Resolve').yAlignment('center').angle(-90);
  yAxisLabel.addClass('chart-label');

  return new Plottable.Components.Table([
    [yAxisLabel, yAxis, plot],
    [null, null, xAxis]
  ]);
}

export default
function controller() {
  this.lineChart = makeLineChart();
}
