/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ViolationTrendsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationTrendsChart/ViolationTrendsChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('violationTrendsChart', () => {
  let violationCounts, component;

  beforeEach(() => {
    violationCounts = [
      {
        timePeriodName: 'Week of July 19th',
        discoveredCounts: {
          SECURITY: {
            LOW: 0,
            MODERATE: 5,
            SEVERE: 50,
            CRITICAL: 28,
          },
          LICENSE: {
            LOW: 2,
            MODERATE: 5,
            SEVERE: 3,
            CRITICAL: 2,
          },
          QUALITY: {
            LOW: 79,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          OTHER: {
            LOW: 1,
            MODERATE: 2,
            SEVERE: 0,
            CRITICAL: 2,
          },
        },
        waivedCounts: {
          SECURITY: {
            LOW: 0,
            MODERATE: 1,
            SEVERE: 7,
            CRITICAL: 0,
          },
          LICENSE: {
            LOW: 0,
            MODERATE: 0,
            SEVERE: 1,
            CRITICAL: 2,
          },
          QUALITY: {
            LOW: 3,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          OTHER: {
            LOW: 0,
            MODERATE: 2,
            SEVERE: 0,
            CRITICAL: 1,
          },
        },
        fixedCounts: {
          SECURITY: {
            LOW: 0,
            MODERATE: 3,
            SEVERE: 0,
            CRITICAL: 4,
          },
          LICENSE: {
            LOW: 0,
            MODERATE: 3,
            SEVERE: 0,
            CRITICAL: 2,
          },
          QUALITY: {
            LOW: 1,
            MODERATE: 0,
            SEVERE: 2,
            CRITICAL: 3,
          },
          OTHER: {
            LOW: 0,
            MODERATE: 4,
            SEVERE: 6,
            CRITICAL: 0,
          },
        },
      },
      {
        timePeriodName: 'Week of July 26th',
        discoveredCounts: {
          SECURITY: {
            LOW: 4,
            MODERATE: 2,
            SEVERE: 10,
            CRITICAL: 2,
          },
          LICENSE: {
            LOW: 2,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 7,
          },
          QUALITY: {
            LOW: 0,
            MODERATE: 4,
            SEVERE: 2,
            CRITICAL: 0,
          },
          OTHER: {
            LOW: 1,
            MODERATE: 0,
            SEVERE: 5,
            CRITICAL: 0,
          },
        },
        waivedCounts: {
          SECURITY: {
            LOW: 0,
            MODERATE: 2,
            SEVERE: 2,
            CRITICAL: 0,
          },
          LICENSE: {
            LOW: 6,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          QUALITY: {
            LOW: 0,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          OTHER: {
            LOW: 1,
            MODERATE: 0,
            SEVERE: 2,
            CRITICAL: 0,
          },
        },
        fixedCounts: {
          SECURITY: {
            LOW: 0,
            MODERATE: 3,
            SEVERE: 0,
            CRITICAL: 1,
          },
          LICENSE: {
            LOW: 0,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          QUALITY: {
            LOW: 0,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
          OTHER: {
            LOW: 0,
            MODERATE: 0,
            SEVERE: 0,
            CRITICAL: 0,
          },
        },
      },
    ];

    const getShallow = getShallowComponent(ViolationTrendsChart, { violationCounts });
    component = getShallow();
  });

  it('renders description', () => {
    const description = component.find('.nx-tile-header__subtitle');
    expect(description).toHaveText(`Violations and remediation over the past 2 weeks.`);
  });
  it('renders all chart containers', () => {
    expect(component.find('#iq-violation-trends-all')).toExist();
    expect(component.find('#iq-violation-trends-security')).toExist();
    expect(component.find('#iq-violation-trends-license')).toExist();
    expect(component.find('#iq-violation-trends-quality')).toExist();
    expect(component.find('#iq-violation-trends-other')).toExist();
  });
});
