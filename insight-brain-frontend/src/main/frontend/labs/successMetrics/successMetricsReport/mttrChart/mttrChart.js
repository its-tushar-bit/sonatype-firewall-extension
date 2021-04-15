/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Axes, Scales } from 'plottable';
import template from './mttrChart.html';

import { createScatterPlotChart } from '../../chartUtils';

const SECONDS_IN_DAY = 24 * 60 * 60;

const mttrChart = {
  template,
  controller,
  controllerAs: 'vm',
  bindings: {
    mttrData: '<',
    activeApplicationCount: '<',
    monthCount: '<',
  },
};

export default mttrChart;

function controller() {
  const vm = this;

  vm.mttrChart = makeMttrChart(vm.mttrData);

  function makeMttrChart(dataset) {
    const xScale = new Scales.Category();

    const lineConfigs = [
      {
        name: 'Critical',
        yAccessor: getYAccessor('criticalMttrInSeconds'),
        className: 'iq-chart__dataset--critical',
      },
      {
        name: 'All',
        yAccessor: getYAccessor('mttrInSeconds'),
        className: 'iq-chart__dataset--overall',
      },
    ];

    function getYAccessor(key) {
      return function (d) {
        // expected data in seconds; use undefined for nulls to display graph breaks
        return d[key] === null ? undefined : d[key] / SECONDS_IN_DAY;
      };
    }

    function xAccessor(d) {
      return d.timePeriodName;
    }

    const xAxis = new Axes.Category(xScale, 'bottom');
    const yAxisLabelText = 'Days to Resolve';

    return createScatterPlotChart(
      xAccessor,
      xScale,
      xAxis,
      null,
      yAxisLabelText,
      lineConfigs,
      dataset
    );
  }
}
