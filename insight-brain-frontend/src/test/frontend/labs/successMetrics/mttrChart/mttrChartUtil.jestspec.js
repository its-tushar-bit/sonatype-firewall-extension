/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Component } from 'plottable';
import { makeMttrChart } from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/mttrChart/mttrChartUtils';

describe('mttrChartUtil', () => {
  const mttrs = [
    {
      timePeriodName: 'Sep',
      mttrInSeconds: 1309714,
      criticalMttrInSeconds: 129714,
    },
    {
      timePeriodName: 'Oct',
      mttrInSeconds: 1299714,
      criticalMttrInSeconds: 1299714,
    },
    {
      timePeriodName: 'Nov',
      mttrInSeconds: 1289714,
      criticalMttrInSeconds: 1209714,
    },
    {
      timePeriodName: 'Dec',
      mttrInSeconds: null,
      criticalMttrInSeconds: null,
    },
    {
      timePeriodName: 'Jan',
      mttrInSeconds: null,
      criticalMttrInSeconds: null,
    },
    {
      timePeriodName: 'Feb',
      mttrInSeconds: 384000,
      criticalMttrInSeconds: 384000,
    },
    {
      timePeriodName: 'Mar',
      mttrInSeconds: 384000,
      criticalMttrInSeconds: 384000,
    },
    {
      timePeriodName: 'Apr',
      mttrInSeconds: null,
      criticalMttrInSeconds: null,
    },
    {
      timePeriodName: 'May',
      mttrInSeconds: null,
      criticalMttrInSeconds: null,
    },
    {
      timePeriodName: 'Jun',
      mttrInSeconds: 1209714,
      criticalMttrInSeconds: 1209714,
    },
    {
      timePeriodName: 'Jul',
      mttrInSeconds: 484000,
      criticalMttrInSeconds: 484000,
    },
    {
      timePeriodName: 'Aug',
      mttrInSeconds: null,
      criticalMttrInSeconds: null,
    },
  ];

  it('makeMttrChart creates a Plottable Component', () => {
    const plot = makeMttrChart(mttrs);
    expect(plot).toEqual(expect.any(Component));
  });
});
