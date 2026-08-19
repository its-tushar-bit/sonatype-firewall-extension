/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, equals, lensPath, not, over } from 'ramda';

import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FAILED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SAVE_CONFIGURATION_REQUESTED,
  FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED,
} from '../firewallActions';
import { pathSet } from '../../util/jsUtil';

export const INTEGRITY_RATING_POLICY_TYPE_ID = 'IntegrityRating';

const initialState = Object.freeze({
  viewState: Object.freeze({
    submitMaskSuccessState: null,
    saveConfigurationError: null,
    isDirty: false,
  }),
  serverState: Object.freeze({
    conditionTypes: [
      {
        id: INTEGRITY_RATING_POLICY_TYPE_ID,
        autoReleaseQuarantineEnabled: false,
      },
    ],
  }),
  formState: Object.freeze({
    conditionTypes: [
      {
        id: INTEGRITY_RATING_POLICY_TYPE_ID,
        autoReleaseQuarantineEnabled: false,
      },
    ],
  }),
});

const saveConfigurationRequested = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    submitMaskSuccessState: false,
    saveConfigurationError: null,
  },
});

const saveConfigurationFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    submitMaskSuccessState: true,
  },
});

const saveConfigurationFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    submitMaskSuccessState: null,
    saveConfigurationError: payload,
  },
});

const loadConfigurationFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    isDirty: false,
  },
  serverState: {
    conditionTypes: payload,
  },
  formState: {
    conditionTypes: payload,
  },
});

const toggleAutoUnquarantineEnabled = (payload, state) => {
  const index = state.formState.conditionTypes.findIndex((element) => element.id === payload);
  const newState = over(lensPath(['formState', 'conditionTypes', index, 'autoReleaseQuarantineEnabled']), not, state);
  return updatedComputedProps(newState);
};

const updatedComputedProps = (state) => {
  return pathSet(['viewState', 'isDirty'], isConfigurationChanged(state), state);
};

const configurationSaveMaskTimerDone = (_, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    submitMaskSuccessState: null,
    saveConfigurationError: null,
  },
});

const reducerActionMap = {
  [FIREWALL_SAVE_CONFIGURATION_REQUESTED]: saveConfigurationRequested,
  [FIREWALL_SAVE_CONFIGURATION_FAILED]: saveConfigurationFailed,
  [FIREWALL_SAVE_CONFIGURATION_FULFILLED]: saveConfigurationFulfilled,
  [FIREWALL_LOAD_CONFIGURATION_REQUESTED]: always(initialState),
  [FIREWALL_LOAD_CONFIGURATION_FULFILLED]: loadConfigurationFulfilled,
  [FIREWALL_TOGGLE_AUTO_UNQUARANTINE_ENABLED]: toggleAutoUnquarantineEnabled,
  [FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE]: configurationSaveMaskTimerDone,
};

function isConfigurationChanged(state) {
  const { formState, serverState } = state;
  return !equals(formState, serverState);
}

const firewallConfigurationModalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default firewallConfigurationModalReducer;
