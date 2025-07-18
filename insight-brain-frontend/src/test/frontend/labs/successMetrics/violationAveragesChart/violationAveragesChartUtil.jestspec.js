/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Plots } from 'plottable';
import { makeChart } from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationAveragesChart/violationAveragesChartUtils';

describe('violationAveragesChartUtil', () => {
  const averages = {
    evaluationCount: 3,
    securityViolations: {
      averageDiscovered: 1,
      averageDiscoveredCritical: 1,
    },
    licenseViolations: {
      averageDiscovered: 12,
      averageDiscoveredCritical: 8,
    },
    qualityViolations: {
      averageDiscovered: 6,
      averageDiscoveredCritical: 2,
    },
    otherViolations: {
      averageDiscovered: 12,
      averageDiscoveredCritical: 11,
    },
    totalViolations: {
      averageDiscovered: 31,
      averageDiscoveredCritical: 22,
    },
  };

  it('makeChart creates a bar chart', () => {
    const plot = makeChart(averages);
    expect(plot).toEqual(expect.any(Plots.Bar));
  });
});
