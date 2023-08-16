/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'jest-enzyme';
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import AddWaiverPageContainer from 'MainRoot/waivers/AddWaiverPageContainer';

import AddWaiverPage from '../../../main/frontend/waivers/AddWaiverPage';

jest.mock('MainRoot/waivers/waiverActions', () => ({
  loadAddWaiverData: () => ({ type: 'LOAD_ADD_WAIVER_DATA' }),
  saveWaiverAndRedirect: () => ({ type: 'SAVE_WAIVER' }),
  setWaiverComment: () => ({ type: 'SET_WAIVER_COMMENT' }),
  setWaiverScope: () => ({ type: 'SET_WAIVER_SCOPE' }),
  setComponentMatcherStrategy: () => ({ type: 'SET_COMPONENT_MATCHER_STRATEGY' }),
  returnToAddWaiverOriginPage: () => ({ type: '@@reduxUiRouter/stateGo' }),
  setExpiryTime: () => ({ type: 'ADD_WAIVER_SET_EXPIRY_TIME' }),
}));

jest.mock('MainRoot/vulnerabilityDetails/vulnerabilityDetailsModalActions', () => ({
  openVulnerabilityDetailsModal: () => ({ type: 'OPEN_VULNERABILITY_DETAILS_MODAL' }),
}));

jest.mock('MainRoot/reduxUiRouter/routerSelectors', () => {
  const actual = jest.requireActual('MainRoot/reduxUiRouter/routerSelectors');

  return {
    ...actual,
    selectIsFirewall: () => false,
  };
});

jest.mock('MainRoot/firewall/firewallSelectors', () => ({
  selectFirewallComponentDetailsPageRouteParams: () => ({
    componentHash: 'componentHash',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'ant',
        classifier: '',
        extension: 'jar',
        groupId: 'ant',
        version: '1.6',
      },
    },
    repositoryId: 'repositoryId',
    matchState: 'matchState',
    proprietary: 'proprietary',
    identificationSource: 'identificationSource',
    pathname: 'pathname',
  }),
}));

describe('AddWaiverPageContainer', function () {
  let store, state, vdom;

  beforeEach(function () {
    state = {
      addWaiver: {
        loading: false,
        waiverComments: {
          value: '',
          isPristine: true,
        },
      },
      violation: {
        violationDetails: {},
      },
      router: {
        currentParams: {
          violationId: 'foo',
          componentHash: 'componentHash',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          repositoryId: 'repositoryId',
          matchState: 'matchState',
          proprietary: 'proprietary',
          identificationSource: 'identificationSource',
          pathname: 'pathname',
        },
        prevState: {
          name: 'prevStateName',
        },
        prevParams: {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
          sidebarReference: 'sidebarReference',
          type: 'type',
        },
      },
      vulnerabilityDetailsModal: {
        vulnerabilityId: 'CVE-12345',
      },
      user: {
        currentUser: {
          displayName: 'test user',
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <AddWaiverPageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('prevStateName', 'prevStateName');
    expect(wrapper).toHaveProp('prevParams', {
      publicId: 'publicId',
      scanId: 'scanId',
      hash: 'hash',
      sidebarReference: 'sidebarReference',
      type: 'type',
    });
    expect(wrapper).toHaveProp('violationDetails', {});
    state = {
      ...state,
      addWaiver: {
        loading: true,
      },
      violation: {
        violationDetails: {
          id: 'bar',
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', { id: 'bar' });
    expect(wrapper).toHaveProp('currentUser', 'test user');
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      loadAddWaiverDataActionCreator = wrapper.prop('loadAddWaiverData'),
      saveWaiverActionCreator = wrapper.prop('saveWaiver'),
      setComponentMatcherStrategyCreator = wrapper.prop('setComponentMatcherStrategy'),
      setWaiverScopeActionCreator = wrapper.prop('setWaiverScope'),
      setWaiverCommentActionCreator = wrapper.prop('setWaiverComment'),
      openVulnerabilityDetailsModalActionCreator = wrapper.prop('openVulnerabilityDetailsModal'),
      returnToAddWaiverOriginPageActionCreator = wrapper.prop('cancelAction'),
      setExpiryTimeActionCreator = wrapper.prop('setExpiryTime');

    expect(loadAddWaiverDataActionCreator).toEqual(expect.any(Function));
    expect(saveWaiverActionCreator).toEqual(expect.any(Function));
    expect(setComponentMatcherStrategyCreator).toEqual(expect.any(Function));
    expect(setWaiverScopeActionCreator).toEqual(expect.any(Function));
    expect(setWaiverCommentActionCreator).toEqual(expect.any(Function));
    expect(openVulnerabilityDetailsModalActionCreator).toEqual(expect.any(Function));
    expect(returnToAddWaiverOriginPageActionCreator).toEqual(expect.any(Function));
    expect(setExpiryTimeActionCreator).toEqual(expect.any(Function));

    expect(store.getActions()).toEqual([]);

    loadAddWaiverDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_ADD_WAIVER_DATA' }]);

    saveWaiverActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_ADD_WAIVER_DATA' }, { type: 'SAVE_WAIVER' }]);

    setComponentMatcherStrategyCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
    ]);

    setWaiverScopeActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
      { type: 'SET_WAIVER_SCOPE' },
    ]);

    setWaiverCommentActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
      { type: 'SET_WAIVER_SCOPE' },
      { type: 'SET_WAIVER_COMMENT' },
    ]);

    openVulnerabilityDetailsModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
      { type: 'SET_WAIVER_SCOPE' },
      { type: 'SET_WAIVER_COMMENT' },
      { type: 'OPEN_VULNERABILITY_DETAILS_MODAL' },
    ]);

    setExpiryTimeActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
      { type: 'SET_WAIVER_SCOPE' },
      { type: 'SET_WAIVER_COMMENT' },
      { type: 'OPEN_VULNERABILITY_DETAILS_MODAL' },
      { type: 'ADD_WAIVER_SET_EXPIRY_TIME' },
    ]);

    returnToAddWaiverOriginPageActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_ADD_WAIVER_DATA' },
      { type: 'SAVE_WAIVER' },
      { type: 'SET_COMPONENT_MATCHER_STRATEGY' },
      { type: 'SET_WAIVER_SCOPE' },
      { type: 'SET_WAIVER_COMMENT' },
      { type: 'OPEN_VULNERABILITY_DETAILS_MODAL' },
      { type: 'ADD_WAIVER_SET_EXPIRY_TIME' },
      { type: '@@reduxUiRouter/stateGo' },
    ]);
  });

  it('renders AddWaiverPage component', function () {
    const addWaiverPageComponent = shallow(vdom).find(AddWaiverPage);
    expect(addWaiverPageComponent).toExist();
    expect(addWaiverPageComponent).toHaveProp('violationId', 'foo');
  });
});
