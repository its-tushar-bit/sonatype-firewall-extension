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
    loadReportMetadataActionMock,
    loadTransitiveViolationsActionMock,
    setSortingParametersActionMock,
    setFilteringParametersActionMock;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          ownerType: 'someOwnerType',
          ownerId: 'someOwnerId',
          scanId: 'someScanId',
          hash: 'someHash',
        },
      },
      transitiveViolations: {
        availableScopes: 'someAvailableScopes',
        reportMetadata: 'someReportMetadata',
        componentTransitivePolicyViolations: 'someComponentTransitivePolicyViolations',
      },
    };
    loadAvailableScopesActionMock = jasmine
      .createSpy('loadAvailableScopesActionMock')
      .and.returnValue({ type: 'BAR1' });
    loadReportMetadataActionMock = jasmine.createSpy('loadReportMetadataActionMock').and.returnValue({ type: 'BAR2' });
    loadTransitiveViolationsActionMock = jasmine
      .createSpy('loadTransitiveViolationsActionMock')
      .and.returnValue({ type: 'BAR3' });
    setSortingParametersActionMock = jasmine
      .createSpy('setSortingParametersActionMock')
      .and.returnValue({ type: 'BAR4' });
    setFilteringParametersActionMock = jasmine
      .createSpy('setFilteringParametersActionMock')
      .and.returnValue({ type: 'BAR5' });
    TransitiveViolationsPageContainer = require('inject-loader!../../../main/frontend/violation/TransitiveViolationsPageContainer')(
      {
        './transitiveViolationsActions': {
          loadAvailableScopes: loadAvailableScopesActionMock,
          loadReportMetadata: loadReportMetadataActionMock,
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
    expect(wrapper).toHaveProp('scanId', 'someScanId');
    expect(wrapper).toHaveProp('hash', 'someHash');
    expect(wrapper).toHaveProp('availableScopes', 'someAvailableScopes');
    expect(wrapper).toHaveProp('reportMetadata', 'someReportMetadata');
    expect(wrapper).toHaveProp('componentTransitivePolicyViolations', 'someComponentTransitivePolicyViolations');
  });

  it('correctly maps the action creators to the TransitiveViolationsPageContainer props', function () {
    const wrapper = shallow(vdom).dive();

    const loadAvailableScopesActionCreator = wrapper.prop('loadAvailableScopes');
    expect(loadAvailableScopesActionCreator).toEqual(jasmine.any(Function));
    loadAvailableScopesActionCreator('test');
    expect(store.getActions()[0]).toEqual({ type: 'BAR1' });

    const loadReportMetadataActionCreator = wrapper.prop('loadReportMetadata');
    expect(loadReportMetadataActionCreator).toEqual(jasmine.any(Function));
    loadReportMetadataActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'BAR2' });

    const loadTransitiveViolationsActionCreator = wrapper.prop('loadTransitiveViolations');
    expect(loadTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    loadTransitiveViolationsActionCreator('test');
    expect(store.getActions()[2]).toEqual({ type: 'BAR3' });

    const setSortingParametersActionCreator = wrapper.prop('setSortingParameters');
    expect(setSortingParametersActionCreator).toEqual(jasmine.any(Function));
    setSortingParametersActionCreator('test');
    expect(store.getActions()[3]).toEqual({ type: 'BAR4' });

    const setFilteringParametersActionCreator = wrapper.prop('setFilteringParameters');
    expect(setFilteringParametersActionCreator).toEqual(jasmine.any(Function));
    setFilteringParametersActionCreator('test');
    expect(store.getActions()[4]).toEqual({ type: 'BAR5' });
  });

  it('renders the TransitiveViolationsPage component', function () {
    const transitiveViolationsPage = shallow(vdom).find(TransitiveViolationsPage);
    expect(transitiveViolationsPage).toExist();
  });
});
