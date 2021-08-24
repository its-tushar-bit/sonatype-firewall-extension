/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ApplicationCountsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/applicationCountsChart/ApplicationCountsChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('applicationCountsChart', () => {
  let monthCount, applicationCounts, component;

  beforeEach(() => {
    monthCount = 3;
    applicationCounts = {
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

    const getShallow = getShallowComponent(ApplicationCountsChart, { monthCount, applicationCounts });
    component = getShallow();
  });

  it('renders description', () => {
    const description = component.find('.nx-tile-header__subtitle');
    expect(description).toHaveText(`Over the past ${monthCount} ${monthCount === 1 ? 'month' : 'months'},
    ${applicationCounts.total.applicationsWithViolations}
    out of ${applicationCounts.activeApplications} applications contained violations,
    and ${applicationCounts.total.applicationsWithCriticalViolations} contained
    critical violations.`);
  });
  it('renders chart container', () => {
    expect(component.find('#application-count-chart')).toExist();
  });
});
