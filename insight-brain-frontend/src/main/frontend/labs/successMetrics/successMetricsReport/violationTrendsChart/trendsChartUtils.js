/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Scales, Plots, Dataset } from 'plottable';
import { prop } from 'ramda';

export const GUIDELINE_TOOLTIP_OFFSET_VERTICAL = -25;
export const GUIDELINE_TOOLTIP_RIGHTMOST_PADDING = 30;
export const BAR_TOOLTIP_OFFSET_HORIZONTAL = 5;

export function moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip) {
  // set content before we get tooltip width
  tooltip.setContent(nearestEntity.datum.timePeriodName);
  const position = getGuidelineTooltipPosition(
    el,
    nearestEntity,
    tooltip.getWidth()
  );
  guideline.value(nearestEntity.datum.timePeriodIndex);
  tooltip.show(position.left, position.top);
}

export function moveBarTooltip(
  el,
  nearestEntity,
  tooltip,
  tooltipOffsetTop,
  showTrendArrow
) {
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

  tooltip.show(
    position.left,
    position.top,
    Math.abs(nearestEntity.datum.violations) + trendsIcon
  );
}

function getBarTooltipPosition(el, nearestEntity, tooltipOffsetTop) {
  return {
    left:
      el.offsetLeft + nearestEntity.position.x + BAR_TOOLTIP_OFFSET_HORIZONTAL,
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
