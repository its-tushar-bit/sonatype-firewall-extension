/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {createReducerFromActionMap} from '../util/reduxUtil';
import {
  FIREWALL_LOAD_STATUS_FAILED,
  FIREWALL_LOAD_STATUS_FULFILLED,
  FIREWALL_LOAD_STATUS_REQUESTED
} from './firewallActions';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadingStatus: false,
    loadStatusError: null
  }),
  configurationState: Object.freeze({
    isEnabled: false
  })
});

function loadStatusRequested(payload, state) {
  return {
    ...initialState,
    viewState: {
      ...initialState.viewState,
      loadingStatus: true
    },
    configurationState: {
      ...state.configurationState
    }
  };
}

function loadStatusFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingStatus: false,
      loadStatusError: null
    },
    configurationState: {
      ...state.configurationState,
      isEnabled: payload.experimentalFeatures.firewallAutoUnquarantine
    }
  };
}

function loadStatusFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loadingStatus: false,
      loadStatusError: payload
    }
  };
}

const reducerActionMap = {
  [FIREWALL_LOAD_STATUS_FAILED]: loadStatusFailed,
  [FIREWALL_LOAD_STATUS_FULFILLED]: loadStatusFulfilled,
  [FIREWALL_LOAD_STATUS_REQUESTED]: loadStatusRequested
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
