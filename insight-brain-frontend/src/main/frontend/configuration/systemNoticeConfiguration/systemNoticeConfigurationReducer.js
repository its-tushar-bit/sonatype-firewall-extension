/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

import { createReducerFromActionMap } from '../../util/reduxUtil';
import { pathSet } from '../../util/jsUtil';
import {
  SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED,
  SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED,
  SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED,
  SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED,
  SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED,
  SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED,
  SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED,
  SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE,
  SYSTEM_NOTICE_CONFIGURATION_RESET_FORM,
  SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED,
  SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE,
  toServerData,
} from './systemNoticeConfigurationActions';

export const initialState = Object.freeze({
  formState: {
    enabled: false,
    message: textInputStateHelpers.initialState(''),
  },
  viewState: {
    loading: true,
    loadError: null,
    updateError: null,
    submitMaskState: null,
    isDirty: false,
  },
  serverData: {},
});

export const DEFAULT_SYSTEM_NOTICE = {
  message: 'Error: could not get the system notice from the server',
  enabled: true,
};
const clearedErrors = pick(['loadError', 'updateError'], initialState.viewState);

function loadRequested(_, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: true,
      loadError: null,
      submitMaskState: null,
    },
  };
}

function formStateFromServerData(state) {
  const { serverData } = state;
  const formState = {
    enabled: serverData.enabled,
    message: textInputStateHelpers.initialState(serverData.message),
  };

  return { ...state, formState };
}

function loadFulfilled(payload, state) {
  return formStateFromServerData({
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      ...clearedErrors,
    },
    serverData: payload,
  });
}

function loadSystemNoticeFailed(payload, state) {
  return formStateFromServerData({
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      loadError: payload,
    },
    serverData: DEFAULT_SYSTEM_NOTICE,
  });
}

function loadPageFailed(payload, state) {
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
    serverData: toServerData(state.formState),
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
  return formStateFromServerData({
    ...state,
    viewState: {
      ...state.viewState,
      isDirty: false,
      updateError: null,
    },
  });
}

function toggleIsSystemNoticeEnabled(_, state) {
  const { serverData, formState } = state;
  const enabled = !formState.enabled;
  return {
    ...state,
    formState: {
      ...formState,
      enabled,
    },
    viewState: {
      ...state.viewState,
      isDirty: enabled !== serverData.enabled || formState.message.trimmedValue !== serverData.message,
    },
  };
}

function setSystemNoticeMessage(payload, state) {
  const { serverData, formState } = state;
  return {
    ...state,
    formState: {
      ...formState,
      message: textInputStateHelpers.userInput(null, payload),
    },
    viewState: {
      ...state.viewState,
      isDirty: payload !== serverData.message || formState.enabled !== serverData.enabled,
    },
  };
}

function resetSubmitMaskState(_, state) {
  return pathSet(['viewState', 'submitMaskState'], null, state);
}

const reducerActionMap = {
  [SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED]: loadRequested,
  [SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED]: loadFulfilled,
  [SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED]: loadSystemNoticeFailed,
  [SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED]: loadPageFailed,
  [SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED]: updateRequested,
  [SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED]: updateFulfilled,
  [SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED]: updateFailed,
  [SYSTEM_NOTICE_CONFIGURATION_RESET_FORM]: resetForm,
  [SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED]: toggleIsSystemNoticeEnabled,
  [SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE]: setSystemNoticeMessage,
  [SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE]: resetSubmitMaskState,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
