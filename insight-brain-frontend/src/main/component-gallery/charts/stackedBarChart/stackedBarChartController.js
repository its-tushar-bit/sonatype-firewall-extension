/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var criticalViolations = [
  {policyType: 0, violations: 1},
  {policyType: 1, violations: 6},
  {policyType: 2, violations: 4},
  {policyType: 3, violations: 8}
];
var otherViolations = [
  {policyType: 0, violations: 2},
  {policyType: 1, violations: 1},
  {policyType: 2, violations: 2},
  {policyType: 3, violations: 1}
];

var labels = ['Security Violations', 'License Violations', 'Quality Violations', 'Other Violations'];

function generateViolationsDomain(criticalViolations, otherViolations) {
  return criticalViolations.map(function(critialViolation, index) {
    return '' + (critialViolation.violations +
        otherViolations[index].violations) +
        ' ' + labels[critialViolation.policyType];
  });
}

function generateCriticalDomain(criticalViolations) {
  return criticalViolations.map(function(critialViolation) {
    return '' + critialViolation.violations + ' Critical';
  });
}

function createStackedBarChart() {
  var plot = new Plottable.Plots.StackedBar('horizontal');
  var xScale = new Plottable.Scales.Linear();
  var yScale = new Plottable.Scales.Category();
  var colorScale = new Plottable.Scales.Color();
  colorScale.range(['rgb(177, 0, 38)', '#7b6563']);
  plot.addDataset(new Plottable.Dataset(criticalViolations).metadata('Critical'));
  plot.addDataset(new Plottable.Dataset(otherViolations).metadata('Other'));
  plot.x(function(d) {return d.violations;}, xScale);
  plot.y(function(d) {return d.policyType;}, yScale);
  plot.attr('fill', function(d, i, dataset) {return dataset.metadata();}, colorScale);

  var violationsScale = new Plottable.Scales.Category()
      .domain(generateViolationsDomain(criticalViolations, otherViolations));
  var criticalScale = new Plottable.Scales.Category()
      .domain(generateCriticalDomain(criticalViolations));

  // Axes
  var xAxis = new Plottable.Axes.Numeric(xScale, 'bottom');
  var yViolationsAxis = new Plottable.Axes.Category(violationsScale, 'left');
  var yCriticalAxis = new Plottable.Axes.Category(criticalScale, 'left');

  var legend = new Plottable.Components.Legend(colorScale).maxEntriesPerRow(3);

  return new Plottable.Components.Table([
    [null, null, legend],
    [yViolationsAxis, yCriticalAxis, plot],
    [null, null, xAxis]
  ]);
}

export default
function controller() {
  this.stackedBarChart = createStackedBarChart();
}
