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
    loadTransitiveViolationWaiversMock,
    setSortingParametersActionMock,
    setFilteringParametersActionMock,
    toggleRequestWaiveTransitiveViolationsMock,
    toggleWaiveTransitiveViolationsMock,
    toggleViewTransitiveViolationWaiversMock,
    setSelectedPolicyViolationIdMock,
    toggleShowViolationsDetailPopoverMock,
    setWaiverToDeleteMock;

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
        transitiveViolationWaivers: 'someTransitiveViolationWaivers',
        isRequestWaiveTransitiveViolationsOpen: 'someIsRequestWaiveTransitiveViolationsOpen',
        isWaiveTransitiveViolationsOpen: 'someIsWaiveTransitiveViolationsOpen',
        isViewTransitiveViolationWaiversOpen: 'someIsViewTransitiveViolationWaiversOpen',
      },
      componentDetailsPolicyViolations: {
        showViolationsDetailPopover: 'someValueToShowThePopover',
      },
      deleteWaiver: {
        waiverToDelete: 'someWaiverToDelete',
      },
    };
    loadAvailableScopesActionMock = jasmine
      .createSpy('loadAvailableScopesActionMock')
      .and.returnValue({ type: 'BAR1' });
    loadReportMetadataActionMock = jasmine.createSpy('loadReportMetadataActionMock').and.returnValue({ type: 'BAR2' });
    loadTransitiveViolationsActionMock = jasmine
      .createSpy('loadTransitiveViolationsActionMock')
      .and.returnValue({ type: 'BAR3' });
    loadTransitiveViolationWaiversMock = jasmine
      .createSpy('loadTransitiveViolationWaiversMock')
      .and.returnValue({ type: 'BAR4' });
    setSortingParametersActionMock = jasmine
      .createSpy('setSortingParametersActionMock')
      .and.returnValue({ type: 'BAR5' });
    setFilteringParametersActionMock = jasmine
      .createSpy('setFilteringParametersActionMock')
      .and.returnValue({ type: 'BAR6' });
    toggleRequestWaiveTransitiveViolationsMock = jasmine
      .createSpy('toggleRequestWaiveTransitiveViolationsMock')
      .and.returnValue({ type: 'BAR7' });
    toggleWaiveTransitiveViolationsMock = jasmine
      .createSpy('toggleWaiveTransitiveViolationsMock')
      .and.returnValue({ type: 'BAR8' });
    toggleViewTransitiveViolationWaiversMock = jasmine
      .createSpy('toggleViewTransitiveViolationWaiversMock')
      .and.returnValue({ type: 'BAR9' });
    setSelectedPolicyViolationIdMock = jasmine
      .createSpy('setSelectedPolicyViolationIdMock')
      .and.returnValue({ type: 'BAR10' });
    toggleShowViolationsDetailPopoverMock = jasmine
      .createSpy('toggleShowViolationsDetailPopoverMock')
      .and.returnValue({ type: 'BAR11' });
    setWaiverToDeleteMock = jasmine.createSpy('setWaiverToDeleteMock').and.returnValue({ type: 'BAR12' });
    TransitiveViolationsPageContainer = require('inject-loader!../../../main/frontend/violation/TransitiveViolationsPageContainer')(
      {
        './transitiveViolationsActions': {
          loadAvailableScopes: loadAvailableScopesActionMock,
          loadReportMetadata: loadReportMetadataActionMock,
          loadTransitiveViolations: loadTransitiveViolationsActionMock,
          loadTransitiveViolationWaivers: loadTransitiveViolationWaiversMock,
          setSortingParameters: setSortingParametersActionMock,
          setFilteringParameters: setFilteringParametersActionMock,
          toggleRequestWaiveTransitiveViolations: toggleRequestWaiveTransitiveViolationsMock,
          toggleWaiveTransitiveViolations: toggleWaiveTransitiveViolationsMock,
          toggleViewTransitiveViolationWaivers: toggleViewTransitiveViolationWaiversMock,
        },
        '../componentDetails/ViolationsTableTile/policyViolationsSlice': {
          actions: {
            setSelectedPolicyViolationId: setSelectedPolicyViolationIdMock,
            toggleShowViolationsDetailPopover: toggleShowViolationsDetailPopoverMock,
          },
        },
        '../waivers/waiverActions': {
          setWaiverToDelete: setWaiverToDeleteMock,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <TransitiveViolationsPageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerType', 'someOwnerType');
    expect(wrapper).toHaveProp('ownerId', 'someOwnerId');
    expect(wrapper).toHaveProp('scanId', 'someScanId');
    expect(wrapper).toHaveProp('hash', 'someHash');
    expect(wrapper).toHaveProp('availableScopes', 'someAvailableScopes');
    expect(wrapper).toHaveProp('reportMetadata', 'someReportMetadata');
    expect(wrapper).toHaveProp('componentTransitivePolicyViolations', 'someComponentTransitivePolicyViolations');
    expect(wrapper).toHaveProp('transitiveViolationWaivers', 'someTransitiveViolationWaivers');
    expect(wrapper).toHaveProp('isRequestWaiveTransitiveViolationsOpen', 'someIsRequestWaiveTransitiveViolationsOpen');
    expect(wrapper).toHaveProp('isWaiveTransitiveViolationsOpen', 'someIsWaiveTransitiveViolationsOpen');
    expect(wrapper).toHaveProp('isViewTransitiveViolationWaiversOpen', 'someIsViewTransitiveViolationWaiversOpen');
    expect(wrapper).toHaveProp('showViolationsDetailPopover', 'someValueToShowThePopover');
    expect(wrapper).toHaveProp('waiverToDelete', 'someWaiverToDelete');
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

    const loadTransitiveViolationWaiversActionCreator = wrapper.prop('loadTransitiveViolationWaivers');
    expect(loadTransitiveViolationWaiversActionCreator).toEqual(jasmine.any(Function));
    loadTransitiveViolationWaiversActionCreator('test');
    expect(store.getActions()[3]).toEqual({ type: 'BAR4' });

    const setSortingParametersActionCreator = wrapper.prop('setSortingParameters');
    expect(setSortingParametersActionCreator).toEqual(jasmine.any(Function));
    setSortingParametersActionCreator('test');
    expect(store.getActions()[4]).toEqual({ type: 'BAR5' });

    const setFilteringParametersActionCreator = wrapper.prop('setFilteringParameters');
    expect(setFilteringParametersActionCreator).toEqual(jasmine.any(Function));
    setFilteringParametersActionCreator('test');
    expect(store.getActions()[5]).toEqual({ type: 'BAR6' });

    const toggleRequestWaiveTransitiveViolationsActionCreator = wrapper.prop('toggleRequestWaiveTransitiveViolations');
    expect(toggleRequestWaiveTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    toggleRequestWaiveTransitiveViolationsActionCreator('test');
    expect(store.getActions()[6]).toEqual({ type: 'BAR7' });

    const toggleWaiveTransitiveViolationsActionCreator = wrapper.prop('toggleWaiveTransitiveViolations');
    expect(toggleWaiveTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    toggleWaiveTransitiveViolationsActionCreator('test');
    expect(store.getActions()[7]).toEqual({ type: 'BAR8' });

    const toggleViewTransitiveViolationWaiversActionCreator = wrapper.prop('toggleViewTransitiveViolationWaivers');
    expect(toggleViewTransitiveViolationWaiversActionCreator).toEqual(jasmine.any(Function));
    toggleViewTransitiveViolationWaiversActionCreator('test');
    expect(store.getActions()[8]).toEqual({ type: 'BAR9' });

    const setSelectedPolicyViolationIdActionCreator = wrapper.prop('setSelectedPolicyViolationId');
    expect(setSelectedPolicyViolationIdActionCreator).toEqual(jasmine.any(Function));
    setSelectedPolicyViolationIdActionCreator('test');
    expect(store.getActions()[9]).toEqual({ type: 'BAR10' });

    const toggleShowViolationsDetailPopoverActionCreator = wrapper.prop('toggleShowViolationsDetailPopover');
    expect(toggleShowViolationsDetailPopoverActionCreator).toEqual(jasmine.any(Function));
    toggleShowViolationsDetailPopoverActionCreator('test');
    expect(store.getActions()[10]).toEqual({ type: 'BAR11' });

    const setWaiverToDeleteActionCreator = wrapper.prop('setWaiverToDelete');
    expect(setWaiverToDeleteActionCreator).toEqual(jasmine.any(Function));
    setWaiverToDeleteActionCreator('test');
    expect(store.getActions()[11]).toEqual({ type: 'BAR12' });
  });

  it('renders the TransitiveViolationsPage component', function () {
    const transitiveViolationsPage = shallow(vdom).find(TransitiveViolationsPage);
    expect(transitiveViolationsPage).toExist();
  });
});
