/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import DashboardApplications from '../../../../../main/frontend/dashboard/results/applications/DashboardApplications';
import DashboardApplicationsTable from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTable';
import DashboardMask from '../../../../../main/frontend/dashboard/results/dashboardMask/DashboardMask';

describe('DashboardApplications', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      loadResults: jasmine.createSpy('loadResults'),
      sortResults: jasmine.createSpy('sortResults'),
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      DashboardApplications,
      minimalProps
    );
    getMountedComponent = enzymeUtils.getMountedComponent(
      DashboardApplications,
      minimalProps
    );
  });

  it('renders a DashboardApplicationsTable with the appropriate props', function () {
    const dashboardApplicationsProps = {
      applicationResults: {
        results: [{ applicationId: 'app1' }, { applicationId: 'app2' }],
        classyBrew: {
          isWhiteText: () => true,
          getColor: () => 'test-background-color',
        },
      },
      needsAcknowledgement: true,
    };

    const dashBoardApplications = getShallowComponent(
        dashboardApplicationsProps
      ),
      table = dashBoardApplications.find(DashboardApplicationsTable);

    expect(table).toExist();
    expect(table).toHaveProp('applicationResults', {
      results: [{ applicationId: 'app1' }, { applicationId: 'app2' }],
      classyBrew: jasmine.any(Object),
    });
    expect(table).toHaveProp('needsAcknowledgement', true);
    expect(table).toHaveProp('reload', jasmine.any(Function));
    expect(table).toHaveProp('colorStyler', {
      isWhiteText: jasmine.any(Function),
      getColor: jasmine.any(Function),
    });
    expect(table).toHaveProp('sortApplications', jasmine.any(Function));

    table.prop('reload')();
    expect(minimalProps.loadResults).toHaveBeenCalledWith('applications');

    table.prop('sortApplications')();
    expect(minimalProps.sortResults).toHaveBeenCalledWith('applications');
  });

  it('renders a mask over the table when filters are dirty', function () {
    const dashboardApplicationsProps = {
      filtersAreDirty: true,
    };

    const dashBoardApplications = getShallowComponent(
        dashboardApplicationsProps
      ),
      mask = dashBoardApplications.find(DashboardMask);
    expect(mask).toExist();
  });

  it('loads applications results on render if the filter is not loading and does not need acknowledgment', function () {
    const dashboardApplicationsProps = {
      filterLoading: false,
      needsAcknowledgement: false,
      applicationResults: {
        results: [
          { applicationId: 'app1', totalApplicationRisk: {}, stageRisks: [] },
          { applicationId: 'app2', totalApplicationRisk: {}, stageRisks: [] },
        ],
        sortFields: ['-totalApplicationRisk.totalRisk'],
      },
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadResults).toHaveBeenCalledWith('applications');
  });

  it('Does not load applications results on render if the filter is loading', function () {
    const dashboardApplicationsProps = {
      filterLoading: true,
      needsAcknowledgement: false,
      applicationResults: {
        results: [
          { applicationId: 'app1', totalApplicationRisk: {}, stageRisks: [] },
          { applicationId: 'app2', totalApplicationRisk: {}, stageRisks: [] },
        ],
        sortFields: ['-totalApplicationRisk.totalRisk'],
      },
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadResults).not.toHaveBeenCalled();
  });

  it('Does not load applications results on render if the filter needs acknowledgment', function () {
    const dashboardApplicationsProps = {
      filterLoading: false,
      needsAcknowledgement: true,
      applicationResults: {
        results: [
          { applicationId: 'app1', totalApplicationRisk: {}, stageRisks: [] },
          { applicationId: 'app2', totalApplicationRisk: {}, stageRisks: [] },
        ],
        sortFields: ['-totalApplicationRisk.totalRisk'],
      },
    };

    getMountedComponent(dashboardApplicationsProps);
    expect(minimalProps.loadResults).not.toHaveBeenCalled();
  });
});
