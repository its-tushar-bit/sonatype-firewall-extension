/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Scales, Components, Interactions, Plots, Dataset } from 'plottable';
import $ from 'jquery';
import {
  props,
  prop,
  sum,
  compose,
  zipWith,
  reduce,
  head,
  tail,
  map,
  negate,
  lensProp,
  over,
  apply,
  prepend,
} from 'ramda';

const GUIDELINE_TOOLTIP_OFFSET_VERTICAL = -25;
export const GUIDELINE_TOOLTIP_RIGHTMOST_PADDING = 30;
const BAR_TOOLTIP_OFFSET_HORIZONTAL = 5;
const CHART_HEIGHT = 31;
const CHART_PADDING = 24;
const ROW_HEIGHT = CHART_PADDING * 2 + CHART_HEIGHT;

const sumCounts = compose(sum, props(['LOW', 'MODERATE', 'SEVERE', 'CRITICAL']));

const negateData = map(over(lensProp('violations'), negate));

const sumData = zipWith((a, b) => ({
  ...a,
  violations: a.violations + b.violations,
}));

function getAllCounts(...arrays) {
  return reduce(sumData, head(arrays), tail(arrays));
}

function getDelta({ discovered, fixed, waived }) {
  return getAllCounts(discovered, negateData(fixed), negateData(waived));
}

const getPositiveMaxOrZero = compose(apply(Math.max), prepend(0), map(prop('violations')));
const getNegativeMinOrZero = compose(apply(Math.min), prepend(0), map(prop('violations')));

export function getViolationTrendsData(violationCounts) {
  const security = { discovered: [], fixed: [], waived: [] },
    license = { discovered: [], fixed: [], waived: [] },
    quality = { discovered: [], fixed: [], waived: [] },
    other = { discovered: [], fixed: [], waived: [] };

  violationCounts.forEach(({ timePeriodName, discoveredCounts, waivedCounts, fixedCounts }, i) => {
    security.discovered.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(discoveredCounts.SECURITY),
    });
    security.waived.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(waivedCounts.SECURITY),
    });
    security.fixed.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(fixedCounts.SECURITY),
    });

    license.discovered.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(discoveredCounts.LICENSE),
    });
    license.waived.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(waivedCounts.LICENSE),
    });
    license.fixed.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(fixedCounts.LICENSE),
    });

    quality.discovered.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(discoveredCounts.QUALITY),
    });
    quality.waived.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(waivedCounts.QUALITY),
    });
    quality.fixed.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(fixedCounts.QUALITY),
    });

    other.discovered.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(discoveredCounts.OTHER),
    });
    other.waived.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(waivedCounts.OTHER),
    });
    other.fixed.push({
      timePeriodIndex: i,
      timePeriodName,
      violations: sumCounts(fixedCounts.OTHER),
    });
  });

  // calculate deltas
  security.delta = getDelta(security);
  license.delta = getDelta(license);
  quality.delta = getDelta(quality);
  other.delta = getDelta(other);

  // all violations
  const all = {
    discovered: getAllCounts(security.discovered, license.discovered, quality.discovered, other.discovered),
    fixed: getAllCounts(security.fixed, license.fixed, quality.fixed, other.fixed),
    waived: getAllCounts(security.waived, license.waived, quality.waived, other.waived),
  };
  all.delta = getDelta(all);

  const statistics = {
    deltaMax: getPositiveMaxOrZero(all.delta),
    deltaMin: getNegativeMinOrZero(all.delta),
    discoveredMax: getPositiveMaxOrZero(all.discovered),
    fixedMax: getPositiveMaxOrZero(all.fixed),
    waivedMax: getPositiveMaxOrZero(all.waived),
  };

  return {
    all,
    security,
    license,
    quality,
    other,
    statistics,
  };
}

const getViolationsSum = compose(sum, map(prop('violations')));

