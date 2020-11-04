/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportFilters from '../../../../main/frontend/applicationReport/react/ReportFilters';
import BackButton from '../../../../main/frontend/react/BackButton';
import {NxStatefulTreeViewMultiSelect, NxRadio} from '@sonatype/react-shared-components';
import IqTreeViewPolicyThreatSlider from '../../../../main/frontend/react/IqTreeViewPolicyThreatSlider';

describe('ReportFilters component', function() {

  const proprietaryFilterOptions = [
    { id: 'nonProprietary', name: 'Non-Proprietary' },
    { id: 'proprietary', name: 'Proprietary' }
  ];

  const matchStateFilterOptions = [
    { id: 'exact', name: 'Exact' },
    { id: 'similar', name: 'Similar' },
    { id: 'unknown', name: 'Unknown' }
  ];

  const violationStateFilterOptions = [
    { id: 'notViolating', name: 'Not Violating' },
    { id: 'open', name: 'Open' },
    { id: 'waived', name: 'Waived' },
    { id: 'grandfathered', name: 'Grandfathered' }
  ];

  const dependencyTypeFilterOptions = [
    { id: 'direct', name: 'Direct Dependencies' },
    { id: 'transitive', name: 'Transitive Dependencies' },
    { id: 'unknown', name: 'Unknown' }
  ];

  const policyTypeFilterOptions = [
    {
      id: 'SECURITY',
      name: 'Security'
    }, {
      id: 'LICENSE',
      name: 'License'
    }, {
      id: 'QUALITY',
      name: 'Quality'
    }, {
      id: 'OTHER',
      name: 'Other'
    }
  ];

  let setAggregateReportEntriesSpy, setExactValueFilterSpy, getShallowComponent, mock$State, minimalProps;

  beforeEach(function() {
    const initSpies = () => {
      setAggregateReportEntriesSpy = jasmine.createSpy('setAggregateReportEntries');
      setExactValueFilterSpy = jasmine.createSpy('setExactValueFilter');
      mock$State = jasmine.createSpyObj('$state', ['get', 'href']);
    };

    minimalProps = () => {
      initSpies();
      return {
        setAggregateReportEntries: setAggregateReportEntriesSpy,
        setExactValueFilter: setExactValueFilterSpy,
        $state: mock$State,
        exactValueFilters: {},
        aggregate: true
      };
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportFilters, minimalProps());
  });

  it('renders a BackButton with the violations state name and the provided $state object, ', function() {
    const backButton = getShallowComponent().find(BackButton);

    expect(backButton).toExist();
    expect(backButton).toHaveProp('stateName', 'violations');
    expect(backButton).toHaveProp('$state', mock$State);
  });

  it('renders view options radio', function() {
    const targetRadios = getShallowComponent().find(NxRadio),
        aggregateViewRadio = targetRadios.at(0),
        allComponentsViewRadio = targetRadios.at(1);

    expect(aggregateViewRadio).toExist();
    expect(aggregateViewRadio).toHaveProp('value', 'aggregate');
    expect(aggregateViewRadio).toHaveProp('isChecked', true);

    expect(allComponentsViewRadio).toExist();
    expect(allComponentsViewRadio).toHaveProp('value', 'all');
    expect(allComponentsViewRadio).toHaveProp('isChecked', false);
  });

  it('renders filter contents', function() {
    const targetFilters = getShallowComponent().find(NxStatefulTreeViewMultiSelect),
        proprietaryMultiSelect = targetFilters.at(0),
        matchSateMultiSelect = targetFilters.at(1),
        violationSateMultiSelect = targetFilters.at(2),
        dependencyTypeMultiSelect = targetFilters.at(3),
        policyTypesMultiSelect = targetFilters.at(4);

    assertMultiSelectInitialState(proprietaryMultiSelect, proprietaryFilterOptions, new Set());
    assertMultiSelectInitialState(matchSateMultiSelect, matchStateFilterOptions, undefined);
    assertMultiSelectInitialState(violationSateMultiSelect, violationStateFilterOptions, new Set());
    assertMultiSelectInitialState(dependencyTypeMultiSelect, dependencyTypeFilterOptions, undefined);
    assertMultiSelectInitialState(policyTypesMultiSelect, policyTypeFilterOptions, undefined);

    const policyThreatSlider = getShallowComponent().find(IqTreeViewPolicyThreatSlider).at(0);
    expect(policyThreatSlider).toExist();
    expect(policyThreatSlider).toHaveProp('value', [0, 10]);
  });

  it('dispatches correct action when toggeling view option', function() {
    const targetRadios = getShallowComponent().find(NxRadio),
        aggregateViewRadio = targetRadios.at(0),
        allComponentsViewRadio = targetRadios.at(1);

    allComponentsViewRadio.simulate('change', 'all');
    expect(setAggregateReportEntriesSpy).toHaveBeenCalledWith(false);

    aggregateViewRadio.simulate('change', 'aggregate');
    expect(setAggregateReportEntriesSpy).toHaveBeenCalledWith(true);
  });

  it('dispatches correct actions when selecting filters', function() {
    assertMultiSelectActionDispatch('#proprietary-filter', ['proprietary'], 'proprietary', new Set([true]));
    assertMultiSelectActionDispatch('#proprietary-filter', ['nonProprietary'], 'proprietary', new Set([false]));
    assertMultiSelectActionDispatch('#proprietary-filter', ['proprietary', 'nonProprietary'], 'proprietary',
        new Set([true, false]));

    assertMultiSelectActionDispatch('#match-state-filter', ['exact'], 'matchState');
    assertMultiSelectActionDispatch('#match-state-filter', ['exact', 'similar'], 'matchState');
    assertMultiSelectActionDispatch('#match-state-filter', ['exact', 'similar', 'unknown'], 'matchState');

    assertMultiSelectActionDispatch('#violation-state-filter', ['open'], 'derivedViolationState', new Set(['open']));
    assertMultiSelectActionDispatch('#violation-state-filter', ['open', 'notViolating'], 'derivedViolationState',
        new Set(['open', 'notViolating']));
    assertMultiSelectActionDispatch('#violation-state-filter', ['waived'], 'derivedViolationState',
        new Set(['waived', 'waived+grandfathered']));
    assertMultiSelectActionDispatch('#violation-state-filter', ['grandfathered'], 'derivedViolationState',
        new Set(['grandfathered', 'waived+grandfathered']));
    assertMultiSelectActionDispatch('#violation-state-filter', ['waived', 'grandfathered'], 'derivedViolationState',
        new Set(['waived', 'grandfathered', 'waived+grandfathered']));

    assertMultiSelectActionDispatch('#dependency-type-filter', ['direct'], 'derivedDependencyType');
    assertMultiSelectActionDispatch('#dependency-type-filter', ['transitive'], 'derivedDependencyType');
    assertMultiSelectActionDispatch('#dependency-type-filter', ['direct', 'transitive', 'unknown'],
        'derivedDependencyType');

    assertMultiSelectActionDispatch('#policy-type-filter', ['SECURITY'], 'policyThreatCategory');
    assertMultiSelectActionDispatch('#policy-type-filter', ['SECURITY', 'LICENSE'], 'policyThreatCategory');
    assertMultiSelectActionDispatch('#policy-type-filter', ['SECURITY', 'LICENSE', 'QUALITY', 'OTHER'],
        'policyThreatCategory');

    assertMultiSelectActionDispatch('#threat-level-filter', [1, 9], 'policyThreatLevel',
        new Set([1, 2, 3, 4, 5, 6, 7, 8, 9]));
    assertMultiSelectActionDispatch('#threat-level-filter', [5, 6], 'policyThreatLevel', new Set([5, 6]));
    assertMultiSelectActionDispatch('#threat-level-filter', [0, 10], 'policyThreatLevel', new Set());
  });

  const assertMultiSelectActionDispatch = (elementId, changeValues, filterName, actionPayload) => {
    const targetFilter = enzymeUtils.getShallowComponent(ReportFilters, minimalProps())().find(elementId);
    targetFilter.simulate('change', changeValues);
    expect(setExactValueFilterSpy).toHaveBeenCalledWith(filterName, actionPayload || new Set(changeValues));
  };

  const assertMultiSelectInitialState = (component, options, selectedIds) => {
    expect(component).toExist();
    expect(component).toHaveProp('options', options);
    expect(component).toHaveProp('selectedIds', selectedIds);
  };
});

