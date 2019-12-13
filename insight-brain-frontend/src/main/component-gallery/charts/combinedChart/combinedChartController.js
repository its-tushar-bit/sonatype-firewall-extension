/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var scoreData = [
  { hoursStudied: 1, testScore: 1 },
  { hoursStudied: 2, testScore: 3 },
  { hoursStudied: 3, testScore: 2 },
  { hoursStudied: 4, testScore: 4 },
  { hoursStudied: 5, testScore: 3 },
  { hoursStudied: 6, testScore: 5 }
];

var studentData = [
  { hoursStudied: 1, students: 10 },
  { hoursStudied: 2, students: 12 },
  { hoursStudied: 3, students: 15 },
  { hoursStudied: 4, students: 7 },
  { hoursStudied: 5, students: 3 },
  { hoursStudied: 6, students: 2 }
];

function createCombinedChart() {
  // LINE PLOT
  var linePlot = new Plottable.Plots.Line();
  var dataSet = new Plottable.Dataset(scoreData, { name: 'Test Scores' });
  var xScale = new Plottable.Scales.Category();
  var yScale = new Plottable.Scales.Linear();
  linePlot.addDataset(dataSet);
  linePlot.x(function(d) {return d.hoursStudied;}, xScale);
  linePlot.y(function(d) {return d.testScore;}, yScale);

  // BAR PLOT
  var barPlot = new Plottable.Plots.Bar();
  var studentDataSet = new Plottable.Dataset(studentData, { name: 'Students' });
  var yStudentsScale = new Plottable.Scales.Linear();
  barPlot.addDataset(studentDataSet);
  barPlot.x(function(d) { return d.hoursStudied; }, xScale);
  barPlot.y(function(d) { return d.students; }, yStudentsScale);
  barPlot.attr('opacity', 0.3);

  // labels
  var xLabel = new Plottable.Components.AxisLabel('Hours Studied');
  var yLabel = new Plottable.Components.AxisLabel(dataSet.metadata().name, 270);
  var yStudentsLabel = new Plottable.Components.AxisLabel(studentDataSet.metadata().name, 270);

  // Axes
  var xAxis = new Plottable.Axes.Category(xScale, 'bottom');
  var yAxis = new Plottable.Axes.Numeric(yScale, 'left');
  var yStudentsAxis = new Plottable.Axes.Numeric(yStudentsScale, 'left');

  var group = new Plottable.Components.Group([ barPlot, linePlot ]);

  return new Plottable.Components.Table([
    [yLabel, yAxis, group, yStudentsAxis, yStudentsLabel],
    [null, null, xAxis, null, null],
    [null, null, xLabel, null, null]
  ]);
}

export default
function controller() {
  this.combinedChart = createCombinedChart();
}
