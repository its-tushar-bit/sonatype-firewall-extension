/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';
import { pathSet } from '../../util/jsUtil';
import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED,
  AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE,
} from './automaticSourceControlConfigurationActions';

const initialState = Object.freeze({
  formState: {
    enabled: false,
  },
  viewState: {
    loading: true,
    loadError: null,
    updateError: null,
    submitMaskState: null,
    isDirty: false,
  },
  serverData: null,
});

const clearedErrors = pick(['loadError', 'updatedError'], initialState);

function loadRequested() {
  return { ...initialState };
}

function loadFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      ...clearedErrors,
    },
    formState: { ...payload },
    serverData: { ...payload },
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      loadError: payload,
    },
  };
}

function toggleAutomaticSourceControlConfigurationEnabled(_, state) {
  const enabled = !state.formState.enabled;
  return {
    ...state,
    formState: {
      ...state.formState,
      enabled,
    },
    viewState: {
      ...state.viewState,
      isDirty: enabled !== state.serverData.enabled,
    },
  };
}

function updateRequested(_, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      submitMaskState: false,
      ...clearedErrors,
    },
  };
}

function updateFulfilled(_, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      submitMaskState: true,
      isDirty: false,
      updateError: null,
      ...clearedErrors,
    },
    serverData: state.formState,
  };
}

function updateFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      updateError: payload,
      submitMaskState: null,
    },
  };
}

function resetForm(_, state) {
  return {
    ...state,
    formState: { ...state.serverData },
    viewState: {
      ...state.viewState,
      isDirty: false,
      updateError: null,
    },
  };
}

function resetSubmitMaskState(_, state) {
  return pathSet(['viewState', 'submitMaskState'], null, state);
}

const reducerActionMap = {
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FULFILLED]: loadFulfilled,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_REQUESTED]: loadRequested,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_LOAD_FAIL]: loadFailed,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_TOGGLE_ENABLED]: toggleAutomaticSourceControlConfigurationEnabled,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_REQUESTED]: updateRequested,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FULFILLED]: updateFulfilled,
  [SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE]: resetSubmitMaskState,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_RESET_FORM]: resetForm,
  [AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_UPDATE_FAILED]: updateFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
