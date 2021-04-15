/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import Firewall from '../../../main/frontend/firewall/Firewall';

describe('FirewallContainer', function () {
  let FirewallContainer, loadDataMock, openConfigurationModalMock, store, state, vdom;

  beforeEach(function () {
    loadDataMock = jasmine.createSpy('loadDataMock').and.returnValue({
      type: 'LOAD_FIREWALL_DATA',
    });

    openConfigurationModalMock = jasmine.createSpy('openConfigurationModalMock').and.returnValue({
      type: 'OPEN_FIREWALL_CONFIGURATION',
    });

    FirewallContainer = require('inject-loader!../../../main/frontend/firewall/FirewallContainer')({
      './firewallActions': {
        loadData: loadDataMock,
        openConfigurationModal: openConfigurationModalMock,
      },
    }).default;

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
    vdom = <FirewallContainer store={store} />;
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
    expect(wrapper).toHaveProp('loadedQuarantineSummary', true);
    expect(wrapper).toHaveProp('quarantineEnabled', true);
    expect(wrapper).toHaveProp('quarantineEnabledRepositoryCount', 1);
    expect(wrapper).toHaveProp('repositoryCount', 1);
    expect(wrapper).toHaveProp('totalComponentCount', 1);
    expect(wrapper).toHaveProp('quarantinedComponentCount', 1);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive();
    const loadDataActionCreator = wrapper.prop('loadData');
    const openConfigurationModalActionCreator = wrapper.prop('openConfigurationModal');

    expect(loadDataActionCreator).toEqual(jasmine.any(Function));
    expect(openConfigurationModalActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_FIREWALL_DATA' }]);

    openConfigurationModalActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_FIREWALL_DATA' }, { type: 'OPEN_FIREWALL_CONFIGURATION' }]);
  });

  it('renders Firewall component', function () {
    const firewall = shallow(vdom).find(Firewall);
    expect(firewall).toExist();
    expect(firewall).toHaveProp('loadError', null);
  });
});
