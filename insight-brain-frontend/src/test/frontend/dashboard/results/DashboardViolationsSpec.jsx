/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import DashboardViolations from '../../../../main/frontend/dashboard/results/violations/DashboardViolations';
import DashboardViolationsTable from '../../../../main/frontend/dashboard/results/violations/DashboardViolationsTable';

describe('DashboardViolations', function() {
  let minimalProps,
      getShallowComponent,
      getMountedComponent,
      loadResultsSpy;

  beforeEach(function() {
    loadResultsSpy = jasmine.createSpy('loadResults');

    minimalProps = {
      results: {
        violations: {
          results: {},
          sortFields: ['field']
        }
      },
      appliedFilter: {
        maxDaysOld: 0
      },
      filterLoading: false,
      needsAcknowledgement: false,
      loadResults: loadResultsSpy,
      sortResults: () => {},
      stateGo: () => {}
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardViolations, minimalProps),
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardViolations, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('calls loadResults if filters are not loading and needsAcknowledgement is false', () => {
    getMountedComponent();
    expect(loadResultsSpy).toHaveBeenCalledWith('violations');
  });

  it('does not load results if filters are loading', () => {
    getMountedComponent({ filterLoading: true });
    expect(loadResultsSpy).not.toHaveBeenCalled();
  });

  it('does not load results if needs acknowledgement', () => {
    getMountedComponent({ needsAcknowledgement: true });
    expect(loadResultsSpy).not.toHaveBeenCalled();

    getMountedComponent({ filterLoading: false, needsAcknowledgement: true });
    expect(loadResultsSpy).not.toHaveBeenCalled();
  });

  it('renders a form mask if filters are dirty', () => {
    const component = getShallowComponent({ filtersAreDirty: true });
    expect(component.find('.form-mask')).toExist();
  });

  it('does not render a form mask if filters are dirty', () => {
    const component = getShallowComponent({ filtersAreDirty: false });
    expect(component.find('.form-mask')).not.toExist();
  });

  it('renders a DashboardViolationsTable component', () => {
    const component = getShallowComponent(),
        table = component.find(DashboardViolationsTable);

    expect(table).toExist();
    expect(table).toHaveProp('violations', minimalProps.results.violations);
    expect(table).toHaveProp('needsAcknowledgement', minimalProps.needsAcknowledgement);
    expect(table).toHaveProp('maxDaysOld', minimalProps.appliedFilter.maxDaysOld);
    expect(table).toHaveProp('stateGo', minimalProps.stateGo);
    expect(table).toHaveProp('sortViolations');
    expect(table).toHaveProp('reload');
  });
});