export function calculateTotals({ discovered, waived, fixed }) {
  const totalDiscovered = getViolationsSum(discovered),
    totalWaived = getViolationsSum(waived),
    totalFixed = getViolationsSum(fixed),
    totalDelta = totalDiscovered - totalWaived - totalFixed;
  return {
    totalDiscovered,
    totalWaived,
    totalFixed,
    totalDelta,
  };
}

export function moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip) {
  // set content before we get tooltip width
  tooltip.setContent(nearestEntity.datum.timePeriodName);
  const position = getGuidelineTooltipPosition(el, nearestEntity, tooltip.getWidth());
  guideline.value(nearestEntity.datum.timePeriodIndex);
  tooltip.show(position.left, position.top);
}

export function moveBarTooltip(el, nearestEntity, tooltip, tooltipOffsetTop, showTrendArrow) {
  tooltipOffsetTop = tooltipOffsetTop || 0;
  const position = getBarTooltipPosition(el, nearestEntity, tooltipOffsetTop);
  let trendsIcon = '';

  if (showTrendArrow) {
    if (nearestEntity.datum.violations > 0) {
      trendsIcon = getTrendsIconHtml('up');
    } else if (nearestEntity.datum.violations < 0) {
      trendsIcon = getTrendsIconHtml('down');
    }
  }

  tooltip.show(position.left, position.top, Math.abs(nearestEntity.datum.violations) + trendsIcon);
}

function getBarTooltipPosition(el, nearestEntity, tooltipOffsetTop) {
  return {
    left: el.offsetLeft + nearestEntity.position.x + BAR_TOOLTIP_OFFSET_HORIZONTAL,
    top: el.offsetTop + tooltipOffsetTop,
  };
}

function getGuidelineTooltipPosition(el, nearestEntity, tooltipWidth) {
  let left = el.offsetLeft + nearestEntity.position.x - tooltipWidth / 2; // centered tooltip
  const offsetRight = el.offsetParent.offsetWidth - (left + tooltipWidth);
  const rightOverlap = GUIDELINE_TOOLTIP_RIGHTMOST_PADDING - offsetRight;
  if (rightOverlap > 0) {
    left = left - rightOverlap;
  }
  return {
    left,
    top: el.offsetTop + GUIDELINE_TOOLTIP_OFFSET_VERTICAL,
  };
}

export function generateBarPlot(xScale, data, barClass, max, min) {
  const yScale = new Scales.Linear();
  if (max) {
    yScale.domainMax(max);
  }
  yScale.domainMin(min || 0);

  return new Plots.Bar()
    .addDataset(new Dataset(data))
    .x(prop('timePeriodIndex'), xScale)
    .y(prop('violations'), yScale)
    .attr('width', 7)
    .attr('class', barClass);
}

export function generateGuidelinePlot(xScale, data) {
  return new Plots.Line()
    .addDataset(new Dataset(data))
    .x(prop('timePeriodIndex'), xScale)
    .y(0, new Scales.Linear())
    .attr('opacity', 0);
}

function getTrendsIconHtml(iconClassSuffix) {
  return `<i class="iq-violation-trends__delta-icon fa fa-caret-${iconClassSuffix}"></i>`;
}

function TrendsTooltip(id, container) {
  const tooltipElement = document.getElementById(id) || createTooltipElement(id, container);
  return {
    setContent(content) {
      tooltipElement.innerHTML = content;
    },
    /**
     * @param left
     * @param top
     * @param content - optional, overrides existing content
     */
    show(left, top, content) {
      if (content) {
        tooltipElement.innerHTML = content;
      }
      tooltipElement.style.left = left + 'px';
      tooltipElement.style.top = top + 'px';
      tooltipElement.style.visibility = 'visible';
    },
    hide() {
      // use visibility:hidden instead of display:none, to be able to get tooltip width before showing it again
      tooltipElement.style.visibility = 'hidden';
    },
    getWidth() {
      return tooltipElement.offsetWidth;
    },
  };
}

function createTooltipElement(id, container) {
  const tooltip = document.createElement('div');
  tooltip.setAttribute('id', id);
  tooltip.setAttribute('class', 'iq-violation-trends__tooltip');
  container.appendChild(tooltip);
  return tooltip;
}

