/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ViolationAveragesChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationAveragesChart/ViolationAveragesChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('violationAveragesChart', () => {
  let averages, isSingleApplicationReport, activeApplicationCount, monthCount, component;

  beforeEach(() => {
    isSingleApplicationReport = false;
    activeApplicationCount = 7;
    monthCount = 3;
    averages = {
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

    const getShallow = getShallowComponent(ViolationAveragesChart, {
      averages,
      isSingleApplicationReport,
      activeApplicationCount,
      monthCount,
    });
    component = getShallow();
  });

  it('renders description', () => {
    const averageEvaluationsRounded = Math.round(averages.evaluationCount);
    const averageDiscoveredTotal = averages.totalViolations.averageDiscovered;
    const averageDiscoveredTotalCritical = averages.totalViolations.averageDiscoveredCritical;

    const description = component.find('.nx-tile-header__subtitle');
    expect(description).toHaveText(
      `Lifecycle performed an average of ${averageEvaluationsRounded} evaluation${
        averageEvaluationsRounded === 1 ? '' : 's'
      } per month on ${activeApplicationCount} application${
        isSingleApplicationReport ? '' : 's'
      } over the past ${monthCount} ${
        monthCount === 1 ? 'month' : 'months'
      }. Lifecycle found an average of ${averageDiscoveredTotal.toFixed(0)} policy violations${
        isSingleApplicationReport ? ',' : ' per application,'
      } ${averageDiscoveredTotalCritical.toFixed(0)} of which were critical.`
    );
  });
  it('renders chart container', () => {
    expect(component.find('#violation-averages-chart')).toExist();
  });
});
