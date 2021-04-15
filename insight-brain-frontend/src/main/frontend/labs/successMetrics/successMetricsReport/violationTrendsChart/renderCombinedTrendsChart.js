/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Scales, Components, Interactions } from 'plottable';
import $ from 'jquery';
import TrendsTooltip from './trendsTooltip';
import {
  generateBarPlot,
  generateGuidelinePlot,
  moveBarTooltip,
  moveGuidelineAndTooltip,
} from './trendsChartUtils';

const CHART_HEIGHT = 31;
const CHART_PADDING = 24;
const ROW_HEIGHT = CHART_PADDING * 2 + CHART_HEIGHT;

/**
 * This directive is used to render each of the 5 charts (represents one column in the overall chart).
 */
export default function renderCombinedTrendsChartDirective() {
  return {
    scope: {
      data: '<',
      statistics: '<',
    },
    link: function (scope, el) {
      renderCombinedTrendsChart(el[0], scope.data, scope.statistics);
    },
  };
}

function renderCombinedTrendsChart(
  el,
  { delta, discovered, waived, fixed },
  statistics
) {
  const xScale = new Scales.Linear().padProportion(0);

  const deltaBarClass = (d) =>
    `iq-violation-trends__bar--delta-${d.violations > 0 ? 'up' : 'down'}`;

  const deltaBar = generateBarPlot(
    xScale,
    delta,
    deltaBarClass,
    statistics.deltaMax,
    statistics.deltaMin
  );
  const newBar = generateBarPlot(
    xScale,
    discovered,
    'iq-violation-trends__bar--discovered',
    statistics.discoveredMax
  );
  const waivedBar = generateBarPlot(
    xScale,
    waived,
    'iq-violation-trends__bar--waived',
    statistics.waivedMax
  );
  const fixedBar = generateBarPlot(
    xScale,
    fixed,
    'iq-violation-trends__bar--fixed',
    statistics.fixedMax
  );
  const guidelineChart = generateGuidelinePlot(xScale, discovered);
  const guideline = new Components.GuideLineLayer(
    Components.GuideLineLayer.ORIENTATION_VERTICAL
  )
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
  const deltaBarTooltip = TrendsTooltip(
    'deltaBarTooltip',
    violationTrendsElement
  );
  const newBarTooltip = TrendsTooltip('newBarTooltip', violationTrendsElement);
  const waivedBarTooltip = TrendsTooltip(
    'waivedBarTooltip',
    violationTrendsElement
  );
  const fixedBarTooltip = TrendsTooltip(
    'fixedBarTooltip',
    violationTrendsElement
  );
  const guidelineTooltip = TrendsTooltip(
    'guidelineTooltip',
    violationTrendsElement
  );
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
      moveBarTooltip(
        el,
        deltaBar.entities()[nearestEntityIndex],
        deltaBarTooltip,
        0,
        true
      );
      moveBarTooltip(
        el,
        newBar.entities()[nearestEntityIndex],
        newBarTooltip,
        ROW_HEIGHT
      );
      moveBarTooltip(
        el,
        waivedBar.entities()[nearestEntityIndex],
        waivedBarTooltip,
        ROW_HEIGHT * 2
      );
      moveBarTooltip(
        el,
        fixedBar.entities()[nearestEntityIndex],
        fixedBarTooltip,
        ROW_HEIGHT * 3
      );
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
