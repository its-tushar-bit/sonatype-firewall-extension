/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global describe, beforeEach, it, expect */
import { Plots, Scales } from 'plottable';
import generateDataset from './mockTrendsChartDataset';
import {
  generateBarPlot,
  generateGuidelinePlot,
  moveBarTooltip,
  moveGuidelineAndTooltip,
  GUIDELINE_TOOLTIP_RIGHTMOST_PADDING,
} from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationTrendsChart/trendsChartUtils';

describe('trendsChartUtils', function () {
  describe('generateBarPlot', function () {
    it('creates Bar chart with passed data', function () {
      const plot = generateBarPlot(
        new Scales.Linear(),
        generateDataset(),
        'foo',
        10,
        -5
      );
      expect(plot).toEqual(jasmine.any(Plots.Bar));
    });

    it('creates Bar chart with passed data when min value is not provided', function () {
      const plot = generateBarPlot(
        new Scales.Linear(),
        generateDataset(),
        'foo',
        10
      );
      expect(plot).toEqual(jasmine.any(Plots.Bar));
    });
  });

  describe('generateGuidelinePlot', function () {
    it('creates Line chart with passed data', function () {
      const plot = generateGuidelinePlot(
        new Scales.Linear(),
        generateDataset()
      );
      expect(plot).toEqual(jasmine.any(Plots.Line));
    });
  });

  describe('moveGuidelineAndTooltip', function () {
    const tooltipWidth = 10;
    const elementOffsetLeft = 234;
    const expectedTooltipLeft = 246;
    const expectedTooltipTop = 573;

    function calculateParentWidthForDesiredOverlap(offsetLeft, overlap) {
      return (
        GUIDELINE_TOOLTIP_RIGHTMOST_PADDING +
        offsetLeft +
        tooltipWidth -
        overlap
      );
    }

    let nearestEntity, el, tooltip, guideline;
    beforeEach(function () {
      nearestEntity = {
        position: { x: 17 },
        datum: {
          timePeriodName: 'Week of Sep 10th',
          timePeriodIndex: 10,
        },
      };

      el = {
        offsetLeft: elementOffsetLeft,
        offsetTop: 598,
        offsetParent: {},
      };

      tooltip = jasmine.createSpyObj('tooltip', [
        'setContent',
        'show',
        'getWidth',
      ]);
      tooltip.getWidth.and.returnValue(tooltipWidth);

      guideline = jasmine.createSpyObj('guideline', ['value']);
    });

    it('sets guideline value and shows tooltip with no position adjustment when overlap is 0', function () {
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(
        expectedTooltipLeft,
        0
      );

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(
        expectedTooltipLeft,
        expectedTooltipTop
      );
    });

    it('sets guideline value and shows tooltip with no position adjustment when overlap is negative', function () {
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(
        expectedTooltipLeft,
        -1
      );

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(
        expectedTooltipLeft,
        expectedTooltipTop
      );
    });

    it('sets guideline value and shows tooltip with position adjustment when overlap is positive', function () {
      const overlap = 1;
      const tooltipLeftAfterAdjustment = expectedTooltipLeft - overlap;
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(
        expectedTooltipLeft,
        overlap
      );

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(
        tooltipLeftAfterAdjustment,
        expectedTooltipTop
      );
    });
  });

  describe('moveBarTooltip', function () {
    let tooltip, nearestEntity, el;

    beforeEach(function () {
      tooltip = jasmine.createSpyObj('tooltip', ['show']);

      nearestEntity = {
        position: { x: 5 },
        datum: {
          violations: 17,
        },
      };

      el = {
        offsetLeft: 234,
        offsetTop: 598,
      };
    });

    it('shows bar tooltip in proper position with no vertical offset and no trends icon', function () {
      moveBarTooltip(el, nearestEntity, tooltip);
      const expectedTooltipLeft = 244;
      expect(tooltip.show).toHaveBeenCalledWith(
        expectedTooltipLeft,
        el.offsetTop,
        '17'
      );
    });

    it('shows bar tooltip in proper position with vertical offset and no trends icon', function () {
      const tooltipOffsetTop = 34;
      moveBarTooltip(el, nearestEntity, tooltip, tooltipOffsetTop);
      const expectedTooltipLeft = 244;
      const expectedTooltipTop = 632;
      expect(tooltip.show).toHaveBeenCalledWith(
        expectedTooltipLeft,
        expectedTooltipTop,
        '17'
      );
    });

    it('shows bar tooltip in proper position with vertical offset and with trends icon', function () {
      const tooltipOffsetTop = 34;
      moveBarTooltip(el, nearestEntity, tooltip, tooltipOffsetTop, true);
      const expectedTooltipLeft = 244;
      const expectedTooltipTop = 632;
      expect(tooltip.show).toHaveBeenCalledWith(
        expectedTooltipLeft,
        expectedTooltipTop,
        jasmine.any(String)
      );
      expect(tooltip.show.calls.mostRecent().args[2]).toMatch('17<i');
    });
  });
});
