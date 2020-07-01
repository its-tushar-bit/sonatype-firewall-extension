/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../util/reduxUtil';
import { pathSet } from '../../util/jsUtil';
import {
  ADVANCED_SEARCH_CONFIG_LOAD_FAILED,
  ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED,
  ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED,
  ADVANCED_SEARCH_CONFIG_SAVE_FAILED,
  ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED,
  ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED,
  ADVANCED_SEARCH_RESET_FORM,
  ADVANCED_SEARCH_SET_IS_ENABLED,
  ADVANCED_SEARCH_TRIGGER_RE_INDEX,
  ADVANCED_SEARCH_CLOSE_RE_INDEX_MODAL,
  ADVANCED_SEARCH_RE_INDEX_FAILED,
  ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE
} from './advancedSearchConfigActions';

const initialState = {
  // State of data held in form.
  // Same shape with serverData.
  formState: {
    isEnabled: false,
    lastIndexTime: null
  },
  // Everything but data. State of the view.
  // Is the page being loaded? Is the submitMask being shown?
  viewState: {
    loading: true,
    error: null,
    submitMaskState: null,
    submitMaskMessage: null,
    isDirty: false,
    showReIndexModal: false
  },
  // State of data in server side.
  // Same shape with formState.
  serverData: null
};

function loadRequested() {
  return {
    ...initialState
  };
}

function loadFulfilled(payload, state) {
  return {
    viewState: {
      ...state.viewState,
      loading: false,
      error: null
    },
    formState: payload,
    serverData: payload
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      loading: false,
      error: payload
    }
  };
}

function saveRequested(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      submitMaskState: false,
      submitMaskMessage: 'Saving',
      error: null
    }
  };
}

function saveFulfilled(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      submitMaskState: true,
      isDirty: false,
      error: null
    },
    serverData: state.formState
  };
}

function saveFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      error: payload,
      submitMaskState: null
    },
    formState: {
      ...state.formState,
      isEnabled: !state.formState.isEnabled
    }
  };
}

function resetSubmitMaskState(payload, state) {
  return pathSet(['viewState', 'submitMaskState'], null, state);
}

function setIsAdvancedSearchEnabled(payload, state) {
  return isDirty({
    ...state,
    formState: {
      ...state.formState,
      isEnabled: !state.formState.isEnabled
    }
  });
}

function triggerReIndex(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      showReIndexModal: true
    }
  };
}

function advancedSearchReIndexFailed(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      showReIndexModal: false,
      error: payload
    }
  };
}

function closeReIndexModal(payload, state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      showReIndexModal: false
    }
  };
}

function isDirty(state) {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      isDirty: state.formState.isEnabled !== state.serverData.isEnabled
    }
  };
}

function resetForm(payload, state) {
  return {
    ...state,
    formState: state.serverData,
    viewState: {
      ...state.viewState,
      isDirty: false
    }
  };
}

const reducerActionMap = {
  [ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED]: loadRequested,
  [ADVANCED_SEARCH_CONFIG_LOAD_FULFILLED]: loadFulfilled,
  [ADVANCED_SEARCH_CONFIG_LOAD_FAILED]: loadFailed,
  [ADVANCED_SEARCH_SET_IS_ENABLED]: setIsAdvancedSearchEnabled,
  [ADVANCED_SEARCH_CONFIG_SAVE_REQUESTED]: saveRequested,
  [ADVANCED_SEARCH_CONFIG_SAVE_FULFILLED]: saveFulfilled,
  [ADVANCED_SEARCH_CONFIG_SAVE_FAILED]: saveFailed,
  [ADVANCED_SEARCH_RESET_FORM]: resetForm,
  [ADVANCED_SEARCH_TRIGGER_RE_INDEX]: triggerReIndex,
  [ADVANCED_SEARCH_CLOSE_RE_INDEX_MODAL]: closeReIndexModal,
  [ADVANCED_SEARCH_RE_INDEX_FAILED]: advancedSearchReIndexFailed,
  [ADVANCED_SEARCH_CONFIG_SAVE_SUBMIT_MASK_TIMER_DONE]: resetSubmitMaskState
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
