/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

import AddWaiverPage from '../../../main/frontend/waivers/AddWaiverPage';

describe('AddWaiverPageContainer', function () {
  let AddWaiverPageContainer,
    saveWaiverMock,
    loadAddWaiverDataMock,
    setWaiverCommentMock,
    setWaiverScopeMock,
    setComponentMatcherStrategyMock,
    setExpiryTimeMock,
    openVulnerabilityDetailsModalMock,
    cancelActionMock,
    store,
    state,
    vdom;

  beforeEach(function () {
    loadAddWaiverDataMock = jasmine.createSpy('loadAddWaiverData').and.returnValue({
      type: 'LOAD_ADD_WAIVER_DATA',
    });
    saveWaiverMock = jasmine.createSpy('saveWaiver').and.returnValue({
      type: 'SAVE_WAIVER',
    });
    setWaiverCommentMock = jasmine.createSpy('setWaiverComment').and.returnValue({
      type: 'SET_WAIVER_COMMENT',
    });
    setWaiverScopeMock = jasmine.createSpy('setWaiverScope').and.returnValue({
      type: 'SET_WAIVER_SCOPE',
    });
    setComponentMatcherStrategyMock = jasmine.createSpy('setComponentMatcherStrategy').and.returnValue({
      type: 'SET_COMPONENT_MATCHER_STRATEGY',
    });
    openVulnerabilityDetailsModalMock = jasmine.createSpy('openVulnerabilityDetailsModal').and.returnValue({
      type: 'OPEN_VULNERABILITY_DETAILS_MODAL',
    });
    cancelActionMock = jasmine.createSpy('cancelAction').and.returnValue({
      type: '@@reduxUiRouter/stateGo',
    });
    setExpiryTimeMock = jasmine.createSpy('setExpiryTime').and.returnValue({
      type: 'ADD_WAIVER_SET_EXPIRY_TIME',
    });

    AddWaiverPageContainer = require('inject-loader!../../../main/frontend/waivers/AddWaiverPageContainer')({
      './waiverActions': {
        loadAddWaiverData: loadAddWaiverDataMock,
        saveWaiverAndRedirect: saveWaiverMock,
        setWaiverComment: setWaiverCommentMock,
        setWaiverScope: setWaiverScopeMock,
        setExpiryTime: setExpiryTimeMock,
        setComponentMatcherStrategy: setComponentMatcherStrategyMock,
        returnToAddWaiverOriginPage: cancelActionMock,
      },
      '../vulnerabilityDetails/vulnerabilityDetailsModalActions': {
        openVulnerabilityDetailsModal: openVulnerabilityDetailsModalMock,
      },
    }).default;

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

    expect(loadAddWaiverDataActionCreator).toEqual(jasmine.any(Function));
    expect(saveWaiverActionCreator).toEqual(jasmine.any(Function));
    expect(setComponentMatcherStrategyCreator).toEqual(jasmine.any(Function));
    expect(setWaiverScopeActionCreator).toEqual(jasmine.any(Function));
    expect(setWaiverCommentActionCreator).toEqual(jasmine.any(Function));
    expect(openVulnerabilityDetailsModalActionCreator).toEqual(jasmine.any(Function));
    expect(returnToAddWaiverOriginPageActionCreator).toEqual(jasmine.any(Function));
    expect(setExpiryTimeActionCreator).toEqual(jasmine.any(Function));

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
