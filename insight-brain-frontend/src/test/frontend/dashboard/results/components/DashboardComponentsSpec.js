/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import DashboardComponents from 'MainRoot/dashboard/results/components/DashboardComponents';
import DashboardComponentsTable from 'MainRoot/dashboard/results/components/DashboardComponentsTable';
import DashboardMask from 'MainRoot/dashboard/results/dashboardMask/DashboardMask';

describe('DashboardComponents', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      loadComponentResults: jasmine.createSpy('loadComponentResults'),
      sortComponents: jasmine.createSpy('sortComponents'),
      stateGo: jasmine.createSpy('stateGo'),
      componentResults: {
        results: ['hash1', 'hash2'],
        sortFields: ['-score'],
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardComponents, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardComponents, minimalProps);
  });

  it('renders a DashboardComponentsTable with the appropriate props', function () {
    const dashboardComponentProps = {
      needsAcknowledgement: true,
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
      table = dashBoardComponents.find(DashboardComponentsTable);

    expect(table).toExist();
    expect(table).toHaveProp('componentResults', {
      results: ['hash1', 'hash2'],
      sortFields: ['-score'],
    });
    expect(table).toHaveProp('needsAcknowledgement', true);
    expect(table).toHaveProp('reload', jasmine.any(Function));
    expect(table).toHaveProp('sortComponents', jasmine.any(Function));
    expect(table).toHaveProp('stateGo', jasmine.any(Function));

    table.prop('reload')();
    expect(minimalProps.loadComponentResults).toHaveBeenCalled();

    table.prop('stateGo')();
    expect(minimalProps.stateGo).toHaveBeenCalled();

    table.prop('sortComponents')();
    expect(minimalProps.sortComponents).toHaveBeenCalled();
  });

  it('renders a mask over the table when filters are dirty', function () {
    const dashboardComponentProps = {
      filtersAreDirty: true,
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
      mask = dashBoardComponents.find(DashboardMask);
    expect(mask).toExist();
  });

  it('does not render a mask over the table when filters are dirty and needs acknowledgement', function () {
    const dashboardComponentProps = {
      filtersAreDirty: true,
      needsAcknowledgement: true,
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
      mask = dashBoardComponents.find(DashboardMask);
    expect(mask).not.toExist();
  });

  it('does not render a mask over the table when filters are dirty but there are no results', function () {
    const dashboardComponentProps = {
      filtersAreDirty: true,
      componentResults: {
        results: null,
      },
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
      mask = dashBoardComponents.find(DashboardMask);
    expect(mask).not.toExist();
  });

  it('renders a mask over the table when there are no results but there is an error', function () {
    const dashboardComponentProps = {
      filtersAreDirty: true,
      componentResults: {
        results: null,
        error: 'error',
      },
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
      mask = dashBoardComponents.find(DashboardMask);
    expect(mask).toExist();
  });

  it('loads component results on render if the filter is not loading and does not need acknowledgment', function () {
    const dashboardComponentProps = {
      filterLoading: false,
      needsAcknowledgement: false,
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadComponentResults).toHaveBeenCalledTimes(1);
  });

  it('Does not load component results on render if the filter is loading', function () {
    const dashboardComponentProps = {
      filterLoading: true,
      needsAcknowledgement: false,
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadComponentResults).not.toHaveBeenCalled();
  });

  it('Does not load component results on render if the filter needs acknowledgment', function () {
    const dashboardComponentProps = {
      filterLoading: false,
      needsAcknowledgement: true,
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadComponentResults).not.toHaveBeenCalled();
  });
});
