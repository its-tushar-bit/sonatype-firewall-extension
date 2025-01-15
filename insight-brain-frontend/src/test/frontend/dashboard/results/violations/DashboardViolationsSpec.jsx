/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import DashboardViolations from 'MainRoot/dashboard/results/violations/DashboardViolations';
import DashboardViolationsTable from 'MainRoot/dashboard/results/violations/DashboardViolationsTable';
import DashboardMask from 'MainRoot/dashboard/results/dashboardMask/DashboardMask';

describe('DashboardViolations', function () {
  let minimalProps, getShallowComponent, getMountedComponent, loadViolationResultsSpy;

  beforeEach(function () {
    loadViolationResultsSpy = jasmine.createSpy('loadViolationResults');

    minimalProps = {
      violations: {
        results: {},
        hasNextPage: false,
        sortFields: ['field'],
        hasMultiplePages: false,
        page: null,
      },
      appliedFilter: {
        maxDaysOld: 0,
      },
      filterLoading: false,
      needsAcknowledgement: false,
      loadViolationResults: loadViolationResultsSpy,
      sortViolations: () => {},
      stateGo: () => {},
      setNextViolationsPage: () => {},
      setPreviousViolationsPage: () => {},
    };

    (getShallowComponent = enzymeUtils.getShallowComponent(DashboardViolations, minimalProps)),
      (getMountedComponent = enzymeUtils.getMountedComponent(DashboardViolations, minimalProps));
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('calls loadResults if filters are not loading and needsAcknowledgement is false', () => {
    getMountedComponent();
    expect(loadViolationResultsSpy).toHaveBeenCalledTimes(1);
  });

  it('does not load results if filters are loading', () => {
    getMountedComponent({ filterLoading: true });
    expect(loadViolationResultsSpy).not.toHaveBeenCalled();
  });

  it('does not load results if needs acknowledgement', () => {
    getMountedComponent({ needsAcknowledgement: true });
    expect(loadViolationResultsSpy).not.toHaveBeenCalled();
  });

  it('renders a form mask if filters are dirty', () => {
    const component = getShallowComponent({ filtersAreDirty: true });
    expect(component.find(DashboardMask)).toExist();
  });

  it('does not render a form mask if filters are not dirty', () => {
    const component = getShallowComponent({ filtersAreDirty: false });
    expect(component.find('.form-mask')).not.toExist();
  });

  it('does not render a form mask if filters are dirty and needs acknowledgement', () => {
    const component = getMountedComponent({ filtersAreDirty: true, needsAcknowledgement: true });
    expect(component.find('.form-mask')).not.toExist();
  });

  it('does not render a mask over the table when filters are dirty but there are no results', () => {
    const component = getMountedComponent({
      filtersAreDirty: true,
      violations: {
        results: null,
        hasNextPage: false,
        sortFields: ['field'],
        hasMultiplePages: false,
        page: null,
      },
    });
    expect(component.find('.form-mask')).not.toExist();
  });

  it('renders a mask over the table when there are no results but there is an error', () => {
    const component = getMountedComponent({
      filtersAreDirty: true,
      violations: {
        results: null,
        hasNextPage: false,
        error: 'error',
        sortFields: ['field'],
        hasMultiplePages: false,
        page: null,
      },
    });
    expect(component.find('.form-mask')).toExist();
  });

  it('renders a DashboardViolationsTable component', () => {
    const component = getShallowComponent(),
      table = component.find(DashboardViolationsTable);

    expect(table).toExist();
    expect(table).toHaveProp('violations', minimalProps.violations);
    expect(table).toHaveProp('needsAcknowledgement', minimalProps.needsAcknowledgement);
    expect(table).toHaveProp('maxDaysOld', minimalProps.appliedFilter.maxDaysOld);
    expect(table).toHaveProp('stateGo', minimalProps.stateGo);
    expect(table).toHaveProp('sortViolations');
    expect(table).toHaveProp('reload');
    expect(table).toHaveProp('setNextViolationsPage');
    expect(table).toHaveProp('setPreviousViolationsPage');
  });
});
