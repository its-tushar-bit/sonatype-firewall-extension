import Plottable from 'plottable';
import {prop} from 'ramda';
import TrendsTooltip from './trendsTooltip';

export default function renderCombinedTrendsChart(el, {delta, discovered, waived, fixed}, statistics) {

  const xScale = new Plottable.Scales.Linear();

  const deltaBar = generateBarPlot(xScale, delta, statistics.deltaMax, statistics.deltaMin);
  const newBar = generateBarPlot(xScale, discovered, statistics.newMax);
  const waivedBar = generateBarPlot(xScale, waived, statistics.waivedMax);
  const fixedBar = generateBarPlot(xScale, fixed, statistics.fixedMax);
  const guidelineChart = generateGuidelineGroup(xScale, discovered);

  const table = new Plottable.Components.Table([
    // Empty label as a 24px spacer.
    [new Plottable.Components.Label('').padding(12)],
    [deltaBar],
    // Empty label as a 48px spacer.
    [new Plottable.Components.Label('').padding(24)],
    [newBar],
    // Empty label as a 48px spacer.
    [new Plottable.Components.Label('').padding(24)],
    [waivedBar],
    // Empty label as a 48px spacer.
    [new Plottable.Components.Label('').padding(24)],
    [fixedBar]
  ]);

  const combinedChart = new Plottable.Components.Group([table, guidelineChart]);

  combinedChart.renderTo(el);

  // interactions
  const violationTrendsElement = $(el).closest('violation-trends')[0];
  const deltaBarTooltip = TrendsTooltip('deltaBarTooltip', violationTrendsElement);
  const newBarTooltip = TrendsTooltip('newBarTooltip', violationTrendsElement);
  const waivedBarTooltip = TrendsTooltip('waivedBarTooltip', violationTrendsElement);
  const fixedBarTooltip = TrendsTooltip('fixedBarTooltip', violationTrendsElement);
  const guidelineTooltip = TrendsTooltip('guidelineTooltip', violationTrendsElement);

  hideGuideline();

  const interaction = new Plottable.Interactions.Pointer();
  interaction
      .onPointerMove(point => {
        // reuse NearestEntity of delta bar chart for guideline chart - to make sure they stay in sync
        const deltaBarNearestEntity = deltaBar.entityNearest(point);
        moveGuidelineAndTooltip(deltaBarNearestEntity, guidelineChart, guidelineTooltip);
        moveBarTooltip(deltaBarNearestEntity, deltaBar, deltaBarTooltip, true);
        moveBarTooltip(newBar.entityNearest(point), newBar, newBarTooltip);
        moveBarTooltip(waivedBar.entityNearest(point), waivedBar, waivedBarTooltip);
        moveBarTooltip(fixedBar.entityNearest(point), fixedBar, fixedBarTooltip);
      })
      .onPointerExit(() => {
        hideGuideline();
        deltaBarTooltip.hide();
        newBarTooltip.hide();
        waivedBarTooltip.hide();
        fixedBarTooltip.hide();
        guidelineTooltip.hide();
      });

  interaction.attachTo(combinedChart);

  function hideGuideline() {
    guidelineChart.components()[1].pixelPosition(-10);
  }

  return combinedChart;
}

function moveGuidelineAndTooltip(nearestEntity, guidelineChart, tooltip) {
  const guidelinePlot = guidelineChart.components()[0];
  const guideline = guidelineChart.components()[1];
  const position = getGuidelineTooltipPosition(nearestEntity, guidelinePlot);
  guideline.value(nearestEntity.datum.week);
  tooltip.show(position.left, position.top, `Week ${nearestEntity.datum.week}`);
}

function moveBarTooltip(nearestEntity, plot, tooltip, showTrendArrow) {
  const position = getBarTooltipPosition(nearestEntity, plot);
  let trendsIcon = '';

  if (showTrendArrow) {
    if (nearestEntity.datum.violations > 0) {
      trendsIcon = '<i class="fa fa-caret-up"></i>';
    }
    else if (nearestEntity.datum.violations < 0) {
      trendsIcon = '<i class="fa fa-caret-down"></i>';
    }
  }

  tooltip.show(position.left, position.top, nearestEntity.datum.violations + trendsIcon);
}

function getPlotPosition(plot) {
  const plotNode = plot.foreground().node();
  const rect = plotNode.getBoundingClientRect();
  const scrollLeft = document.body.scrollLeft || document.documentElement.scrollLeft;
  return {
    left: rect.left + scrollLeft,
    top: rect.top
  };
}

function getBarTooltipPosition(nearestEntity, plot) {
  const offsetVertical = -20;
  const offsetHorizontal = 5;
  const plotPosition = getPlotPosition(plot);
  return {
    left: plotPosition.left + nearestEntity.position.x + offsetHorizontal,
    top: plotPosition.top + offsetVertical
  };
}

function getGuidelineTooltipPosition(nearestEntity, plot) {
  const offsetVertical = -20;
  const plotPosition = getPlotPosition(plot);
  return {
    left: plotPosition.left + nearestEntity.position.x,
    top: plotPosition.top + offsetVertical
  };
}

function generateBarPlot(xScale, data, max, min) {
  const yScale = new Plottable.Scales.Linear();
  if (max) {
    yScale.domainMax(max);
  }
  if (min) {
    yScale.domainMin(min);
  }
  return new Plottable.Plots.Bar()
      .addDataset(new Plottable.Dataset(data))
      .x(prop('week'), xScale)
      .y(prop('violations'), yScale)
      .attr('opacity', 0.9);
}

function generateGuidelineGroup(xScale, data) {
  const yScale = new Plottable.Scales.Linear();
  const linePlot = new Plottable.Plots.Line()
      .addDataset(new Plottable.Dataset(data))
      .x(prop('week'), xScale)
      .y(0, yScale)
      .attr('opacity', 0);

  const guideline = new Plottable.Components.GuideLineLayer(Plottable.Components.GuideLineLayer.ORIENTATION_VERTICAL)
      .addClass('black')
      .scale(xScale);

  return new Plottable.Components.Group([linePlot, guideline]);
}
