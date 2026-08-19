/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Axes, Scales } from 'plottable';
import { complement, concat, drop, dropWhile, equals, map, objOf, takeWhile } from 'ramda';
import { createScatterPlotChart } from '../../chartUtils';

const SECONDS_IN_DAY = 24 * 60 * 60;
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export const getMttrData = (data) => {
  const monthsOfMttr = data.length;

  if (monthsOfMttr === 0) {
    return data;
  } else {
    if (monthsOfMttr < 12) {
      const firstMonthName = data[0].timePeriodName,
        // function which matches months which are not the first month from the data
        isNotFirstMonthOfData = complement(equals(firstMonthName)),
        // previous months that come before firstMonthName in the year
        previousMonthsInYear = takeWhile(isNotFirstMonthOfData, MONTHS),
        // additional months from the previous year for a total of a full year
        additionalMonthsInPreviousYear = dropWhile(isNotFirstMonthOfData, MONTHS),
        yearOfMonthsBeforeData = concat(additionalMonthsInPreviousYear, previousMonthsInYear),
        // months that we need to add to the data to get a year overall
        missingMonths = drop(monthsOfMttr, yearOfMonthsBeforeData),
        missingRecords = map(objOf('timePeriodName'), missingMonths);

      return concat(missingRecords, data);
    }
    return data;
  }
};

export function makeMttrChart(dataset) {
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
  return createScatterPlotChart(xAccessor, xScale, xAxis, null, yAxisLabelText, lineConfigs, dataset);
}
