/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment';
import ApplicationCountsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/applicationCountsChart/ApplicationCountsChart';
import ComponentCountsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/componentCountsChart/ComponentCountsChart';
import MttrChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/mttrChart/MttrChart';
import SuccessMetricsReport from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/SuccessMetricsReport';
import ViolationAveragesChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationAveragesChart/ViolationAveragesChart';
import ViolationsByCategoryChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationsByCategoryChart/ViolationsByCategoryChart';
import ViolationTrendsChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationTrendsChart/ViolationTrendsChart';
import * as enzymeUtils from '../../../enzymeUtils';

describe('successMetricsReport', () => {
  describe('when has no data to show', () => {
    let getShallow;

    beforeEach(() => {
      getShallow = enzymeUtils.getShallowComponent(SuccessMetricsReport, {
        applicationCounts: {},
        router: {
          currentParams: {},
        },
      });
    });

    it('and most recent evaluations is enabled, renders no data message', () => {
      const component = getShallow({ includeLatestData: false });
      const description = component.find('.nx-page-title__description h3');

      expect(description).toHaveText(
        ` There's not enough data to generate Success Metrics. Run some evaluations and check again next month. Create a Success Metrics report using the 'include most recent evaluations' option to see the latest data.`
      );
    });
    it('and most recent evaluations is disabled, renders no data message', () => {
      const component = getShallow({ includeLatestData: true });
      const description = component.find('.nx-page-title__description h3');

      expect(description).toHaveText(
        ` There's not enough data to generate Success Metrics. Run some evaluations and check again.`
      );
    });
    it('does not render any chart', () => {
      const component = getShallow();

      const violationTrendsChart = component.find(ViolationTrendsChart);
      const violationsByCategoryChart = component.find(ViolationsByCategoryChart);
      const violationAveragesChart = component.find(ViolationAveragesChart);
      const mttrChart = component.find(MttrChart);
      const applicationCountsChart = component.find(ApplicationCountsChart);
      const componentCountsChart = component.find(ComponentCountsChart);

      expect(violationTrendsChart).not.toExist();
      expect(violationsByCategoryChart).not.toExist();
      expect(violationAveragesChart).not.toExist();
      expect(mttrChart).not.toExist();
      expect(applicationCountsChart).not.toExist();
      expect(componentCountsChart).not.toExist();
    });
  });

  describe('when has data to show', () => {
    let getShallow, lastUpdated, isSingleApplicationReport, monthCount, applicationCounts;

    beforeAll(() => {
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
      lastUpdated = 1507218887089;
      isSingleApplicationReport = false;
      monthCount = 3;

      getShallow = enzymeUtils.getShallowComponent(SuccessMetricsReport, {
        applicationCounts,
        isSingleApplicationReport,
        monthCount,
        lastUpdated,
        router: {
          currentParams: {},
        },
      });
    });

    it('renders description with correct data when it is full calendar report', () => {
      const component = getShallow({ includeLatestData: false });
      const description = component.find('.nx-page-title__description h3');
      expect(description).toHaveText(
        `
              This report contains data for
              ${applicationCounts.activeApplications}
              application${isSingleApplicationReport ? '' : 's'}, evaluated over the past
              ${monthCount} ${monthCount === 1 ? 'month' : 'months'},
              aggregated and deduplicated over the source, build, stage release, release, and operate stages. Last
              updated
              ${moment(lastUpdated).format('MMM DD, YYYY')}.
            `
      );
    });
    it('renders description with correct data when report include recent evaluations', () => {
      const component = getShallow({ includeLatestData: true });
      const description = component.find('.nx-page-title__description h3');
      expect(description).toHaveText(
        `
              This report contains data for
              ${applicationCounts.activeApplications}
              application${isSingleApplicationReport ? '' : 's'}, evaluated over the past
              ${monthCount} ${monthCount === 1 ? 'month' : 'months'},
              aggregated and deduplicated over the source, build, stage release, release, and operate stages. Last
              updated
              ${moment(lastUpdated).format('MMM DD, YYYY hh:mm:ss A')}.
            `
      );
    });
    it('renders all charts', () => {
      const component = getShallow();

      const violationTrendsChart = component.find(ViolationTrendsChart);
      const violationsByCategoryChart = component.find(ViolationsByCategoryChart);
      const violationAveragesChart = component.find(ViolationAveragesChart);
      const mttrChart = component.find(MttrChart);
      const applicationCountsChart = component.find(ApplicationCountsChart);
      const componentCountsChart = component.find(ComponentCountsChart);

      expect(violationTrendsChart).toExist();
      expect(violationsByCategoryChart).toExist();
      expect(violationAveragesChart).toExist();
      expect(mttrChart).toExist();
      expect(applicationCountsChart).toExist();
      expect(componentCountsChart).toExist();
    });
  });

  describe('delete button', () => {
    let getShallow;

    beforeEach(() => {
      getShallow = enzymeUtils.getShallowComponent(SuccessMetricsReport, {
        applicationCounts: {},
        router: {
          currentParams: {},
        },
      });
    });

    it('is rendered', () => {
      const component = getShallow();
      expect(component.find('#delete-report-button')).toExist();
    });

    it('shows delete modal when clicked', () => {
      const component = getShallow();
      const deleteButton = component.find('#delete-report-button');
      deleteButton.simulate('click');
      expect(component.find('#delete-modal')).toExist();
    });
  });
});
