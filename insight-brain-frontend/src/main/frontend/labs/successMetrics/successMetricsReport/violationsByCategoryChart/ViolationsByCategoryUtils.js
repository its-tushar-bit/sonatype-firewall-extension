/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { defaultTo, filter, isEmpty, not, pipe, prop, props, sum, without } from 'ramda';
import { Axes, Scales } from 'plottable';
import { createScatterPlotChart } from '../../chartUtils';

const notNullFilter = without([null]);

const getNotNullValues = pipe(props(['security', 'license', 'quality', 'other']), notNullFilter);

export const computeWeekCount = (dataset) => filter(pipe(getNotNullValues, isEmpty, not), dataset).length;

export const generateChart = (dataset) => {
  const xAccessor = prop('timePeriodName');
  const xScale = new Scales.Category();
  const xAxis = new Axes.Category(xScale, 'bottom');
  const yAxisLabelText = 'Policy Violations';
  const lineConfigs = [
    {
      name: 'Total',
      yAccessor: function (entry) {
        const notNullValues = getNotNullValues(entry);
        // for null values return undefined to display graph breaks
        return isEmpty(notNullValues) ? undefined : sum(notNullValues);
      },
      className: 'iq-chart__dataset--overall',
    },
    {
      name: 'Security',
      yAccessor: pipe(prop('security'), defaultTo(undefined)),
      className: 'iq-chart__dataset--security',
    },
    {
      name: 'License',
      yAccessor: pipe(prop('license'), defaultTo(undefined)),
      className: 'iq-chart__dataset--license',
    },
    {
      name: 'Quality',
      yAccessor: pipe(prop('quality'), defaultTo(undefined)),
      className: 'iq-chart__dataset--quality',
    },
    {
      name: 'Other',
      yAccessor: pipe(prop('other'), defaultTo(undefined)),
      className: 'iq-chart__dataset--other',
    },
  ];
  return createScatterPlotChart(xAccessor, xScale, xAxis, null, yAxisLabelText, lineConfigs, dataset);
};
