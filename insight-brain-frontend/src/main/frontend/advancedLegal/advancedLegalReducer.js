/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
  ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE
} from './advancedLegalActions';
import { TEXT_BASED_OBLIGATIONS } from '../legal/advancedLegalConstants';

const initialState = {
  viewStateApplications: {
    loading: false,
    error: null
  },
  applications: [],
  viewStateApplicationReport: {
    loading: false,
    error: null
  },
  applicationReport: null,
  component: {
    loading: true
  },
  availableScopes: {
    loading: false,
    error: null
  }
};

function loadApplicationsRequested() {
  return {
    ...initialState,
    viewStateApplications: {
      loading: true,
      error: null
    }
  };
}

function loadApplicationsFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false
    },
    applications: payload
  };
}

function loadApplicationsFailed(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false,
      error: payload
    }
  };
}

function loadApplicationReportRequested(_, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      loading: true,
      error: null
    },
    applicationReport: null
  };
}

function loadApplicationReportFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false
    },
    applicationReport: payload
  };
}

function loadApplicationReportFailed(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false,
      error: payload
    }
  };
}

function loadComponentRequested() {
  return {
    ...initialState,
    component: {
      loading: true,
      error: null
    }
  };
}

function loadComponentFulfilled(payload, state) {
  const newState = {
    ...state,
    component: {
      loading: false,
      ...payload
    }
  };
  newState.component.obligations.forEach(element => {
    element.originalStatus = element.status;
    if (TEXT_BASED_OBLIGATIONS.indexOf(element.name) >= 0) {
      if (element.attributions.length === 0) {
        element.attributions.push({
          id: null,
          content: '',
          ownerId: 'ROOT_ORGANIZATION_ID'
        });
      }
      element.attributions[0].originalContent = element.attributions[0].content;
      element.attributions[0].originalOwnerId = element.attributions[0].ownerId;
      element.attributions[0].showAttributionModal = false;
      element.attributions[0].error = null;
      element.attributions[0].saveAttributionSubmitMask = null;
    }
  });
  return newState;
}

function loadComponentFailed(payload, state) {
  return {
    ...state,
    component: {
      loading: false,
      error: payload
    }
  };
}

function loadAvailableScopesRequested(_, state) {
  return {
    ...state,
    availableScopes: {
      loading: true,
      error: null
    }
  };
}

function loadAvailableScopesFulfilled(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: null,
      ...payload
    }
  };
}

function loadAvailableScopesFailed(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: payload
    }
  };
}

function setAttributionText(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].content = payload.value;
      return true;
    }
  });
  return newState;
}

function setObligationFulfilled(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.status = payload.value ? 'FULFILLED' : element.originalStatus;
      return true;
    }
  });
  return newState;
}

function setAttributionScope(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].ownerId = payload.value;
      return true;
    }
  });
  return newState;
}

function setShowAttributionModal(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].showAttributionModal = payload.value;
      return true;
    }
  });
  return newState;
}

function saveAttributionRequested(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].error = null;
      element.attributions[0].saveAttributionSubmitMask = null;
      return true;
    }
  });
  return newState;
}

function saveAttributionFulfilled(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      const newStatus = element.status; // TODO: read obligation status in DLS-1021
      element.originalStatus = newStatus;
      element.status = newStatus;
      element.attributions[0].id = payload.value.id;
      element.attributions[0].originalContent = payload.value.content;
      element.attributions[0].content = payload.value.content;
      element.attributions[0].originalOwnerId = payload.value.ownerId;
      element.attributions[0].ownerId = payload.value.ownerId;
      element.attributions[0].error = null;
      element.attributions[0].saveAttributionSubmitMask = true;
      return true;
    }
  });
  return newState;
}

function saveAttributionFailed(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].error = payload.value;
      element.attributions[0].saveAttributionSubmitMask = null;
      return true;
    }
  });
  return newState;
}

function saveAttributionSubmitMaskDone(payload, state) {
  const newState = { ...state };
  newState.component.obligations.some(element => {
    if (element.name === payload.name) {
      element.attributions[0].saveAttributionSubmitMask = null;
      element.attributions[0].showAttributionModal = false;
      return true;
    }
  });
  return newState;
}

const reducerActionMap = {
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED]: loadApplicationsRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED]: loadApplicationsFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED]: loadApplicationsFailed,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED]: loadApplicationReportRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED]: loadApplicationReportFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED]: loadApplicationReportFailed,
  [ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED]: loadComponentRequested,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED]: loadComponentFulfilled,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FAILED]: loadComponentFailed,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED]: loadAvailableScopesRequested,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED]: loadAvailableScopesFulfilled,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED]: loadAvailableScopesFailed,
  [ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT]: setAttributionText,
  [ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED]: setObligationFulfilled,
  [ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE]: setAttributionScope,
  [ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL]: setShowAttributionModal,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED]: saveAttributionRequested,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED]: saveAttributionFulfilled,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED]: saveAttributionFailed,
  [ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE]: saveAttributionSubmitMaskDone
};

const advancedLegalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default advancedLegalReducer;
