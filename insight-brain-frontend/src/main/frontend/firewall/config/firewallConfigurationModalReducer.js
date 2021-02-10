/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {always, equals, lensPath, not, over} from 'ramda';

import {createReducerFromActionMap} from '../../util/reduxUtil';
import {
  FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FAILED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SAVE_CONFIGURATION_REQUESTED,
  FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED
} from '../firewallActions';
import {pathSet} from '../../util/jsUtil';

const initialState = Object.freeze({
  viewState: Object.freeze({
    sumbitMaskSuccessState: null,
    saveConfigurationError: null,
    loadedConfiguration: false,
    loadConfigurationError: null,
    isDirty: false
  }),
  serverState: Object.freeze({
    autoUnquarantineEnabled: false
  }),
  formState: Object.freeze({
    autoUnquarantineEnabled: false
  })
});

const saveConfigurationRequested = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    sumbitMaskSuccessState: false,
    saveConfigurationError: null
  }
});

const saveConfigurationFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    saveConfigurationError: null,
    sumbitMaskSuccessState: true
  }
});

const saveConfigurationFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    sumbitMaskSuccessState: null,
    saveConfigurationError: payload
  }
});

const loadConfigurationFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedConfiguration: true,
    loadConfigurationError: null,
    isDirty: false
  },
  serverState: payload,
  formState: payload
});

const loadConfigurationFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedConfiguration: true,
    loadConfigurationError: payload
  }
});

const toggleAutoUnquarantineEnabled = (_, state) => {
  const newState = over(lensPath(['formState', 'autoUnquarantineEnabled']), not, state);
  return pathSet(['viewState', 'isDirty'], isConfigurationChanged(newState), newState);
};

const configurationSaveMaskTimerDone = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    sumbitMaskSuccessState: null,
    saveConfigurationError: null
  }
});

const reducerActionMap = {
  [FIREWALL_SAVE_CONFIGURATION_REQUESTED]: saveConfigurationRequested,
  [FIREWALL_SAVE_CONFIGURATION_FAILED]: saveConfigurationFailed,
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_REQUESTED]: always(initialState),
  [FIREWALL_LOAD_CONFIGURATION_FULFILLED]: loadConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_FAILED]: loadConfigurationFailed,
  [FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED]: toggleAutoUnquarantineEnabled,
  [FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE]: configurationSaveMaskTimerDone
};

function isConfigurationChanged(state) {
  const {formState, serverState} = state;
  return !equals(formState, serverState);
}

const firewallConfigurationModalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default firewallConfigurationModalReducer;
