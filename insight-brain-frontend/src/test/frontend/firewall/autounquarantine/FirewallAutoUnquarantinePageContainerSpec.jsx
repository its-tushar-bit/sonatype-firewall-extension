/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import FirewallAutoUnquarantinePage from '../../../../main/frontend/firewall/autounquarantine/FirewallAutoUnquarantinePage';

describe('FirewallAutoUnquarantinePageContainer', function () {
  let FirewallAutoUnquarantinePageContainer,
    loadAutoUnquarantineDataMock,
    loadReleaseQuarantineListMock,
    openConfigurationModalMock,
    selectReleaseQuarantineComponentMock,
    store,
    state,
    vdom;

  beforeEach(function () {
    loadAutoUnquarantineDataMock = jasmine.createSpy('loadAutoUnquarantineDataMock').and.returnValue({
      type: 'AUTO_UNQUARANTINE_LOAD_DATA_REQUESTED',
    });

    loadReleaseQuarantineListMock = jasmine.createSpy('loadReleaseQuarantineListMock').and.returnValue({
      type: 'LOAD_RELEASE_QUARANTINE_LIST',
    });

    openConfigurationModalMock = jasmine.createSpy('openConfigurationModalMock').and.returnValue({
      type: 'OPEN_FIREWALL_CONFIGURATION',
    });

    selectReleaseQuarantineComponentMock = jasmine.createSpy('selectReleaseQuarantineComponentMock').and.returnValue({
      type: 'SELECT_RELEASE_QUARANTINE_COMPONENT',
    });

    FirewallAutoUnquarantinePageContainer = require('inject-loader!../../../../main/frontend/firewall/autounquarantine/FirewallAutoUnquarantinePageContainer')(
      {
        '../firewallActions': {
          loadAutoUnquarantineData: loadAutoUnquarantineDataMock,
          loadReleaseQuarantineList: loadReleaseQuarantineListMock,
          openConfigurationModal: openConfigurationModalMock,
          selectReleaseQuarantineComponent: selectReleaseQuarantineComponentMock,
        },
      }
    ).default;

    state = {
      loadError: 'this is not the error',
      firewall: {
        viewState: {
          loadedStatus: false,
          isShowConfigurationModal: false,
          loadError: null,
        },
        statusState: {
          isEnabled: false,
        },
        autoUnquarantineState: {
          viewState: {
            loadedConfiguration: false,
            loadedReleaseQuarantineSummary: false,
            autoReleaseQuarantineCountMTD: '-',
            autoReleaseQuarantineCountYTD: '-',
            enabledPolicyConditionTypesCount: 0,
            totalPolicyConditionTypesCount: 1,
          },
        },
        configurationState: {
          autoUnquarantineEnabled: false,
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <FirewallAutoUnquarantinePageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadedStatus', false);
    expect(wrapper).toHaveProp('isShowConfigurationModal', false);
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('isEnabled', false);
    expect(wrapper).toHaveProp('loadedConfiguration', false);
    expect(wrapper).toHaveProp('loadedReleaseQuarantineSummary', false);
    expect(wrapper).toHaveProp('autoReleaseQuarantineCountMTD', '-');
    expect(wrapper).toHaveProp('autoReleaseQuarantineCountYTD', '-');
    expect(wrapper).toHaveProp('enabledPolicyConditionTypesCount', 0);
    expect(wrapper).toHaveProp('totalPolicyConditionTypesCount', 1);
    expect(wrapper).toHaveProp('autoUnquarantineEnabled', false);

    state = {
      ...state,
      firewall: {
        ...state.firewall,
        viewState: {
          ...state.firewall.viewState,
          loadedStatus: true,
          isShowConfigurationModal: true,
          loadError: 'error',
        },
        statusState: {
          ...state.firewall.statusState,
          isEnabled: true,
        },
        autoUnquarantineState: {
          ...state.firewall.autoUnquarantineState,
          viewState: {
            ...state.firewall.autoUnquarantineState.viewState,
            loadedConfiguration: true,
            loadedReleaseQuarantineSummary: true,
            autoReleaseQuarantineCountMTD: 5,
            enabledPolicyConditionTypesCount: 1,
            totalPolicyConditionTypesCount: 2,
          },
        },
        configurationState: {
          ...state.firewall.configurationState,
          autoUnquarantineEnabled: true,
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadedStatus', true);
    expect(wrapper).toHaveProp('isShowConfigurationModal', true);
    expect(wrapper).toHaveProp('loadError', 'error');
    expect(wrapper).toHaveProp('isEnabled', true);
    expect(wrapper).toHaveProp('loadedConfiguration', true);
    expect(wrapper).toHaveProp('loadedReleaseQuarantineSummary', true);
    expect(wrapper).toHaveProp('autoReleaseQuarantineCountMTD', 5);
    expect(wrapper).toHaveProp('enabledPolicyConditionTypesCount', 1);
    expect(wrapper).toHaveProp('totalPolicyConditionTypesCount', 2);
    expect(wrapper).toHaveProp('autoUnquarantineEnabled', true);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      loadAutoUnquarantineDataActionCreator = wrapper.prop('loadAutoUnquarantineData'),
      loadReleaseQuarantineListActionCreator = wrapper.prop('loadReleaseQuarantineList'),
      openConfigurationModalActionCreator = wrapper.prop('openConfigurationModal'),
      selectReleaseQuarantineComponentActionCreator = wrapper.prop('selectReleaseQuarantineComponent');

    expect(loadAutoUnquarantineDataActionCreator).toEqual(jasmine.any(Function));
    expect(loadReleaseQuarantineListActionCreator).toEqual(jasmine.any(Function));
    expect(openConfigurationModalActionCreator).toEqual(jasmine.any(Function));
    expect(selectReleaseQuarantineComponentActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadAutoUnquarantineDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'AUTO_UNQUARANTINE_LOAD_DATA_REQUESTED' }]);

    loadReleaseQuarantineListActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'AUTO_UNQUARANTINE_LOAD_DATA_REQUESTED' },
      { type: 'LOAD_RELEASE_QUARANTINE_LIST' },
    ]);

    openConfigurationModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'AUTO_UNQUARANTINE_LOAD_DATA_REQUESTED' },
      { type: 'LOAD_RELEASE_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
    ]);

    selectReleaseQuarantineComponentActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'AUTO_UNQUARANTINE_LOAD_DATA_REQUESTED' },
      { type: 'LOAD_RELEASE_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'SELECT_RELEASE_QUARANTINE_COMPONENT' },
    ]);
  });

  it('renders FirewallAutoUnquarantinePage component', function () {
    const firewallAutoUnquarantinePage = shallow(vdom).find(FirewallAutoUnquarantinePage);
    expect(firewallAutoUnquarantinePage).toExist();
    expect(firewallAutoUnquarantinePage).toHaveProp('loadError', null);
  });
});
