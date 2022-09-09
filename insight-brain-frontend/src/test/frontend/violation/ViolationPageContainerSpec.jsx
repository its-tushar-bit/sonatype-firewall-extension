/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('ViolationPageContainer', function () {
  let ViolationPageContainer,
    loadViolationActionMock,
    loadVulnerabilityDetailsActionMock,
    stateGoMock,
    fetchStageTypesMock,
    state,
    store,
    vdom,
    loadFirewallPolicyVulnerabilityDetailsMock,
    mock$State;

  beforeEach(function () {
    loadViolationActionMock = jasmine.createSpy('loadViolation').and.returnValue({ type: 'LOAD_VIOLATION' });
    loadVulnerabilityDetailsActionMock = jasmine
      .createSpy('loadVulnerabilityDetails')
      .and.returnValue({ type: 'LOAD_VULNERABILITY' });
    fetchStageTypesMock = jasmine.createSpy('fetchStageTypes').and.returnValue({ type: 'FETCH_STAGE_TYPES' });
    stateGoMock = jasmine.createSpy('stateGo').and.returnValue({ type: 'STATE_GO' });
    loadFirewallPolicyVulnerabilityDetailsMock = jasmine
      .createSpy('loadFirewallPolicyVulnerabilityDetails')
      .and.returnValue({ type: 'LOAD_POLICY_VULNERABILITY' });

    ViolationPageContainer = require('inject-loader!../../../main/frontend/violation/ViolationPageContainer')({
      './violationActions': {
        loadViolation: loadViolationActionMock,
        loadVulnerabilityDetails: loadVulnerabilityDetailsActionMock,
        loadFirewallPolicyVulnerabilityDetails: loadFirewallPolicyVulnerabilityDetailsMock,
      },
      '../stages/stagesActions': {
        fetchStageTypes: fetchStageTypesMock,
      },
      '../reduxUiRouter/routerActions': {
        stateGo: stateGoMock,
      },
    }).default;

    state = {
      violation: {
        loading: false,
        violationDetailsError: null,
      },
      stages: {
        dashboard: {
          stageTypes: null,
          error: null,
        },
      },
      componentDetailsPolicyViolations: { selectedViolationId: 'foo' },
      router: {
        currentParams: {
          id: 'foo',
        },
      },
      firewall: {
        componentDetailsPage: {
          policyViolations: [],
        },
      },
      showViolationsDetailPopover: {
        isPolicyPopoverShown: undefined,
        selectPolicyId: undefined,
      },
    };

    store = configureStore()(() => state);
    vdom = <ViolationPageContainer store={store} $state={mock$State} />;
  });

  it('maps the state slice ("violation") to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('violationDetailsError', null);

    state = {
      ...state,
      violation: {
        loading: true,
        violationDetailsError: 'foo',
      },
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('violationDetailsError', 'foo');
  });

  it('maps the stageTypes and error props from the dashboard stages to `stageTypes` and `stageTypesError`', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stageTypes', null);
    expect(wrapper).toHaveProp('stageTypesError', null);

    state = {
      ...state,
      stages: {
        dashboard: {
          stageTypes: [],
          error: 'foo',
        },
      },
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stageTypes', []);
    expect(wrapper).toHaveProp('stageTypesError', 'foo');
  });

  it('maps action creators to ViolationPageContainer props', function () {
    const wrapper = shallow(vdom).dive(),
      loadViolationActionCreator = wrapper.prop('loadViolation'),
      loadVulnerabilityDetailsActionCreator = wrapper.prop('loadVulnerabilityDetails'),
      loadFirewallPolicyVulnerabilityDetailsCreator = wrapper.prop('loadFirewallPolicyVulnerabilityDetails'),
      fetchStageTypesActionCreator = wrapper.prop('fetchStageTypes'),
      stateGoActionCreator = wrapper.prop('stateGo');

    expect(loadViolationActionCreator).toEqual(jasmine.any(Function));
    expect(loadVulnerabilityDetailsActionCreator).toEqual(jasmine.any(Function));
    expect(fetchStageTypesActionCreator).toEqual(jasmine.any(Function));
    expect(stateGoActionCreator).toEqual(jasmine.any(Function));
    expect(loadFirewallPolicyVulnerabilityDetailsCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadViolationActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }]);

    loadVulnerabilityDetailsActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }, { type: 'LOAD_VULNERABILITY' }]);

    fetchStageTypesActionCreator();

    expect(store.getActions()).toEqual([
      { type: 'LOAD_VIOLATION' },
      { type: 'LOAD_VULNERABILITY' },
      { type: 'FETCH_STAGE_TYPES' },
    ]);

    stateGoActionCreator();

    expect(store.getActions()).toEqual([
      { type: 'LOAD_VIOLATION' },
      { type: 'LOAD_VULNERABILITY' },
      { type: 'FETCH_STAGE_TYPES' },
      { type: 'STATE_GO' },
    ]);

    loadFirewallPolicyVulnerabilityDetailsCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_VIOLATION' },
      { type: 'LOAD_VULNERABILITY' },
      { type: 'FETCH_STAGE_TYPES' },
      { type: 'STATE_GO' },
      { type: 'LOAD_POLICY_VULNERABILITY' },
    ]);
  });

  it('maps the state slice "policyViolations" to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('policyViolations', []);

    state = {
      ...state,
      firewall: {
        componentDetailsPage: {
          policyViolations: [{ policyViolationId: '02a6107559a94c39b04d4ec8374b9508' }],
        },
      },
    };

    // force state update
    store.dispatch({ type: 'UPDATE POLICY VIOLATION' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('policyViolations', [{ policyViolationId: '02a6107559a94c39b04d4ec8374b9508' }]);
  });

  it('maps the state slice "isPolicyPopoverShown" to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('isPolicyPopoverShown', undefined);
  });

  it('maps the state slice "selectPolicyId" to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('selectPolicyId', undefined);
  });
});
