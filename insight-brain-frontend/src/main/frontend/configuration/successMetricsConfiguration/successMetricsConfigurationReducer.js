/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import { createReducerFromActionMap } from '../../util/reduxUtil';
import { pathSet } from '../../util/jsUtil';
import {
  SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED,
  SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED,
  SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED,
  SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED,
  SUCCESS_METRICS_CONFIGURATION_RESET_FORM,
  SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE,
} from './successMetricsConfigurationActions';

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

const clearedErrors = pick(['loadError', 'updateError'], initialState.viewState);

function loadRequested() {
  return initialState;
}

function loadFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      ...clearedErrors,
    },
    formState: payload,
    serverData: payload,
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
    formState: state.serverData,
    viewState: {
      ...state.viewState,
      updateError: null,
      isDirty: false,
    },
  };
}

function toggleIsSuccessMetricsEnabled(_, state) {
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

function resetSubmitMaskState(_, state) {
  return pathSet(['viewState', 'submitMaskState'], null, state);
}

const reducerActionMap = {
  [SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED]: loadRequested,
  [SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED]: loadFulfilled,
  [SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED]: loadFailed,
  [SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED]: updateRequested,
  [SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED]: updateFulfilled,
  [SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED]: updateFailed,
  [SUCCESS_METRICS_CONFIGURATION_RESET_FORM]: resetForm,
  [SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED]: toggleIsSuccessMetricsEnabled,
  [SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE]: resetSubmitMaskState,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
