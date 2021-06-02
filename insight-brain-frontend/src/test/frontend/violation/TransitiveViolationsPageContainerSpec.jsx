/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import TransitiveViolationsPage from '../../../main/frontend/violation/TransitiveViolationsPage';

describe('TransitiveViolationsPageContainer', function () {
  let store,
    state,
    vdom,
    TransitiveViolationsPageContainer,
    loadAvailableScopesActionMock,
    loadTransitiveViolationsActionMock,
    setSortingParametersActionMock,
    setFilteringParametersActionMock;

  beforeEach(function () {
    state = {
      router: {
        prevState: {
          name: 'applicationReport.policy',
        },
        prevParams: {
          scanId: 'someScanId',
        },
        currentParams: {
          ownerType: 'someOwnerType',
          ownerId: 'someOwnerId',
          stageTypeId: 'someStageTypeId',
          hash: 'someHash',
        },
      },
      transitiveViolations: {
        availableScopes: 'someAvailableScopes',
        componentTransitivePolicyViolations: 'someComponentTransitivePolicyViolations',
      },
    };
    loadAvailableScopesActionMock = jasmine
      .createSpy('loadAvailableScopesActionMock')
      .and.returnValue({ type: 'BAR1' });
    loadTransitiveViolationsActionMock = jasmine
      .createSpy('loadTransitiveViolationsActionMock')
      .and.returnValue({ type: 'BAR2' });
    setSortingParametersActionMock = jasmine
      .createSpy('setSortingParametersActionMock')
      .and.returnValue({ type: 'BAR3' });
    setFilteringParametersActionMock = jasmine
      .createSpy('setFilteringParametersActionMock')
      .and.returnValue({ type: 'BAR4' });
    TransitiveViolationsPageContainer = require('inject-loader!../../../main/frontend/violation/TransitiveViolationsPageContainer')(
      {
        './transitiveViolationsActions': {
          loadAvailableScopes: loadAvailableScopesActionMock,
          loadTransitiveViolations: loadTransitiveViolationsActionMock,
          setSortingParameters: setSortingParametersActionMock,
          setFilteringParameters: setFilteringParametersActionMock,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <TransitiveViolationsPageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scanId', 'someScanId');
    expect(wrapper).toHaveProp('ownerType', 'someOwnerType');
    expect(wrapper).toHaveProp('ownerId', 'someOwnerId');
    expect(wrapper).toHaveProp('stageTypeId', 'someStageTypeId');
    expect(wrapper).toHaveProp('hash', 'someHash');
    expect(wrapper).toHaveProp('availableScopes', 'someAvailableScopes');
    expect(wrapper).toHaveProp('componentTransitivePolicyViolations', 'someComponentTransitivePolicyViolations');
  });

  it('has an undefined scanId if the previous state is not an application report', () => {
    state.router.prevState.name = 'other';
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scanId', undefined);
  });

  it('correctly maps the action creators to the TransitiveViolationsPageContainer props', function () {
    const wrapper = shallow(vdom).dive();

    const loadAvailableScopesActionCreator = wrapper.prop('loadAvailableScopes');
    expect(loadAvailableScopesActionCreator).toEqual(jasmine.any(Function));
    loadAvailableScopesActionCreator('test');
    expect(store.getActions()[0]).toEqual({ type: 'BAR1' });

    const loadTransitiveViolationsActionCreator = wrapper.prop('loadTransitiveViolations');
    expect(loadTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    loadTransitiveViolationsActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'BAR2' });

    const setSortingParametersActionCreator = wrapper.prop('setSortingParameters');
    expect(setSortingParametersActionCreator).toEqual(jasmine.any(Function));
    setSortingParametersActionCreator('test');
    expect(store.getActions()[2]).toEqual({ type: 'BAR3' });

    const setFilteringParametersActionCreator = wrapper.prop('setFilteringParameters');
    expect(setFilteringParametersActionCreator).toEqual(jasmine.any(Function));
    setFilteringParametersActionCreator('test');
    expect(store.getActions()[3]).toEqual({ type: 'BAR4' });
  });

  it('renders the TransitiveViolationsPage component', function () {
    const transitiveViolationsPage = shallow(vdom).find(TransitiveViolationsPage);
    expect(transitiveViolationsPage).toExist();
  });
});
