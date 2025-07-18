/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Plots } from 'plottable';
import { makeChart } from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/applicationCountsChart/applicationCountsChartUtils';

describe('applicationCountsChart', () => {
  const applicationCounts = {
    totalApplications: 5,
    activeApplications: 4,
    total: {
      applicationsWithViolations: 3,
      applicationsWithCriticalViolations: 2,
    },
    security: {
      applicationsWithViolations: 2,
      applicationsWithCriticalViolations: 2,
    },
    license: {
      applicationsWithViolations: 1,
      applicationsWithCriticalViolations: 1,
    },
    quality: {
      applicationsWithViolations: 1,
      applicationsWithCriticalViolations: 0,
    },
    other: {
      applicationsWithViolations: 0,
      applicationsWithCriticalViolations: 0,
    },
  };

  it('makeChart creates a bar chart', () => {
    const plot = makeChart(applicationCounts);
    expect(plot).toEqual(expect.any(Plots.Bar));
  });
});
