/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {createReducerFromActionMap} from '../util/reduxUtil';
import {
  FIREWALL_LOAD_STATUS_FAILED,
  FIREWALL_LOAD_STATUS_FULFILLED,
  FIREWALL_LOAD_STATUS_REQUESTED,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL
} from './firewallActions';
import {always} from 'ramda';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadedStatus: false,
    loadStatusError: null,
    isShowConfigurationModal: false
  }),
  configurationState: Object.freeze({
    isEnabled: false
  })
});

const loadStatusFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: null
  },
  configurationState: {
    ...state.configurationState,
    isEnabled: payload.experimentalFeatures.firewallAutoUnquarantine
  }
});

const loadStatusFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadedStatus: true,
    loadStatusError: payload
  }
});

const setShowConfigurationModal = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    isShowConfigurationModal: payload
  }
});

const reducerActionMap = {
  [FIREWALL_LOAD_STATUS_FAILED]: loadStatusFailed,
  [FIREWALL_LOAD_STATUS_FULFILLED]: loadStatusFulfilled,
  [FIREWALL_LOAD_STATUS_REQUESTED]: always(initialState),
  [FIREWALL_SET_SHOW_CONFIGURATION_MODAL]: setShowConfigurationModal
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
