/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import DashboardComponents from '../../../../../main/frontend/dashboard/results/components/DashboardComponents';
import DashboardComponentsTable
  from '../../../../../main/frontend/dashboard/results/components/DashboardComponentsTable';

describe('DashboardComponents', function() {
  let minimalProps,
      getShallowComponent,
      getMountedComponent;

  beforeEach(function() {
    minimalProps = {
      loadResults: jasmine.createSpy('loadResults'),
      sortResults: jasmine.createSpy('sortResults'),
      stateGo: jasmine.createSpy('stateGo')
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardComponents, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardComponents, minimalProps);
  });

  it('renders a DashboardComponentsTable with the appropriate props', function() {
    const dashboardComponentProps = {
      results: {
        components: ['hash1', 'hash2']
      },
      needsAcknowledgement: true
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
        table = dashBoardComponents.find(DashboardComponentsTable);

    expect(table).toExist();
    expect(table).toHaveProp('componentResults', ['hash1', 'hash2']);
    expect(table).toHaveProp('needsAcknowledgement', true);
    expect(table).toHaveProp('reload', jasmine.any(Function));
    expect(table).toHaveProp('sortComponents', jasmine.any(Function));
    expect(table).toHaveProp('stateGo', jasmine.any(Function));

    table.prop('reload')();
    expect(minimalProps.loadResults).toHaveBeenCalledWith('components');

    table.prop('stateGo')();
    expect(minimalProps.stateGo).toHaveBeenCalled();

    table.prop('sortComponents')();
    expect(minimalProps.sortResults).toHaveBeenCalledWith('components');
  });

  it('renders a mask over the table when filters are dirty', function() {
    const dashboardComponentProps = {
      filtersAreDirty: true
    };

    const dashBoardComponents = getShallowComponent(dashboardComponentProps),
        mask = dashBoardComponents.find('.form-mask');
    expect(mask).toExist();
  });

  it('loads component results on render if the filter is not loading and does not need acknowledgment', function() {
    const dashboardComponentProps = {
      filterLoading: false,
      needsAcknowledgement: false,
      results: {
        components: {
          results: ['hash1', 'hash2'],
          sortFields: ['-score']
        }
      }
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadResults).toHaveBeenCalledWith('components');
  });

  it('Does not load component results on render if the filter is loading', function() {
    const dashboardComponentProps = {
      filterLoading: true,
      needsAcknowledgement: false,
      results: {
        components: {
          results: ['hash1', 'hash2'],
          sortFields: ['-score']
        }
      }
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadResults).not.toHaveBeenCalled();
  });

  it('Does not load component results on render if the filter needs acknowledgment', function() {
    const dashboardComponentProps = {
      filterLoading: false,
      needsAcknowledgement: true,
      results: {
        components: {
          results: ['hash1', 'hash2'],
          sortFields: ['-score']
        }
      }
    };

    getMountedComponent(dashboardComponentProps);
    expect(minimalProps.loadResults).not.toHaveBeenCalled();
  });

});
