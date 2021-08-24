/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pipe, map, prop, apply, applyTo, xprod, reject, prepend, curry, reverse } from 'ramda';
import { Axes, Components, Dataset, Plots, Scales } from 'plottable';

// This is needed to avoid truncation of max and min scatter points.
// It's equivalent to calling yScale.padProportion(0.08),
// except padProportion() doesn't work properly if domainMin is set (plottable bug).
const Y_SCALE_PADDING_FACTOR = 0.08;
const NUMBER_OF_TICKS = 4;

// @visibleForTesting
export function calculateTickInterval(maxValue) {
  // prevent generating ticks if there is no data
  // (plottable generates ticks from 0 to 1 if there is no data)
  if (maxValue === 0) {
    return 1;
  }

  // if max Value is less than or equal to 1 - use 2 decimals for multiples
  if (maxValue <= 1) {
    return Math.round((1 / NUMBER_OF_TICKS) * 100) / 100;
  }

  var tickInterval = maxValue / NUMBER_OF_TICKS;

  // if tickInterval is more then 5 - make it multiples of 5
  if (tickInterval > 5) {
    return Math.floor(tickInterval / 5) * 5;
  }

  // if tickInterval is between 1 and 5 - make it multiples of 1
  if (tickInterval > 1) {
    return Math.floor(tickInterval);
  }

  return tickInterval;
}

/**
 * @visibleForTesting
 * Given a dataset, and a list of accessor functions to apply to it, find the maximum value that will come
 * out of any of the accessors being applied to any of the data points
 * @param data The dataset
 * @param accessors the list of accessor functions
 */
export const getMaxAccessedValue = pipe(
  xprod, // create all possible data/accessor pairs
  map(apply(applyTo)), // apply the accessor functions
  reject(isNaN), // get rid of invalid results
  prepend(0), // make 0 the minimum/default
  apply(Math.max) // find the max
);

/**
 * Create a full fledged multi-line Plottable scatter chart, complete with axes and labels
 * @param xAccessor The common xAccessor to use for all lines
 * @param xScale a Plottable Scale for the X axis
 * @param xAxis a Plottable Axis object appropriately configured for the xScale
 * @param xAxislabelText Text to display along the X axis, labelling that axis. Can be null if no text is desired
 * @param yAxisLabelText Text to display along the Y Axis, labelling that axis
 * @param lineConfigs A list of objects denoting the different lines in the plot, which are objects having the
 * following properties:
 *   name The name for this line which appears in the legend. The legend will be ordered according to the list order
 *   yAccessor The yAccessor for this line
 *   className The CSS classname to apply to elements on this line
 * The plot lines are guaranteed to be ordered in the DOM according to the REVERSE order of this list (so that lines
 * earlier in the list stack on top). This, combined with the legend ordering, implies that more "important" lines
 * should come earlier in the list
 * @param data the array of objects making up the data set
 */
export function createScatterPlotChart(xAccessor, xScale, xAxis, xAxisLabelText, yAxisLabelText, lineConfigs, data) {
  const colorScale = new Scales.Color().domain(map(prop('name'), lineConfigs)),
    yAccessors = map(prop('yAccessor'), lineConfigs),
    maxValue = getMaxAccessedValue(data, yAccessors),
    padding = Y_SCALE_PADDING_FACTOR * maxValue,
    yScaleTickGenerator = Scales.TickGenerators.intervalTickGenerator(calculateTickInterval(maxValue)),
    yDomainMax = Math.max(1, maxValue) + padding,
    yScale = new Scales.Linear()
      .domainMin(0 - padding)
      .tickGenerator(yScaleTickGenerator)
      .domainMax(yDomainMax),
    // partially applied form of getScatterPlot, ready to take yAccessor, colorDomain, and className as
    // remaining args
    getPlot = getScatterPlot(data, colorScale, xScale, yScale, xAccessor),
    getPlotForLineConfig = ({ name, yAccessor, className }) => getPlot(yAccessor, name, className),
    plots = map(getPlotForLineConfig, reverse(lineConfigs)),
    plotGroup = new Components.Group(plots),
    legend = new Components.Legend(colorScale).maxEntriesPerRow(Infinity),
    yAxis = new Axes.Numeric(yScale, 'left').endTickLength(0),
    xAxisLabel = xAxisLabelText ? new Components.AxisLabel(xAxisLabelText) : null,
    yAxisLabel = new Components.AxisLabel(yAxisLabelText).yAlignment('center').angle(-90),
    tableRows = [
      [null, null, legend],
      [yAxisLabel, yAxis, plotGroup],
      [null, null, xAxis],
    ].concat(xAxisLabel ? [[null, null, xAxisLabel]] : []);

  return new Components.Table(tableRows);
}

const getScatterPlot = curry(function getScatterPlot(
  data,
  colorScale,
  xScale,
  yScale,
  xAccessor,
  yAccessor,
  colorDomain,
  className
) {
  const scatterPlot = new Plots.Scatter()
      .addDataset(new Dataset(data))
      .x(xAccessor, xScale)
      .y(yAccessor, yScale)
      .attr('fill', colorDomain, colorScale)
      .size(8)
      .attr('opacity', 1)
      .attr('class', className),
    linePlot = new Plots.Line()
      .addDataset(new Dataset(data))
      .x(xAccessor, xScale)
      .y(yAccessor, yScale)
      .attr('stroke', colorDomain, colorScale)
      .attr('stroke-width', 0.5)
      .attr('opacity', 1)
      .attr('class', className);

  return new Components.Group([linePlot, scatterPlot]);
});

export const renderChart = (chart, element) => {
  const render = () => {
    if (chart && chart.renderTo) chart.renderTo(element);
  };

  render();

  window.addEventListener('resize', render);

  return () => {
    window.removeEventListener('resize', render);
  };
};