export function renderCombinedTrendsChart(element, { delta, discovered, waived, fixed }, statistics) {
  let el;

  if (typeof element === 'string') el = document.querySelector(element);
  else el = element;

  const xScale = new Scales.Linear().padProportion(0);

  const deltaBarClass = (d) => `iq-violation-trends__bar--delta-${d.violations > 0 ? 'up' : 'down'}`;

  const deltaBar = generateBarPlot(xScale, delta, deltaBarClass, statistics.deltaMax, statistics.deltaMin);
  const newBar = generateBarPlot(xScale, discovered, 'iq-violation-trends__bar--discovered', statistics.discoveredMax);
  const waivedBar = generateBarPlot(xScale, waived, 'iq-violation-trends__bar--waived', statistics.waivedMax);
  const fixedBar = generateBarPlot(xScale, fixed, 'iq-violation-trends__bar--fixed', statistics.fixedMax);
  const guidelineChart = generateGuidelinePlot(xScale, discovered);
  const guideline = new Components.GuideLineLayer(Components.GuideLineLayer.ORIENTATION_VERTICAL)
    .addClass('iq-violation-trends__guideline')
    .scale(xScale);
  const guidelineGroup = new Components.Group([guidelineChart, guideline]);

  const table = new Components.Table([
    // Empty label as a 24px spacer.
    [new Components.Label('').padding(CHART_PADDING / 2)],
    [deltaBar],
    // Empty label as a 48px spacer.
    [new Components.Label('').padding(CHART_PADDING)],
    [newBar],
    // Empty label as a 48px spacer.
    [new Components.Label('').padding(CHART_PADDING)],
    [waivedBar],
    // Empty label as a 48px spacer.
    [new Components.Label('').padding(CHART_PADDING)],
    [fixedBar],
  ]);

  const combinedChart = new Components.Group([table, guidelineGroup]);

  // rendered chart before attaching interactions
  combinedChart.renderTo(el);

  // interactions
  const violationTrendsElement = $(el).closest('#violation-trends-chart')[0];
  const deltaBarTooltip = TrendsTooltip('deltaBarTooltip', violationTrendsElement);
  const newBarTooltip = TrendsTooltip('newBarTooltip', violationTrendsElement);
  const waivedBarTooltip = TrendsTooltip('waivedBarTooltip', violationTrendsElement);
  const fixedBarTooltip = TrendsTooltip('fixedBarTooltip', violationTrendsElement);
  const guidelineTooltip = TrendsTooltip('guidelineTooltip', violationTrendsElement);
  let nearestEntityIndex = null;

  hideGuideline();

  const interaction = new Interactions.Pointer();
  interaction
    .onPointerMove((point) => {
      const nearestEntity = guidelineChart.entityNearest(point);

      // do not move tooltips if NearestEntity has not changed
      if (nearestEntityIndex === nearestEntity.index) {
        return;
      }

      nearestEntityIndex = nearestEntity.index;
      moveGuidelineAndTooltip(el, nearestEntity, guideline, guidelineTooltip);
      moveBarTooltip(el, deltaBar.entities()[nearestEntityIndex], deltaBarTooltip, 0, true);
      moveBarTooltip(el, newBar.entities()[nearestEntityIndex], newBarTooltip, ROW_HEIGHT);
      moveBarTooltip(el, waivedBar.entities()[nearestEntityIndex], waivedBarTooltip, ROW_HEIGHT * 2);
      moveBarTooltip(el, fixedBar.entities()[nearestEntityIndex], fixedBarTooltip, ROW_HEIGHT * 3);
    })
    .onPointerExit(() => {
      hideGuideline();
      deltaBarTooltip.hide();
      newBarTooltip.hide();
      waivedBarTooltip.hide();
      fixedBarTooltip.hide();
      guidelineTooltip.hide();
      nearestEntityIndex = null;
    });

  interaction.attachTo(combinedChart);

  function hideGuideline() {
    guideline.pixelPosition(-10);
  }
}
