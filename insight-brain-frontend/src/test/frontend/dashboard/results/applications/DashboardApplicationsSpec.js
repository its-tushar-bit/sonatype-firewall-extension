/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import DashboardApplications from 'MainRoot/dashboard/results/applications/DashboardApplications';
import DashboardApplicationsTable from 'MainRoot//dashboard/results/applications/DashboardApplicationsTable';
import DashboardMask from 'MainRoot/dashboard/results/dashboardMask/DashboardMask';

describe('DashboardApplications', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      loadApplicationResults: jasmine.createSpy('loadApplicationResults'),
      sortApplications: jasmine.createSpy('sortApplications'),
      applicationResults: {
        results: [
          { applicationId: 'app1', totalApplicationRisk: {}, stageRisks: [] },
          { applicationId: 'app2', totalApplicationRisk: {}, stageRisks: [] },
        ],
        sortFields: ['-totalApplicationRisk.totalRisk'],
        pageCount: 1,
        page: 0,
      },
      setNextApplicationsPage: () => {},
      setPreviousApplicationsPage: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardApplications, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardApplications, minimalProps);
  });

  it('renders a DashboardApplicationsTable with the appropriate props', function () {
    const dashboardApplicationsProps = {
      applicationResults: {
        results: [{ applicationId: 'app1' }, { applicationId: 'app2' }],
        hasNextPage: true,
        classyBrew: {
          isWhiteText: () => true,
          getColor: () => 'test-background-color',
        },
        pageCount: 1,
        page: 0,
      },
      needsAcknowledgement: true,
    };

    const dashBoardApplications = getShallowComponent(dashboardApplicationsProps),
      table = dashBoardApplications.find(DashboardApplicationsTable);

    expect(table).toExist();
    expect(table).toHaveProp('applicationResults', {
      results: [{ applicationId: 'app1' }, { applicationId: 'app2' }],
      hasNextPage: true,
      classyBrew: jasmine.any(Object),
      pageCount: 1,
      page: 0,
    });
    expect(table).toHaveProp('needsAcknowledgement', true);
    expect(table).toHaveProp('reload', jasmine.any(Function));
    expect(table).toHaveProp('colorStyler', {
      isWhiteText: jasmine.any(Function),
      getColor: jasmine.any(Function),
    });
    expect(table).toHaveProp('sortApplications', jasmine.any(Function));
    expect(table).toHaveProp('setNextApplicationsPage');
    expect(table).toHaveProp('setPreviousApplicationsPage');
    table.prop('reload')();
    expect(minimalProps.loadApplicationResults).toHaveBeenCalled();

    table.prop('sortApplications')();
    expect(minimalProps.sortApplications).toHaveBeenCalled();
  });

  it('renders a mask over the table when filters are dirty', function () {
    const dashboardApplicationsProps = {
      filtersAreDirty: true,
    };

    const dashBoardApplications = getShallowComponent(dashboardApplicationsProps),
      mask = dashBoardApplications.find(DashboardMask);
    expect(mask).toExist();
  });

  it('does not render a form mask if filters are dirty and needs acknowledgement', () => {
    const dashBoardApplications = getShallowComponent({ filtersAreDirty: true, needsAcknowledgement: true });
    expect(dashBoardApplications.find(DashboardMask)).not.toExist();
  });

  it('does not render a mask over the table when filters are dirty but there are no results', () => {
    const dashBoardApplications = getShallowComponent({
      filtersAreDirty: true,
      applicationResults: {
        results: null,
        pageCount: 0,
        page: null,
      },
    });
    expect(dashBoardApplications.find(DashboardMask)).not.toExist();
  });

  it('renders a mask over the table when there are no results but there is an error', () => {
    const dashBoardApplications = getShallowComponent({
      filtersAreDirty: true,
      applicationResults: {
        results: null,
        error: 'error',
        pageCount: 0,
        page: null,
      },
    });
    expect(dashBoardApplications.find(DashboardMask)).toExist();
  });

  it('loads applications results on render if the filter is not loading and does not need acknowledgment', function () {
    const dashboardApplicationsProps = {
      filterLoading: false,
      needsAcknowledgement: false,
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadApplicationResults).toHaveBeenCalledTimes(1);
  });

  it('Does not load applications results on render if the filter is loading', function () {
    const dashboardApplicationsProps = {
      filterLoading: true,
      needsAcknowledgement: false,
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadApplicationResults).not.toHaveBeenCalled();
  });

  it('Does not load applications results on render if the filter needs acknowledgment', function () {
    const dashboardApplicationsProps = {
      filterLoading: false,
      needsAcknowledgement: true,
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadApplicationResults).not.toHaveBeenCalled();
  });
});
