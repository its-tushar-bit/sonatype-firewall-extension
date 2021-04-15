/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import FirewallConfigurationModal from '../../../../main/frontend/firewall/config/FirewallConfigurationModal';
import { INTEGRITY_RATING_POLICY_TYPE_ID } from '../../../../main/frontend/firewall/config/firewallConfigurationModalReducer';

describe('FirewallConfigurationModalContainer', function () {
  let FirewallConfigurationModalContainer,
    toggleAutoUnquarantineEnabledMock,
    saveConfigurationMock,
    loadConfigurationMock,
    closeConfigurationModalMock,
    store,
    state,
    vdom;

  beforeEach(function () {
    toggleAutoUnquarantineEnabledMock = jasmine.createSpy('toggleAutoUnquarantineEnabledMock').and.returnValue({
      type: 'TOGGLE_FIREWALL_AUTO_RELEASE_ENABLED',
    });

    saveConfigurationMock = jasmine.createSpy('saveConfigurationMock').and.returnValue({
      type: 'SAVE_FIREWALL_CONFIGURATION',
    });

    loadConfigurationMock = jasmine.createSpy('loadConfigurationMock').and.returnValue({
      type: 'LOAD_FIREWALL_CONFIGURATION',
    });

    closeConfigurationModalMock = jasmine.createSpy('closeConfigurationModalMock').and.returnValue({
      type: 'CLOSE_FIREWALL_CONFIGURATION_MODAL',
    });

    FirewallConfigurationModalContainer = require('inject-loader!../../../../main/frontend/firewall/config/FirewallConfigurationModalContainer')(
      {
        '../firewallActions': {
          toggleAutoUnquarantineEnabled: toggleAutoUnquarantineEnabledMock,
          saveConfiguration: saveConfigurationMock,
          loadConfiguration: loadConfigurationMock,
          closeConfigurationModal: closeConfigurationModalMock,
        },
      }
    ).default;

    state = {
      submitMaskSuccessState: false,
      firewall: {
        autoUnquarantineState: {
          viewState: {
            loadedConfiguration: false,
            loadConfigurationError: null,
          },
        },
      },
      firewallConfigurationModal: {
        viewState: {
          submitMaskSuccessState: null,
          saveConfigurationError: null,
          isDirty: false,
        },
        formState: {
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              autoReleaseQuarantineEnabled: false,
            },
          ],
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <FirewallConfigurationModalContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadedConfiguration', false);
    expect(wrapper).toHaveProp('loadConfigurationError', null);
    expect(wrapper).toHaveProp('submitMaskSuccessState', null);
    expect(wrapper).toHaveProp('saveConfigurationError', null);
    expect(wrapper).toHaveProp('isDirty', false);
    expect(wrapper).toHaveProp('conditionTypes', [
      {
        id: INTEGRITY_RATING_POLICY_TYPE_ID,
        autoReleaseQuarantineEnabled: false,
      },
    ]);

    state = {
      ...state,
      firewall: {
        ...state.firewall,
        autoUnquarantineState: {
          ...state.firewall.autoUnquarantineState,
          viewState: {
            ...state.firewall.autoUnquarantineState.viewState,
            loadedConfiguration: true,
            loadConfigurationError: 'error',
          },
        },
      },
      firewallConfigurationModal: {
        ...state.firewallConfigurationModal,
        viewState: {
          ...state.firewallConfigurationModal.viewState,
          submitMaskSuccessState: true,
          saveConfigurationError: 'error',
          isDirty: true,
        },
        formState: {
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              autoReleaseQuarantineEnabled: true,
            },
          ],
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadedConfiguration', true);
    expect(wrapper).toHaveProp('loadConfigurationError', 'error');
    expect(wrapper).toHaveProp('submitMaskSuccessState', true);
    expect(wrapper).toHaveProp('saveConfigurationError', 'error');
    expect(wrapper).toHaveProp('isDirty', true);
    expect(wrapper).toHaveProp('conditionTypes', [
      {
        id: INTEGRITY_RATING_POLICY_TYPE_ID,
        autoReleaseQuarantineEnabled: true,
      },
    ]);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive();
    const toggleAutoUnquarantineEnabledActionCreator = wrapper.prop('toggleAutoUnquarantineEnabled');
    const saveConfigurationActionCreator = wrapper.prop('saveConfiguration');
    const loadConfigurationActionCreator = wrapper.prop('loadConfiguration');
    const closeConfigurationModalActionCreator = wrapper.prop('closeConfigurationModal');

    expect(toggleAutoUnquarantineEnabledActionCreator).toEqual(jasmine.any(Function));
    expect(saveConfigurationActionCreator).toEqual(jasmine.any(Function));
    expect(loadConfigurationActionCreator).toEqual(jasmine.any(Function));
    expect(closeConfigurationModalActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    toggleAutoUnquarantineEnabledActionCreator();
    expect(store.getActions()).toEqual([{ type: 'TOGGLE_FIREWALL_AUTO_RELEASE_ENABLED' }]);

    saveConfigurationActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'TOGGLE_FIREWALL_AUTO_RELEASE_ENABLED' },
      { type: 'SAVE_FIREWALL_CONFIGURATION' },
    ]);

    loadConfigurationActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'TOGGLE_FIREWALL_AUTO_RELEASE_ENABLED' },
      { type: 'SAVE_FIREWALL_CONFIGURATION' },
      { type: 'LOAD_FIREWALL_CONFIGURATION' },
    ]);

    closeConfigurationModalActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'TOGGLE_FIREWALL_AUTO_RELEASE_ENABLED' },
      { type: 'SAVE_FIREWALL_CONFIGURATION' },
      { type: 'LOAD_FIREWALL_CONFIGURATION' },
      { type: 'CLOSE_FIREWALL_CONFIGURATION_MODAL' },
    ]);
  });

  it('renders FirewallConfigurationModal component', function () {
    const firewallConfigurationModal = shallow(vdom).find(FirewallConfigurationModal);
    expect(firewallConfigurationModal).toExist();
    expect(firewallConfigurationModal).toHaveProp('submitMaskSuccessState', null);
  });
});
