/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Plots } from 'plottable';
import {
  makeChart,
  showRow,
} from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/componentCountsChart/componentCountsChartUtils';

describe('componentCountsChartUtil', () => {
  const componentCounts = {
    componentsPerApplication: 32,
    componentsInTheMostApplications: [
      { componentDisplayName: 'SimpleJson 0.38.0', count: 1 },
      {
        componentDisplayName: 'ch.qos.logback : logback-access : 0.6',
        count: 1,
      },
      {
        componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3',
        count: 1,
      },
      { componentDisplayName: 'commons-dbcp : commons-dbcp : 1.4', count: 2 },
      {
        componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
        count: 2,
      },
    ],
    componentsWithTheMostViolations: [
      {
        componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
        count: 1,
      },
      {
        componentDisplayName: 'org.apache.geronimo.framework : geronimo-security : 2.1',
        count: 1,
      },
      {
        componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15',
        count: 1,
      },
      {
        componentDisplayName: 'tomcat : catalina-host-manager : 5.5.23',
        count: 2,
      },
      { componentDisplayName: 'tomcat : tomcat-util : 5.5.23', count: 2 },
    ],
  };

  it('showRow checks if row is empty', () => {
    expect(showRow('~empty~')).toBe(false);
    expect(showRow('123~empty~')).toBe(false);
    expect(showRow('~empty~123')).toBe(false);
    expect(showRow('1~empty~23')).toBe(false);
    expect(showRow('~1em2pty3~')).toBe(true);
    expect(showRow('123')).toBe(true);
  });

  it('makeChart creates a bar chart with componentsInTheMostApplications data', () => {
    const plot = makeChart(componentCounts, 'componentsInTheMostApplications', 'iq-chart__dataset--component');
    expect(plot).toEqual(expect.any(Plots.Bar));
  });

  it('makeChart renders componentsWithTheMostViolations chart', () => {
    const plot = makeChart(componentCounts, 'componentsWithTheMostViolations', 'iq-chart__dataset--critical');
    expect(plot).toEqual(expect.any(Plots.Bar));
  });
});
