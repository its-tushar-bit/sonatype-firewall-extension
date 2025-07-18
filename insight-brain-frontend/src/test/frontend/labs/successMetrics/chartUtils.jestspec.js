/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Scales, Axes, Component } from 'plottable';
import * as chartUtils from '../../../../main/frontend/labs/successMetrics/chartUtils';

describe('chartUtils', function () {
  describe('calculateTickInterval', function () {
    it('does not round tickInterval if it is less then 1', function () {
      expect(chartUtils.calculateTickInterval(2)).toBe(0.5);
    });

    it('rounds tickInterval to multiples of 1 if between 1 and 5', function () {
      expect(chartUtils.calculateTickInterval(13)).toBe(3);
    });

    it('rounds tickInterval to multiples of 5 if more then 5', function () {
      expect(chartUtils.calculateTickInterval(44)).toBe(10);
    });

    it('returns 1 if maxValue is 0', function () {
      expect(chartUtils.calculateTickInterval(0)).toBe(1);
    });

    it('returns .25 if maxValue is less than or equal to 1 and 4 ticks requested', function () {
      expect(chartUtils.calculateTickInterval(1)).toBe(0.25);
    });
  });

  describe('createScatterPlotChart', function () {
    it('returns a Plottable Component when given a category scale and axis', function () {
      const xAccessor = (x) => x,
        xScale = new Scales.Category(),
        xAxis = new Axes.Category(xScale, 'bottom'),
        xAxisLabelText = 'foo',
        yAxisLabelText = 'bar',
        lineConfigs = [
          {
            name: 'Qwerty',
            yAccessor: () => 1,
            className: 'qwerty',
          },
          {
            name: 'Asdf',
            yAccessor: () => 2,
            className: 'asdf',
          },
        ],
        legendOrder = ['Asdf', 'Qwerty'],
        data = [{}, {}],
        result = chartUtils.createScatterPlotChart(
          xAccessor,
          xScale,
          xAxis,
          xAxisLabelText,
          yAxisLabelText,
          lineConfigs,
          legendOrder,
          data
        );

      expect(result).toEqual(expect.any(Component));
    });

    it('returns a Plottable Component when given a linear scale and numeric axis', function () {
      const xAccessor = (x) => x,
        xScale = new Scales.Linear().domain([0, 1]),
        xAxis = new Axes.Numeric(xScale, 'bottom'),
        xAxisLabelText = 'foo',
        yAxisLabelText = 'bar',
        lineConfigs = [
          {
            name: 'Qwerty',
            yAccessor: () => 1,
            className: 'qwerty',
          },
          {
            name: 'Asdf',
            yAccessor: () => 2,
            className: 'asdf',
          },
        ],
        legendOrder = ['Asdf', 'Qwerty'],
        data = [{}, {}],
        result = chartUtils.createScatterPlotChart(
          xAccessor,
          xScale,
          xAxis,
          xAxisLabelText,
          yAxisLabelText,
          lineConfigs,
          legendOrder,
          data
        );

      expect(result).toEqual(expect.any(Component));
    });
  });

  describe('getMaxAccessedValue', function () {
    it('returns the greatest value computed by any of the accessor functions over any of the data points', function () {
      const dataset = [
          {
            foo: 1,
            bar: 2,
          },
          {
            foo: 4,
            bar: 1,
          },
          {
            foo: 0,
            bar: 0,
          },
          {},
        ],
        accessors = [(x) => x.foo, (x) => x.bar];

      expect(chartUtils.getMaxAccessedValue(dataset, accessors)).toBe(4);
    });

    it('returns 0 if there is no data', function () {
      const dataset = [],
        accessors = [(x) => x.foo, (x) => x.bar];

      expect(chartUtils.getMaxAccessedValue(dataset, accessors)).toBe(0);
    });

    it('returns 0 if there are no rows for which the accessors return numbers', function () {
      const dataset = [
          {
            foo: 'asdf',
          },
        ],
        accessors = [(x) => x.foo, (x) => x.bar];

      expect(chartUtils.getMaxAccessedValue(dataset, accessors)).toBe(0);
    });

    it('returns 0 if there are no accessors', function () {
      const dataset = [
          {
            foo: 1,
            bar: 2,
          },
          {
            foo: 4,
            bar: 1,
          },
          {
            foo: 0,
            bar: 0,
          },
          {},
        ],
        accessors = [];

      expect(chartUtils.getMaxAccessedValue(dataset, accessors)).toBe(0);
    });
  });
});
