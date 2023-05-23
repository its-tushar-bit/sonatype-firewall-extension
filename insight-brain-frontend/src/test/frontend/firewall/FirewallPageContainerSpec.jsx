/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import FirewallPage from '../../../main/frontend/firewall/FirewallPage';

describe('FirewallPageContainer', function () {
  let FirewallPageContainer,
    closeWelcomeModalMock,
    initializeWelcomeModalMock,
    loadFirewallDataMock,
    openConfigurationModalMock,
    loadQuarantineListMock,
    goToRepositoryComponentDetailsPageMock,
    setQuarantineGridPolicyFilterMock,
    setQuarantineGridComponentNameFilterMock,
    store,
    state,
    vdom;

  beforeEach(function () {
    closeWelcomeModalMock = jasmine.createSpy('closeWelcomeModalMock').and.returnValue({
      type: 'FIREWALL_SET_SHOW_WELCOME_MODAL',
      payload: false,
    });

    initializeWelcomeModalMock = jasmine.createSpy('initializeWelcomeModalMock').and.returnValue({
      type: 'FIREWALL_SET_SHOW_WELCOME_MODAL',
      payload: true,
    });

    loadFirewallDataMock = jasmine.createSpy('loadFirewallDataMock').and.returnValue({
      type: 'LOAD_FIREWALL_DATA',
    });

    openConfigurationModalMock = jasmine.createSpy('openConfigurationModalMock').and.returnValue({
      type: 'OPEN_FIREWALL_CONFIGURATION',
    });

    loadQuarantineListMock = jasmine.createSpy('loadQuarantineListMock').and.returnValue({
      type: 'LOAD_QUARANTINE_LIST',
    });

    goToRepositoryComponentDetailsPageMock = jasmine
      .createSpy('goToRepositoryComponentDetailsPageMock')
      .and.returnValue({
        type: 'firewall.componentDetailsPage',
      });

    setQuarantineGridPolicyFilterMock = jasmine.createSpy('setQuarantineGridPolicyFilterMock').and.returnValue({
      type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER',
    });

    setQuarantineGridComponentNameFilterMock = jasmine
      .createSpy('setQuarantineGridComponentNameFilterMock')
      .and.returnValue({
        type: 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER',
      });

    FirewallPageContainer = require('inject-loader!../../../main/frontend/firewall/FirewallPageContainer')({
      './firewallActions': {
        closeWelcomeModal: closeWelcomeModalMock,
        initializeWelcomeModal: initializeWelcomeModalMock,
        loadFirewallData: loadFirewallDataMock,
        openConfigurationModal: openConfigurationModalMock,
        loadQuarantineList: loadQuarantineListMock,
        goToRepositoryComponentDetailsPage: goToRepositoryComponentDetailsPageMock,
        setQuarantineGridPolicyFilter: setQuarantineGridPolicyFilterMock,
        setQuarantineGridComponentNameFilter: setQuarantineGridComponentNameFilterMock,
      },
    }).default;

    state = {
      loadError: 'this is not the error',
      firewall: {
        showWelcomeModal: false,
        viewState: {
          isShowConfigurationModal: false,
          loadError: null,
        },
        autoUnquarantineState: {
          viewState: {
            loadedConfiguration: false,
            loadedReleaseQuarantineSummary: false,
            autoReleaseQuarantineCountMTD: '-',
            enabledPolicyConditionTypesCount: 0,
            totalPolicyConditionTypesCount: 1,
          },
        },
        configurationState: {
          autoUnquarantineEnabled: false,
        },
        quarantineSummaryState: {
          viewState: {
            loadedQuarantineSummary: false,
            quarantineEnabled: false,
            quarantineEnabledRepositoryCount: 0,
            repositoryCount: 0,
            totalComponentCount: 0,
            quarantinedComponentCount: 0,
          },
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <FirewallPageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('showWelcomeModal', false);
    expect(wrapper).toHaveProp('isShowConfigurationModal', false);
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('loadedConfiguration', false);
    expect(wrapper).toHaveProp('loadedReleaseQuarantineSummary', false);
    expect(wrapper).toHaveProp('autoReleaseQuarantineCountMTD', '-');
    expect(wrapper).toHaveProp('enabledPolicyConditionTypesCount', 0);
    expect(wrapper).toHaveProp('totalPolicyConditionTypesCount', 1);
    expect(wrapper).toHaveProp('autoUnquarantineEnabled', false);
    expect(wrapper).toHaveProp('loadedQuarantineSummary', false);
    expect(wrapper).toHaveProp('quarantineEnabled', false);
    expect(wrapper).toHaveProp('quarantineEnabledRepositoryCount', 0);
    expect(wrapper).toHaveProp('repositoryCount', 0);
    expect(wrapper).toHaveProp('totalComponentCount', 0);
    expect(wrapper).toHaveProp('quarantinedComponentCount', 0);

    state = {
      ...state,
      firewall: {
        ...state.firewall,
        showWelcomeModal: true,
        viewState: {
          ...state.firewall.viewState,
          isShowConfigurationModal: true,
          loadError: 'error',
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
        quarantineSummaryState: {
          ...state.firewall.quarantineSummaryState,
          viewState: {
            ...state.firewall.quarantineSummaryState.viewState,
            loadedQuarantineSummary: true,
            quarantineEnabled: true,
            quarantineEnabledRepositoryCount: 1,
            repositoryCount: 1,
            totalComponentCount: 1,
            quarantinedComponentCount: 1,
          },
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('showWelcomeModal', true);
    expect(wrapper).toHaveProp('isShowConfigurationModal', true);
    expect(wrapper).toHaveProp('loadError', 'error');
    expect(wrapper).toHaveProp('loadedConfiguration', true);
    expect(wrapper).toHaveProp('loadedReleaseQuarantineSummary', true);
    expect(wrapper).toHaveProp('autoReleaseQuarantineCountMTD', 5);
    expect(wrapper).toHaveProp('enabledPolicyConditionTypesCount', 1);
    expect(wrapper).toHaveProp('totalPolicyConditionTypesCount', 2);
    expect(wrapper).toHaveProp('autoUnquarantineEnabled', true);
    expect(wrapper).toHaveProp('loadedQuarantineSummary', true);
    expect(wrapper).toHaveProp('quarantineEnabled', true);
    expect(wrapper).toHaveProp('quarantineEnabledRepositoryCount', 1);
    expect(wrapper).toHaveProp('repositoryCount', 1);
    expect(wrapper).toHaveProp('totalComponentCount', 1);
    expect(wrapper).toHaveProp('quarantinedComponentCount', 1);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      initializeWelcomeModalActionCreator = wrapper.prop('initializeWelcomeModal'),
      closeWelcomeModalActionCreator = wrapper.prop('closeWelcomeModal'),
      loadFirewallDataActionCreator = wrapper.prop('loadFirewallData'),
      openConfigurationModalActionCreator = wrapper.prop('openConfigurationModal'),
      loadQuarantineListActionCreator = wrapper.prop('loadQuarantineList'),
      goToRepositoryComponentDetailsPageActionCreator = wrapper.prop('goToRepositoryComponentDetailsPage'),
      setQuarantineGridPolicyFilterActionCreator = wrapper.prop('setQuarantineGridPolicyFilter'),
      setQuarantineGridComponentNameFilterActionCreator = wrapper.prop('setQuarantineGridComponentNameFilter');

    expect(initializeWelcomeModalActionCreator).toEqual(jasmine.any(Function));
    expect(closeWelcomeModalActionCreator).toEqual(jasmine.any(Function));
    expect(loadFirewallDataActionCreator).toEqual(jasmine.any(Function));
    expect(openConfigurationModalActionCreator).toEqual(jasmine.any(Function));
    expect(loadQuarantineListActionCreator).toEqual(jasmine.any(Function));
    expect(goToRepositoryComponentDetailsPageActionCreator).toEqual(jasmine.any(Function));
    expect(setQuarantineGridPolicyFilterActionCreator).toEqual(jasmine.any(Function));
    expect(setQuarantineGridComponentNameFilterActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadFirewallDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_FIREWALL_DATA' }]);

    loadQuarantineListActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_FIREWALL_DATA' }, { type: 'LOAD_QUARANTINE_LIST' }]);

    openConfigurationModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
    ]);

    goToRepositoryComponentDetailsPageActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'firewall.componentDetailsPage' },
    ]);

    setQuarantineGridPolicyFilterActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'firewall.componentDetailsPage' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER' },
    ]);

    setQuarantineGridComponentNameFilterActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'firewall.componentDetailsPage' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER' },
    ]);

    initializeWelcomeModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'firewall.componentDetailsPage' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER' },
      { type: 'FIREWALL_SET_SHOW_WELCOME_MODAL', payload: true },
    ]);

    closeWelcomeModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_FIREWALL_DATA' },
      { type: 'LOAD_QUARANTINE_LIST' },
      { type: 'OPEN_FIREWALL_CONFIGURATION' },
      { type: 'firewall.componentDetailsPage' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER' },
      { type: 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER' },
      { type: 'FIREWALL_SET_SHOW_WELCOME_MODAL', payload: true },
      { type: 'FIREWALL_SET_SHOW_WELCOME_MODAL', payload: false },
    ]);
  });

  it('renders Firewall component', function () {
    const firewallPage = shallow(vdom).find(FirewallPage);

    expect(firewallPage).toExist();
    expect(firewallPage).toHaveProp('loadError', null);
  });
});
