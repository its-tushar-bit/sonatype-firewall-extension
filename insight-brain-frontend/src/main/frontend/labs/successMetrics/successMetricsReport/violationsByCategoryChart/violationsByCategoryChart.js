/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {defaultTo, filter, isEmpty, not, pipe, prop, props, sum, without} from 'ramda';
import Plottable from 'plottable';

import { createScatterPlotChart } from '../../chartUtils';
import template from './violationsByCategoryChart.html';

const violationsByCategoryChart = {
  template,
  controller: ViolationsByCategoryChartController,
  controllerAs: 'vm',
  bindings: {
    violationsByCategoryData: '<'
  }
};

export default violationsByCategoryChart;

function ViolationsByCategoryChartController() {
  const vm = this,
      dataset = vm.violationsByCategoryData,
      notNullFilter = without([null]),
      getNotNullValues = pipe(props(['security', 'license', 'quality', 'other']), notNullFilter),
      weekCount = filter(pipe(getNotNullValues, isEmpty, not), dataset).length,
      xAccessor = prop('timePeriodName'),

      xScale = new Plottable.Scales.Category(),
      xAxis = new Plottable.Axes.Category(xScale, 'bottom'),

      yAxisLabelText = 'Policy Violations',

      lineConfigs = [{
        name: 'Total',
        yAccessor: function(entry) {
          const notNullValues = getNotNullValues(entry);
          // for null values return undefined to display graph breaks
          return isEmpty(notNullValues) ? undefined : sum(notNullValues);
        },
        className: 'iq-chart__dataset--overall'
      }, {
        name: 'Security',
        yAccessor: pipe(prop('security'), defaultTo(undefined)),
        className: 'iq-chart__dataset--security'
      }, {
        name: 'License',
        yAccessor: pipe(prop('license'), defaultTo(undefined)),
        className: 'iq-chart__dataset--license'
      }, {
        name: 'Quality',
        yAccessor: pipe(prop('quality'), defaultTo(undefined)),
        className: 'iq-chart__dataset--quality'
      }, {
        name: 'Other',
        yAccessor: pipe(prop('other'), defaultTo(undefined)),
        className: 'iq-chart__dataset--other'
      }],

      chart = createScatterPlotChart(xAccessor, xScale, xAxis, null, yAxisLabelText, lineConfigs, dataset);

  Object.assign(vm, {
    violationsByCategoryChart: chart,
    weekCount
  });
}
