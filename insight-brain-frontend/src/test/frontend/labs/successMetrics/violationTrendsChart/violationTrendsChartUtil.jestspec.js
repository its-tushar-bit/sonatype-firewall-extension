/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Plots, Scales } from 'plottable';
import {
  generateBarPlot,
  generateGuidelinePlot,
  moveBarTooltip,
  moveGuidelineAndTooltip,
  GUIDELINE_TOOLTIP_RIGHTMOST_PADDING,
} from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationTrendsChart/ViolationTrendsChartUtils';

function generateDataset() {
  const week0Discovered = 10;
  const week1Discovered = 20;
  const week2Discovered = 4;

  const week0Waived = 1;
  const week1Waived = 7;
  const week2Waived = 5;

  const week0Fixed = 5;
  const week1Fixed = 10;
  const week2Fixed = 2;

  const week0Delta = week0Discovered - week0Waived - week0Fixed;
  const week1Delta = week1Discovered - week1Waived - week1Fixed;
  const week2Delta = week2Discovered - week2Waived - week2Fixed;

  return {
    discovered: [
      {
        timePeriodIndex: 0,
        timePeriodName: 'Week of Sep 10th',
        violations: week0Discovered,
      },
      {
        timePeriodIndex: 1,
        timePeriodName: 'Week of Sep 17th',
        violations: week1Discovered,
      },
      {
        timePeriodIndex: 2,
        timePeriodName: 'Week of Sep 24th',
        violations: week2Discovered,
      },
    ],
    waived: [
      {
        timePeriodIndex: 0,
        timePeriodName: 'Week of Sep 10th',
        violations: week0Waived,
      },
      {
        timePeriodIndex: 1,
        timePeriodName: 'Week of Sep 17th',
        violations: week1Waived,
      },
      {
        timePeriodIndex: 2,
        timePeriodName: 'Week of Sep 24th',
        violations: week2Waived,
      },
    ],
    fixed: [
      {
        timePeriodIndex: 0,
        timePeriodName: 'Week of Sep 10th',
        violations: week0Fixed,
      },
      {
        timePeriodIndex: 1,
        timePeriodName: 'Week of Sep 17th',
        violations: week1Fixed,
      },
      {
        timePeriodIndex: 2,
        timePeriodName: 'Week of Sep 24th',
        violations: week2Fixed,
      },
    ],
    delta: [
      {
        timePeriodIndex: 0,
        timePeriodName: 'Week of Sep 10th',
        violations: week0Delta,
      },
      {
        timePeriodIndex: 1,
        timePeriodName: 'Week of Sep 17th',
        violations: week1Delta,
      },
      {
        timePeriodIndex: 2,
        timePeriodName: 'Week of Sep 24th',
        violations: week2Delta,
      },
    ],
  };
}

describe('violationTrendsChartUtils', function () {
  describe('generateBarPlot', function () {
    it('creates Bar chart with passed data', function () {
      const plot = generateBarPlot(new Scales.Linear(), generateDataset(), 'foo', 10, -5);
      expect(plot).toEqual(expect.any(Plots.Bar));
    });

    it('creates Bar chart with passed data when min value is not provided', function () {
      const plot = generateBarPlot(new Scales.Linear(), generateDataset(), 'foo', 10);
      expect(plot).toEqual(expect.any(Plots.Bar));
    });
  });

  describe('generateGuidelinePlot', function () {
    it('creates Line chart with passed data', function () {
      const plot = generateGuidelinePlot(new Scales.Linear(), generateDataset());
      expect(plot).toEqual(expect.any(Plots.Line));
    });
  });

  describe('moveGuidelineAndTooltip', function () {
    const tooltipWidth = 10;
    const elementOffsetLeft = 234;
    const expectedTooltipLeft = 246;
    const expectedTooltipTop = 573;

    function calculateParentWidthForDesiredOverlap(offsetLeft, overlap) {
      return GUIDELINE_TOOLTIP_RIGHTMOST_PADDING + offsetLeft + tooltipWidth - overlap;
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

      tooltip = {
        setContent: jest.fn(),
        show: jest.fn(),
        getWidth: jest.fn(),
      };
      tooltip.getWidth.mockReturnValue(tooltipWidth);

      guideline = {
        value: jest.fn(),
      };
    });

    it('sets guideline value and shows tooltip with no position adjustment when overlap is 0', function () {
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(expectedTooltipLeft, 0);

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(expectedTooltipLeft, expectedTooltipTop);
    });

    it('sets guideline value and shows tooltip with no position adjustment when overlap is negative', function () {
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(expectedTooltipLeft, -1);

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(expectedTooltipLeft, expectedTooltipTop);
    });

    it('sets guideline value and shows tooltip with position adjustment when overlap is positive', function () {
      const overlap = 1;
      const tooltipLeftAfterAdjustment = expectedTooltipLeft - overlap;
      el.offsetParent.offsetWidth = calculateParentWidthForDesiredOverlap(expectedTooltipLeft, overlap);

      moveGuidelineAndTooltip(el, nearestEntity, guideline, tooltip);
      expect(guideline.value).toHaveBeenCalledWith(10);
      expect(tooltip.setContent).toHaveBeenCalledWith('Week of Sep 10th');
      expect(tooltip.show).toHaveBeenCalledWith(tooltipLeftAfterAdjustment, expectedTooltipTop);
    });
  });

  describe('moveBarTooltip', function () {
    let tooltip, nearestEntity, el;

    beforeEach(function () {
      tooltip = {
        show: jest.fn(),
      };

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
      expect(tooltip.show).toHaveBeenCalledWith(expectedTooltipLeft, el.offsetTop, '17');
    });

    it('shows bar tooltip in proper position with vertical offset and no trends icon', function () {
      const tooltipOffsetTop = 34;
      moveBarTooltip(el, nearestEntity, tooltip, tooltipOffsetTop);
      const expectedTooltipLeft = 244;
      const expectedTooltipTop = 632;
      expect(tooltip.show).toHaveBeenCalledWith(expectedTooltipLeft, expectedTooltipTop, '17');
    });

    it('shows bar tooltip in proper position with vertical offset and with trends icon', function () {
      const tooltipOffsetTop = 34;
      moveBarTooltip(el, nearestEntity, tooltip, tooltipOffsetTop, true);
      const expectedTooltipLeft = 244;
      const expectedTooltipTop = 632;
      expect(tooltip.show).toHaveBeenCalledWith(expectedTooltipLeft, expectedTooltipTop, expect.any(String));
      expect(tooltip.show.mock.calls[tooltip.show.mock.calls.length - 1][2]).toMatch('17<span');
    });
  });
});
